# Dopamin Paradise – Setup unter Windows

Diese Anleitung bringt die App auf dem PC zum Laufen, auf GitHub Pages für das Smartphone und in die tägliche Automatisierung per Aufgabenplanung.

## 1) Lokaler Test der Engine

Im Projektordner (z. B. `C:\DopamineParadise` bzw. hier `C:\Users\Emil\Videos\Bigpets 3.0\Habit`) eine PowerShell öffnen und ausführen:

```powershell
cd "C:\Users\Emil\Videos\Bigpets 3.0\Habit"
python daily_engine.py
```

Erwartung: Es wird `daily_drop.json` aktualisiert. Da `unused_queue` mit 3 Einträgen unter `BUFFER_THRESHOLD = 7` liegt, versucht die Engine optional Gemini/OpenRouter (nur wenn API-Keys gesetzt sind) und fällt sonst sauber auf `local_hybrid` zurück.

Optionale Umgebungsvariablen:

```powershell
$env:GEMINI_API_KEY="..."
$env:OPENROUTER_API_KEY="..."
$env:BUFFER_THRESHOLD="7"
$env:BUFFER_TARGET="14"
$env:AUTO_GIT_PUSH="False"
python daily_engine.py
```

## 2) Täglicher Auto-Run per Aufgabenplanung

1. `Win + R` → `taskschd.msc` → Enter.
2. **Aufgabe erstellen (einfache)** → Name z. B. `Dopamin Paradise Daily Engine`.
3. **Trigger**: Täglich, z. B. 09:00.
4. **Aktion**: Programm `python` (oder voller Pfad zur Python.exe, falls `python` nicht gefunden wird).
5. **Argumente**: `daily_engine.py`
6. **Starten in**: `C:\Users\Emil\Videos\Bigpets 3.0\Habit` (Windows-Pfade mit Backslashes; bei anderen Ordnern anpassen).
7. Optional unter **Bedingungen**: „Aufgabe nur starten, wenn der Computer im Netzbetrieb ist“ deaktivieren, falls offline-Tests laufen sollen.
8. Speichern und per **Ausführen** einmal testen.

Hinweis: `AUTO_GIT_PUSH` bleibt standardmäßig `False`. Erst auf `True` setzen, wenn `daily_drop.json` automatisch committed/gepusht werden soll.

## 3) GitHub + GitHub Pages (Smartphone-App)

1. Repository auf GitHub anlegen und diese Dateien pushen:

   ```powershell
   git init
   git add index.html state.json daily_drop.json daily_engine.py SETUP_WINDOWS.md
   git commit -m "Initial Dopamin Paradise"
   git branch -M main
   git remote add origin https://github.com/<USER>/<REPO>.git
   git push -u origin main
   ```

2. In GitHub: **Settings → Pages → Source: Deploy from a branch → Branch: main / root**.
3. Nach 1–2 Minuten ist die App unter `https://<USER>.github.io/<REPO>/` erreichbar.
4. Auf dem Smartphone den Link öffnen → Browser-Menü → **„Zum Startbildschirm hinzufügen“** (iOS: Teilen → Zum Home-Bildschirm; Android/Chrome: Menü → App installieren / Zum Startbildschirm).

## 4) daily_drop.json in die App laden

- In der App unter **Sync & Export → daily_drop.json** die GitHub-Pages-URL eintragen, z. B. `https://<USER>.github.io/<REPO>/daily_drop.json`, und **URL** klicken.
- Oder `daily_drop.json` lokal exportieren und per **Datei laden** importieren.
- Der Import übernimmt `daily`, `story`, `crate` und alle neuen `unused_queue`-Einträge in den Drop-Puffer.
