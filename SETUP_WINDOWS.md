# Dopamin Paradise – Setup unter Windows

Diese Anleitung bringt die App auf GitHub Pages, installiert sie als PWA auf dem Smartphone und richtet den täglichen Engine-Lauf per Aufgabenplanung ein.

## 1) Lokaler Test der Engine

Im Projektordner `C:\Users\Emil\Videos\Bigpets 3.0\Habit` eine PowerShell öffnen:

```powershell
cd "C:\Users\Emil\Videos\Bigpets 3.0\Habit"
python daily_engine.py
```

Erwartung: `daily_drop.json` wird aktualisiert. Wenn `unused_queue` unter `BUFFER_THRESHOLD = 7` liegt, versucht die Engine optional Gemini/OpenRouter (nur bei gesetzten API-Keys) und fällt sonst sauber auf `local_hybrid` zurück.

Optionale Umgebungsvariablen:

```powershell
$env:GEMINI_API_KEY="..."
$env:OPENROUTER_API_KEY="..."
$env:BUFFER_THRESHOLD="7"
$env:BUFFER_TARGET="14"
python daily_engine.py
```

## 2) Täglicher Auto-Run per Aufgabenplanung

Es gibt zwei Wege:

### Empfohlen: `run_daily.bat`

`run_daily.bat` setzt `AUTO_GIT_PUSH=True` und startet die Engine. Einmal prüfen, dass Git Push ohne Nachfrage funktioniert:

```powershell
cd "C:\Users\Emil\Videos\Bigpets 3.0\Habit"
git push
```

Wenn das klappt, in der Aufgabenplanung (`Win + R` → `taskschd.msc`):

1. **Aufgabe erstellen (einfach)** → Name z. B. `Dopamin Paradise Daily`.
2. **Trigger**: Täglich, gewünschte Uhrzeit.
3. **Aktion**: Programm `C:\Users\Emil\Videos\Bigpets 3.0\Habit\run_daily.bat`
4. **Starten in**: `C:\Users\Emil\Videos\Bigpets 3.0\Habit`
5. Speichern und per **Ausführen** testen.

### Alternativ ohne Auto-Push

Aktion: Programm `python`, Argumente `daily_engine.py`, Starten in `C:\Users\Emil\Videos\Bigpets 3.0\Habit`. Dann muss `daily_drop.json` manuell committed/gepusht werden.

## 3) GitHub Pages (Smartphone-App)

Repo: `https://github.com/soosder/habit` (public), Pages ist auf `main / root` geschaltet.

App-URL:

```text
https://soosder.github.io/habit/
```

Neue Änderungen pushen:

```powershell
cd "C:\Users\Emil\Videos\Bigpets 3.0\Habit"
git add index.html manifest.webmanifest sw.js icon.svg icon-192.png icon-512.png run_daily.bat state.json daily_drop.json daily_engine.py SETUP_WINDOWS.md
git commit -m "Update Dopamin Paradise"
git push
```

## 4) App auf dem Handy installieren

1. Am Handy `https://soosder.github.io/habit/` öffnen.
2. **Android/Chrome:** Menü → **App installieren** oder **Zum Startbildschirm hinzufügen**.
3. **iPhone/Safari:** Teilen → **Zum Home-Bildschirm**.

Die App enthält jetzt `manifest.webmanifest` und `sw.js`; der Service Worker cached die App und lädt `daily_drop.json` netzwerkaktuell nach.

## 5) daily_drop.json Sync

- Beim Öffnen der App wird `daily_drop.json` von derselben Origin automatisch geprüft und importiert, wenn sich der Inhalt geändert hat.
- Manueller Import bleibt möglich unter **Sync & Export** per URL, Datei oder Text.
- Der Import übernimmt `daily`, `story`, `crate` und neue `unused_queue`-Einträge in den Drop-Puffer.

## 6) Optionaler WLAN-Schnelltest

Wenn PC und Handy im selben WLAN sind:

```powershell
cd "C:\Users\Emil\Videos\Bigpets 3.0\Habit"
python -m http.server 8000 --bind 0.0.0.0
```

`ipconfig` → IPv4-Adresse notieren, am Handy `http://<IP>:8000` öffnen. Das funktioniert nur, solange PC und Server laufen; für den Dauerbetrieb bleibt GitHub Pages die bessere Lösung.

## 7) Native Android App (optional, parallel zur Web-App)

Unter `native-android/` liegt eine echte native App (Kotlin + Jetpack Compose). Sie wird **nicht** lokal gebaut, sondern per GitHub Actions in der Cloud.

Ablauf:

1. Änderungen committen und pushen (Pfad `native-android/**` löst den Workflow aus).
2. Auf GitHub: **Actions → Android APK** → neuesten Lauf öffnen.
3. **Artifacts → dopamine-paradise-debug-apk** herunterladen.
4. APK aufs Handy übertragen und installieren (einmalig „Installieren aus unbekannten Quellen" erlauben).

Die App lädt `daily_drop.json` automatisch von `https://soosder.github.io/habit/daily_drop.json`. Detaillierte Anleitung: `native-android/README.md`.
