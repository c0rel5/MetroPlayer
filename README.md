# Metro Player

A music player for Android inspired by Windows Phone's Metro design.

[**Download the latest APK**](https://github.com/c0rel5/MetroPlayer/releases/latest)

## Features

**Playback**
- Background playback, notification and lock screen controls (Media3)
- Shuffle, repeat (all / one), favorites
- Playback speed 0.75–2×, sleep timer
- Queue that shows the real play order when shuffle is on
- Mini player on every screen

**Library**
- Live tiles on the home screen: artists, albums, playlists, favorites, recent
- Jump list: tap a letter header to open a letter grid, as on Windows Phone
- Global search across artists, albums and tracks
- Format filter (`.flac`, `.mp3`, …) in any track list, with a configurable default
- Track menu: play next, add to queue, favorite, add to playlist, go to artist, go to album

**Lyrics**
- Synced LRC lyrics with line highlighting; tap a line to seek
- Fetched from [lrclib.net](https://lrclib.net) or loaded from a `.lrc` / `.txt` file
- Per-track sync offset (±0.5 s)
- Two display modes: synced (auto-scroll) and selectable (static, copyable text)
- Two animations to choose from: lines glide to the center with words lighting up one by one, or the classic sliding window
- Tap the cover to flip the card and show lyrics on the back
- Custom GIF in place of the note during instrumental breaks

**Appearance**
- Four skins, each with its own app and notification icon: Metro, Windows 7, Aero, Material You
- Dark and light themes, selectable accent color
- Windows Phone style toggles and sliders, cascading tile and list animations

## Installation

1. Download the latest `MetroPlayer-*.apk` from [Releases](https://github.com/c0rel5/MetroPlayer/releases/latest).
2. Allow installation from unknown sources and install the file.
3. Grant access to audio and notifications on first launch.

Every build is signed with the same key, so new versions install over old ones without uninstalling.

Requires Android 8.0 (API 26) or newer.

## Building

**Android Studio:** open the project folder and run. JDK 17 is required; Gradle is downloaded automatically.

**Command line:**

```bash
gradle assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

**GitHub Actions:** the [workflow](.github/workflows/build.yml) builds the APK on every push and publishes it to Releases, using [`RELEASE_NOTES.md`](RELEASE_NOTES.md) as the release body.

## Tech stack

- Kotlin, Jetpack Compose
- AndroidX Media3 (ExoPlayer, MediaSession)
- Coil (images and GIFs)
- minSdk 26, targetSdk 35, package `com.metroplayer`

## Project structure

```
app/src/main/java/com/metroplayer/
├── MainActivity.kt
├── PlaybackService.kt
├── MetroNotification.kt
├── SkinIcons.kt
├── player/    playback controller
├── data/      library, lyrics, settings
└── ui/        screens, tiles, now playing, lyrics
```

## Custom font

The original concept uses Segoe UI / Open Sans; the app currently uses the system sans-serif. To use your own, put the font file in `app/src/main/res/font/` and change the `MetroFont` line in `ui/MetroTheme.kt`.

## Contributing

Bug reports and pull requests are welcome. Open an [issue](https://github.com/c0rel5/MetroPlayer/issues) to start.

## Disclaimer

Metro Player is an independent fan project and is not affiliated with Microsoft.