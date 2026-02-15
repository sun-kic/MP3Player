# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install on connected device
./gradlew installDebug

# Run all unit tests
./gradlew test

# Run specific test class
./gradlew test --tests "com.example.mp3player.ExampleUnitTest"

# Run instrumentation tests (requires device/emulator)
./gradlew connectedAndroidTest

# Lint check
./gradlew lint
```

## Architecture Overview

**Android Kotlin app using ExoPlayer (Media3) for audio/video playback.**

### Core Components

- **MediaPlayerManager** (`player/MediaPlayerManager.kt`): Singleton ExoPlayer wrapper managing playlist, playback state, and listener notifications. All playback control flows through this class.

- **MainActivity**: Sets up Navigation Component with three destinations, handles fullscreen mode for video, and restores last playback state on launch.

### UI Fragments (Navigation Component)

1. **FileBrowserFragment**: File system browser with storage root detection, permission handling (API 30+ scoped storage), and folder navigation.

2. **MediaPlayerFragment**: Playback UI with YouTube-style gesture controls (double-tap to seek, single-tap to toggle controls). Auto-hides controls during video playback.

3. **NowPlayingFragment**: Playlist display with current track highlighting.

### Data Layer (`data/`)

- **MediaFile/MediaFolder**: Data classes with factory methods `fromFile()`
- **FileScanner**: Detects storage volumes and scans for media files
- **PlaybackPreferences**: SharedPreferences wrapper for persisting playback state
- **SupportedFormats**: Audio (mp3, wav, flac) and video (mp4, mkv, mpg, mpeg, avi) format definitions

### Key Patterns

- **Singleton** for MediaPlayerManager (thread-safe with `@Volatile` and `synchronized`)
- **PlaybackListener** interface for reactive UI updates (onTrackChanged, onPlaylistChanged, onPlaybackStateChanged)
- **ViewBinding** with `_binding`/`binding` pattern in fragments
- **Navigation Component** for fragment transitions

## SDK Configuration

- Min SDK: 24 (Android 7.0)
- Target SDK: 35 (Android 15)
- Dependencies managed via `gradle/libs.versions.toml`

## Permission Handling

FileBrowserFragment handles three permission scenarios:
- Android 11+ (R): MANAGE_EXTERNAL_STORAGE with Settings redirect
- Android 13+ (Tiramisu): READ_MEDIA_AUDIO and READ_MEDIA_VIDEO
- Pre-Android 13: READ_EXTERNAL_STORAGE

## Layout Structure

- Default layouts for phones
- `layout-w600dp/` for tablets
- `layout-w1240dp/` for large tablets
- Landscape-optimized UI throughout
