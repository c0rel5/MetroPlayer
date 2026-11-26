<div align="center">

# 🎵 Metro Player

**A music player for Android in the spirit of Windows Phone.**  
Live tiles, flat typography, smooth motion, synced lyrics and a built-in equalizer.

[![Release](https://img.shields.io/github/v/release/c0rel5/MetroPlayer?style=for-the-badge&color=1BA1E2&label=release)](https://github.com/c0rel5/MetroPlayer/releases/latest)
![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Media3](https://img.shields.io/badge/Media3-ExoPlayer-F4511E?style=for-the-badge)

[**⬇ Download the latest APK**](https://github.com/c0rel5/MetroPlayer/releases/latest) · [Changelog](RELEASE_NOTES.md) · [Build it yourself](#-build)

</div>

---

## ✨ Highlights

| | |
|---|---|
| 🧱 **Metro design** | Live tiles on the home screen, pivot navigation, square cover art, flat controls. Four styles: *Metro*, *Windows 7*, *Aero* and *Material You*, plus dark / light / system theme and a custom accent color. |
| 🎤 **Synced lyrics** | Lines light up as the song plays, tap a line to seek. Lyrics are fetched from [lrclib.net](https://lrclib.net) or loaded from your own `.lrc` / `.txt` file. Per-track sync offset of ±0.5 s. |
| 🪩 **Two lyric animations** | *New*: lines glide to the center and words light up one by one. *Classic*: the block of lines slides in. Choose in Settings. |
| 🔄 **Cover flip** | Tap the cover on the Now Playing screen: the card flips over and the lyrics appear on its back. |
| 🎚 **Equalizer** | Five bands, twelve ready-made presets (Bass, Rock, Pop, Jazz, Vocal, Electronic…) and manual sliders. It works on all music in the app. |
| 🖼 **Your own GIF** | Replace the GIF that is shown instead of the "♪" note during instrumental breaks with any GIF or image you like. |
| 📊 **Statistics and Daily Mix** | Listening time, plays, top tracks and artists, a day-by-day chart and a listening streak. A 30-track *Daily Mix* is built every day from your history. Everything stays on the phone. |
| 🔎 **Library tools** | Global search, quick jump by letter (like Windows Phone), format filter (`.flac`, `.mp3`…) with a default format in Settings, favorites, playlists, history. |
| ⏱ **Playback extras** | Sleep timer, speed 0.75×–2×, shuffle, repeat one / all, queue editing, background playback with a notification and lock-screen controls. |

## 📲 Install

1. Open the [**Releases**](https://github.com/c0rel5/MetroPlayer/releases/latest) page and download the `.apk` file.
2. Open it on your phone and allow installing from this source if Android asks.
3. Grant access to your music when the app starts.

Every build is signed with the same key, so you can **install a new version over the old one** without uninstalling.

> **Requirements:** Android 8.0 (API 26) or newer.

> **Heads-up:** Google services may flag the APK as a virus. Please check it on [VirusTotal](https://www.virustotal.com/) ¯\\\_(ツ)\_/¯

## 🧭 Screens

- **Now Playing**: pivot *now playing · queue · history*, big square cover, round controls, progress bar with remaining time, and a lyrics line under the buttons (or lyrics instead of the cover).
- **Home**: live tiles for artists, albums, playlists, favorites, recent tracks and all tracks.
- **Lists**: play / shuffle on top, track count and total duration, a "···" menu on every track (play next, add to queue, favorite, playlist, go to artist, go to album).
- **Lyrics**: synced mode with auto-scroll, or a selectable mode where you can copy the text.
- **Settings**: style, theme, accent color, equalizer, lyrics options, custom GIF, default format.

## 🛠 Build

**Android Studio**

1. `File → Open` and select the project folder.
2. Use JDK 17. Gradle is downloaded automatically.
3. Press **Run**.

**GitHub Actions (no Android Studio needed)**

Push the project to a repository. The workflow in [`.github/workflows/build.yml`](.github/workflows/build.yml) builds the APK and publishes it as a GitHub release with notes taken from [`RELEASE_NOTES.md`](RELEASE_NOTES.md).

| | |
|---|---|
| Package | `com.metroplayer` |
| minSdk / targetSdk | 26 / 35 |
| Language | Kotlin, Jetpack Compose |
| Playback | AndroidX Media3 (ExoPlayer + MediaSession) |
| Images and GIFs | Coil |

## 🗂 Project structure

```
app/src/main/java/com/metroplayer/
├── MainActivity.kt        entry point, permissions
├── MetroApp.kt            application object: store, repository, player, auto lyrics
├── PlaybackService.kt     background playback, notification, equalizer
├── data/                  Store (settings), MusicRepository, Lyrics (lrclib, LRC), EqPresets
├── player/                PlayerController (UI side of the media session)
└── ui/                    Compose screens: Home, NowPlaying, Lyrics, Settings, About…
```

## 🔤 Font

The app uses **Segoe UI Unicode** (`res/font/segoe_ui_unicode.otf`). To use another font, put a `.ttf` / `.otf` into `app/src/main/res/font/` and change the single `MetroFont` line in `ui/MetroTheme.kt`.

## 🏷 Versioning

| Change | Version | Example |
|---|---|---|
| New features | the second number goes up | `2.6` → `2.7` |
| Bug fixes only | a third number is added or goes up | `2.6` → `2.6.1` → `2.6.2` |

The version lives in one place, `versionName` in [`app/build.gradle.kts`](app/build.gradle.kts), and the release title and file name are generated from it.

## 🤝 Contributing

The source code is open. Issues, ideas and pull requests are welcome. If you find a bug, please open an issue and describe what you did and what you expected to happen.

## 👤 Author

Made by **corei5**, with a lot of care for the community around the project. 💙
