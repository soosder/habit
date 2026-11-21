# Dopamin Paradise – native Android App

Native Android-Version der App (Kotlin + Jetpack Compose). Die Web-App unter `index.html` bleibt als Fallback bestehen.

## Was die App kann

- Tägliche Aufgabe starten, pausieren, erledigen
- XP, Münzen, Level, Serien
- Täglicher `daily_drop.json`-Sync von GitHub Pages
- Drop-Queue als Impulse-Chips auf dem Startbildschirm
- Kisten & Loot (Daily-Kiste, Mini-Kiste für 15 Münzen)
- Paradies-Aufbau, Begleiter/Crew, Freunde, Story-Kapitel
- Konzepte verwalten, `state.json` als Text kopieren

Fortschritt wird lokal in SharedPreferences gespeichert.

## Sync-Quelle

```text
https://soosder.github.io/habit/daily_drop.json
```

Die App lädt den Drop beim Start und per Knopf **„Drop jetzt laden"** in der Sync-Ansicht.

## APK bauen (Cloud, ohne Android Studio)

GitHub Actions baut bei jedem Push auf `main` (sobald sich `native-android/` ändert) eine Debug-APK.

1. Änderungen pushen.
2. Auf GitHub: **Actions → Android APK → letzter Lauf**.
3. Unten auf **Artifacts → dopamine-paradise-debug-apk** klicken und die `.apk` herunterladen.
4. APK auf das Android-Handy übertragen (z. B. per USB, E-Mail an dich selbst, Google Drive).
5. Auf dem Handy die APK öffnen und installieren. Einmalig **„Installieren aus unbekannten Quellen"** erlauben.

Alternativ kann die APK auch direkt aus einem **Release** kommen, sobald wir Signing/Release-Workflow einrichten.

## Lokaler Build (optional)

Voraussetzung: JDK 17, Android SDK (oder Android Studio).

```powershell
cd native-android
gradle assembleDebug
```

Die APK liegt dann unter:

```text
app\build\outputs\apk\debug\app-debug.apk
```

## Hinweise

- Debug-APK ist für den persönlichen Gebrauch gedacht.
- Für eine spätere Play-Store-Version brauchen wir ein Release-Keystore als GitHub Secret.
- API-Keys für die tägliche Engine bleiben ausschließlich in Windows-Umgebungsvariablen, nicht im Repo.
