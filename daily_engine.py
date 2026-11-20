#!/usr/bin/env python3
"""Dopamin-Paradise Daily Engine.

Läuft einmal täglich (z. B. per Windows Aufgabenplanung), prüft den Puffer
unverbrauchter Drop-Einträge und erzeugt daily_drop.json entweder lokal
(Standard, wenn der Puffer reicht) oder über optionale Free-Tier-APIs.
"""

from __future__ import annotations

import hashlib
import json
import os
import random
import re
import subprocess
import sys
import urllib.error
import urllib.request
from datetime import datetime, timedelta
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent
STATE_PATH = BASE_DIR / "state.json"
DROP_PATH = BASE_DIR / "daily_drop.json"

BUFFER_THRESHOLD = int(os.environ.get("BUFFER_THRESHOLD", "7"))
BUFFER_TARGET = int(os.environ.get("BUFFER_TARGET", "14"))
API_TIMEOUT = int(os.environ.get("API_TIMEOUT", "20"))
AUTO_GIT_PUSH = os.environ.get("AUTO_GIT_PUSH", "False").strip().lower() in {"1", "true", "yes", "on"}

GEMINI_API_KEY = os.environ.get("GEMINI_API_KEY", "").strip()
GEMINI_MODEL = os.environ.get("GEMINI_MODEL", "gemini-2.0-flash")
OPENROUTER_API_KEY = os.environ.get("OPENROUTER_API_KEY", "").strip()
OPENROUTER_MODEL = os.environ.get("OPENROUTER_MODEL", "meta-llama/llama-3.3-70b-instruct:free")

DEFAULT_CONCEPTS = [
    "10 Minuten an die frische Luft",
    "Eine Mini-Kiste öffnen",
    "Erstes Paradies-Beet pflanzen",
    "5 Minuten dehnen",
    "Eine kleine Errungenschaft notieren",
]

QUEUE_TYPES = {"quest", "mini_quest", "loot", "story", "paradise", "crew"}
RARITIES = ["common", "rare", "epic", "legendary"]


def now_iso() -> str:
    return datetime.now().astimezone().isoformat(timespec="seconds")


def today_str() -> str:
    return datetime.now().strftime("%Y-%m-%d")


def load_json(path: Path, default):
    try:
        with path.open("r", encoding="utf-8") as fh:
            return json.load(fh)
    except FileNotFoundError:
        return default
    except (json.JSONDecodeError, OSError) as exc:
        print(f"[warn] {path.name} konnte nicht gelesen werden ({exc}); Standard wird genutzt.")
        return default


def atomic_write_json(path: Path, data) -> None:
    tmp = path.with_suffix(path.suffix + ".tmp")
    with tmp.open("w", encoding="utf-8") as fh:
        json.dump(data, fh, ensure_ascii=False, indent=2)
        fh.write("\n")
    tmp.replace(path)


def ensure_state(state: dict) -> dict:
    state.setdefault("version", 1)
    state.setdefault("concepts", [])
    if not isinstance(state["concepts"], list):
        state["concepts"] = []
    state.setdefault("progress", {})
    state.setdefault("meta", {})
    state["meta"]["last_engine_seen_at"] = now_iso()
    return state


def ensure_drop(drop: dict) -> dict:
    drop.setdefault("version", 1)
    drop.setdefault("generated_at", None)
    drop.setdefault("date", None)
    drop.setdefault("source", "uninitialized")
    drop.setdefault("buffer_before", 0)
    drop.setdefault("buffer_after", 0)
    drop.setdefault("unused_queue", [])
    drop.setdefault("daily", None)
    drop.setdefault("story", None)
    drop.setdefault("crate", None)
    drop.setdefault("history", [])
    if not isinstance(drop["unused_queue"], list):
        drop["unused_queue"] = []
    if not isinstance(drop["history"], list):
        drop["history"] = []
    return drop


def seed_for(*parts: str) -> random.Random:
    digest = hashlib.sha256("::".join(parts).encode("utf-8")).hexdigest()
    return random.Random(int(digest[:16], 16))


def active_concepts(state: dict) -> list[str]:
    concepts = []
    for item in state.get("concepts", []):
        if isinstance(item, dict):
            text = str(item.get("text", "")).strip()
            if text and item.get("active", True):
                concepts.append(text)
    return concepts or DEFAULT_CONCEPTS


def queue_item(rng: random.Random, index: int, concept: str, item_type: str, created: str) -> dict:
    rarity = rng.choices(RARITIES, weights=[62, 26, 9, 3])[0]
    mult = {"common": 1, "rare": 1.5, "epic": 2.2, "legendary": 3.5}[rarity]
    focus = rng.choice([0, 10, 15, 20, 25, 30])
    return {
        "id": f"lq_{today_str()}_{index:02d}",
        "type": item_type if item_type in QUEUE_TYPES else "quest",
        "title": f"{concept} #{index + 1}",
        "text": f"Lokaler Hybrid-Impuls auf Basis des Konzepts: {concept}",
        "focus_minutes": focus,
        "reward_xp": int((25 + rng.randint(0, 25)) * mult),
        "reward_coins": int((4 + rng.randint(0, 10)) * mult),
        "rarity": rarity,
        "created_at": created,
    }


def local_hybrid_generate(state: dict, drop: dict, needed: int = 0, reason: str = "local") -> dict:
    """Erzeugt Daily-Impuls, Story, Kisten-Template und optional Queue-Refill."""
    created = now_iso()
    day = today_str()
    rng = seed_for(day, reason, str(needed), json.dumps(state.get("concepts", []), ensure_ascii=False, sort_keys=True))
    concepts = active_concepts(state)
    concept = rng.choice(concepts)
    focus = rng.choice([10, 15, 20, 25])
    rarity = rng.choices(RARITIES, weights=[55, 30, 12, 3])[0]

    daily = {
        "id": f"daily_{day}",
        "type": "quest",
        "title": f"Tagesimpuls: {concept}",
        "text": f"Heute reicht ein kleiner Schritt: {concept}. Danach darf die Kiste aufgehen.",
        "focus_minutes": focus,
        "reward_xp": 40 + focus * 2,
        "reward_coins": 8 + focus // 2,
        "source": "local_hybrid",
        "created_at": created,
    }

    story = {
        "id": f"story_{day}",
        "title": f"Kapitel {day} – {concept[:34]}",
        "text": (
            f"Der Funke „{concept}“ zieht durch die Stadt. Jemand öffnet eine runde Kiste, "
            "Lichterketten schalten sich ein, und das Paradies wächst um ein Beet."
        ),
        "source": "local_hybrid",
        "created_at": created,
    }

    crate = {
        "id": f"crate_{day}",
        "rarity": rarity,
        "source": "local_hybrid",
        "loot": {
            "type": rng.choice(["coins", "xp", "concept", "story", "crew"]),
            "coins": int((8 + rng.randint(0, 18)) * {"common": 1, "rare": 1.5, "epic": 2.2, "legendary": 3.5}[rarity]),
            "xp": int((12 + rng.randint(0, 24)) * {"common": 1, "rare": 1.5, "epic": 2.2, "legendary": 3.5}[rarity]),
            "title": f"{rarity.capitalize()}-Kisten-Beute",
            "text": "Ein local erzeugter Loot-Slot für die App.",
        },
        "created_at": created,
    }

    refill = []
    types = ["quest", "mini_quest", "loot", "story", "paradise", "crew"]
    for i in range(max(0, needed)):
        refill.append(queue_item(rng, i, rng.choice(concepts), types[i % len(types)], created))

    return {"daily": daily, "story": story, "crate": crate, "unused_queue": refill}


def extract_json_object(text: str) -> dict:
    text = text.strip()
    if text.startswith("```"):
        text = re.sub(r"^```(?:json)?\s*", "", text)
        text = re.sub(r"\s*```$", "", text)
    try:
        return json.loads(text)
    except json.JSONDecodeError:
        pass
    start = text.find("{")
    end = text.rfind("}")
    if start == -1 or end == -1 or end <= start:
        raise ValueError("Antwort enthält kein JSON-Objekt")
    return json.loads(text[start : end + 1])


def http_json(url: str, payload: dict, headers: dict | None = None, timeout: int = API_TIMEOUT):
    data = json.dumps(payload, ensure_ascii=False).encode("utf-8")
    req = urllib.request.Request(
        url,
        data=data,
        headers={"Content-Type": "application/json", "User-Agent": "dopamine-paradise-engine/1.0", **(headers or {})},
        method="POST",
    )
    with urllib.request.urlopen(req, timeout=timeout) as resp:
        return json.loads(resp.read().decode("utf-8"))


def build_prompt(state: dict, needed: int, buffer_before: int) -> str:
    concepts = active_concepts(state)
    concept_lines = "\n".join(f"- {c}" for c in concepts[:30])
    return f"""Du bist ein kreativer Generator für eine Habit-/Gamification-App.
Antworte AUSSCHLIESSLICH mit einem JSON-Objekt, ohne Markdown, ohne Erklärung.

Schema (alle Felder Pflicht, außer daily/story/crate dürfen null sein):
{{
  "daily": {{"type":"quest","title":"...","text":"...","focus_minutes":15,"reward_xp":40,"reward_coins":8}},
  "story": {{"title":"...","text":"..."}},
  "crate": {{"rarity":"common|rare|epic|legendary","loot":{{"type":"coins|xp|concept|story|crew","coins":10,"xp":15,"title":"...","text":"..."}}}},
  "unused_queue": [{{"type":"quest|mini_quest|loot|story|paradise|crew","title":"...","text":"...","focus_minutes":15,"reward_xp":30,"reward_coins":5,"rarity":"common|rare|epic|legendary"}}]
}}

Regeln:
- Sprache: Deutsch, motivierend, kurz.
- unused_queue muss genau {needed} Einträge enthalten.
- buffer_before={buffer_before}; es fehlen {needed} Puffer-Einträge bis Ziel {BUFFER_TARGET}.
- Verwende nach Möglichkeit diese Konzepte:
{concept_lines}
"""


def gemini_generate(state: dict, needed: int, buffer_before: int) -> dict:
    if not GEMINI_API_KEY:
        raise RuntimeError("GEMINI_API_KEY fehlt")
    models = []
    for m in [GEMINI_MODEL, "gemini-2.0-flash", "gemini-1.5-flash"]:
        if m and m not in models:
            models.append(m)
    prompt = build_prompt(state, needed, buffer_before)
    last_error = None
    for model in models:
        url = f"https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent"
        payload = {
            "contents": [{"parts": [{"text": prompt}]}],
            "generationConfig": {"responseMimeType": "application/json", "temperature": 0.8},
        }
        try:
            data = http_json(url, payload, headers={"x-goog-api-key": GEMINI_API_KEY})
            parts = data.get("candidates", [{}])[0].get("content", {}).get("parts", [])
            text = "".join(part.get("text", "") for part in parts if isinstance(part, dict))
            return extract_json_object(text)
        except Exception as exc:  # noqa: BLE001 - Fallback soll alle Fehler abfangen
            last_error = exc
    raise RuntimeError(f"Gemini fehlgeschlagen: {last_error}")


def openrouter_generate(state: dict, needed: int, buffer_before: int) -> dict:
    if not OPENROUTER_API_KEY:
        raise RuntimeError("OPENROUTER_API_KEY fehlt")
    payload = {
        "model": OPENROUTER_MODEL,
        "messages": [
            {"role": "system", "content": "Du gibst nur valides JSON zurück."},
            {"role": "user", "content": build_prompt(state, needed, buffer_before)},
        ],
        "temperature": 0.8,
        "response_format": {"type": "json_object"},
    }
    data = http_json(
        "https://openrouter.ai/api/v1/chat/completions",
        payload,
        headers={
            "Authorization": f"Bearer {OPENROUTER_API_KEY}",
            "HTTP-Referer": "https://dopamine-paradise.local",
            "X-Title": "Dopamin Paradise Engine",
        },
    )
    text = data.get("choices", [{}])[0].get("message", {}).get("content", "")
    return extract_json_object(text)


def normalize_queue_items(items, fallback_concept: str, created: str, start_index: int = 0) -> list[dict]:
    out = []
    for i, raw in enumerate(items or []):
        if not isinstance(raw, dict):
            continue
        item_type = str(raw.get("type", "quest"))
        if item_type not in QUEUE_TYPES:
            item_type = "quest"
        rarity = str(raw.get("rarity", "common"))
        if rarity not in RARITIES:
            rarity = "common"
        out.append(
            {
                "id": str(raw.get("id") or f"api_{today_str()}_{start_index + i:02d}"),
                "type": item_type,
                "title": str(raw.get("title") or raw.get("name") or f"{fallback_concept} #{i + 1}")[:140],
                "text": str(raw.get("text") or raw.get("description") or "")[:500],
                "focus_minutes": max(0, int(raw.get("focus_minutes") or raw.get("focusMinutes") or 0)),
                "reward_xp": max(0, int(raw.get("reward_xp") or raw.get("xp") or 0)),
                "reward_coins": max(0, int(raw.get("reward_coins") or raw.get("coins") or 0)),
                "rarity": rarity,
                "created_at": str(raw.get("created_at") or created),
            }
        )
    return out


def normalize_api_payload(payload: dict, needed: int) -> dict:
    if not isinstance(payload, dict):
        raise ValueError("API lieferte kein Objekt")
    created = now_iso()
    concept = active_concepts({"concepts": []})[0]
    queue = normalize_queue_items(payload.get("unused_queue"), concept, created)
    if not queue:
        raise ValueError("API lieferte keine unused_queue")
    daily = payload.get("daily") if isinstance(payload.get("daily"), dict) else None
    story = payload.get("story") if isinstance(payload.get("story"), dict) else None
    crate = payload.get("crate") if isinstance(payload.get("crate"), dict) else None
    return {"daily": daily, "story": story, "crate": crate, "unused_queue": queue[:needed * 2]}


def try_providers(state: dict, needed: int, buffer_before: int) -> tuple[dict | None, list[str], str | None]:
    errors = []
    providers = []
    if GEMINI_API_KEY:
        providers.append(("gemini", gemini_generate))
    if OPENROUTER_API_KEY:
        providers.append(("openrouter", openrouter_generate))
    for name, fn in providers:
        try:
            raw = fn(state, needed, buffer_before)
            return normalize_api_payload(raw, needed), errors, name
        except Exception as exc:  # noqa: BLE001
            errors.append(f"{name}: {exc}")
    return None, errors, None


def maybe_git_push() -> list[str]:
    if not AUTO_GIT_PUSH:
        return []
    messages = []
    commands = [
        ["git", "rev-parse", "--is-inside-work-tree"],
        ["git", "add", "state.json", "daily_drop.json"],
        ["git", "diff", "--cached", "--quiet"],
        ["git", "commit", "-m", f"chore: daily_drop {today_str()}"],
        ["git", "push"],
    ]
    for cmd in commands:
        try:
            result = subprocess.run(cmd, cwd=BASE_DIR, capture_output=True, text=True, timeout=60, check=False)
        except (OSError, subprocess.TimeoutExpired) as exc:
            messages.append(f"git {' '.join(cmd[:2])} fehlgeschlagen: {exc}")
            break
        if result.returncode != 0 and cmd[:2] != ["git", "diff"]:
            messages.append(f"git {' '.join(cmd[:3])} fehlgeschlagen: {(result.stderr or result.stdout).strip()[:300]}")
            if cmd[:2] == ["git", "rev-parse"]:
                break
    return messages


def main() -> int:
    state = ensure_state(load_json(STATE_PATH, {}))
    drop = ensure_drop(load_json(DROP_PATH, {}))

    buffer_before = len(drop.get("unused_queue", []))
    buffer_after_target = BUFFER_TARGET
    errors: list[str] = []
    source = "local_hybrid"
    mode = "buffer_sufficient"

    if buffer_before >= BUFFER_THRESHOLD:
        generated = local_hybrid_generate(state, drop, needed=0, reason="buffer_sufficient")
    else:
        needed = max(3, buffer_after_target - buffer_before)
        generated, errors, provider = try_providers(state, needed, buffer_before)
        if generated and provider:
            source = provider
            mode = "api_refill"
        else:
            generated = local_hybrid_generate(state, drop, needed=needed, reason="api_fallback")
            source = "local_hybrid"
            mode = "api_fallback"

    queue = list(drop.get("unused_queue", []))
    refill = generated.get("unused_queue") or []
    if refill:
        queue.extend(refill)
        queue = queue[-60:]

    drop["generated_at"] = now_iso()
    drop["date"] = today_str()
    drop["source"] = source
    drop["mode"] = mode
    drop["provider_errors"] = errors
    drop["buffer_before"] = buffer_before
    drop["unused_queue"] = queue
    drop["buffer_after"] = len(queue)
    if generated.get("daily"):
        drop["daily"] = generated["daily"]
    if generated.get("story"):
        drop["story"] = generated["story"]
    if generated.get("crate"):
        drop["crate"] = generated["crate"]
    drop["history"] = (drop.get("history", []) + [{
        "date": today_str(),
        "generated_at": drop["generated_at"],
        "source": drop["source"],
        "mode": mode,
        "buffer_before": buffer_before,
        "buffer_after": drop["buffer_after"],
        "refill": len(refill),
        "errors": errors,
    }])[-60:]

    atomic_write_json(DROP_PATH, drop)
    atomic_write_json(STATE_PATH, state)

    git_messages = maybe_git_push()

    print(f"[ok] daily_drop.json geschrieben: source={drop['source']} mode={mode} buffer {buffer_before}->{drop['buffer_after']}")
    if errors:
        print("[info] Provider-Fehler (Fallback lokal):")
        for err in errors:
            print(f"  - {err}")
    for msg in git_messages:
        print(f"[git] {msg}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
