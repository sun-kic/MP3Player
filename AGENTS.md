# AGENTS.md

This document provides guidelines for agentic coding agents working on the MP3Player Android project.

## Project Overview

MP3Player is an Android media player application built with Kotlin. It uses ExoPlayer for audio/video playback and follows modern Android architecture patterns with Jetpack libraries.

## Build Commands

```bash
# Build the project
./gradlew build

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Clean build
./gradlew clean

# Install debug build on connected device
./gradlew installDebug
```

## Test Commands

```bash
# Run all unit tests
./gradlew test

# Run unit tests for a specific class
./gradlew test --tests "com.example.mp3player.ExampleUnitTest"

# Run a specific test method
./gradlew test --tests "com.example.mp3player.ExampleUnitTest.addition_isCorrect"

# Run instrumentation tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Run specific instrumentation test
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.mp3player.ExampleInstrumentedTest
```

## Lint Commands

```bash
# Run lint check
./gradlew lint

# Run lint with HTML report
./gradlew lintDebug

# Check for Kotlin code issues
./gradlew ktlintCheck
```

## Code Style Guidelines

### General Formatting
- Use 4 spaces for indentation (not tabs)
- Maximum line length: 120 characters
- Use trailing commas in multi-line parameter lists

### Imports
- Group imports: Android SDK, androidx, third-party, project-specific
- Use wildcard imports only for static constants
- Remove unused imports before committing
- Example order:
  ```kotlin
  import android.*
  import androidx.*
  import com.example.mp3player.*
  ```

### Naming Conventions
- **Classes**: PascalCase (e.g., `MediaPlayerManager`, `FileBrowserFragment`)
- **Functions**: camelCase (e.g., `loadPlaylist()`, `playNext()`)
- **Variables**: camelCase for public, descriptive names
- **Constants**: UPPER_SNAKE_CASE or const val
- **Private bindings**: prefix with underscore (e.g., `_binding`)
- **Package names**: lowercase, no underscores (e.g., `com.example.mp3player.ui.filebrowser`)

### Types & Nullability
- Use nullable types (`Type?`) only when null is a valid state
- Prefer `lateinit` for dependency injection in Fragments/Activities
- Use `by lazy` for expensive initialization
- Always check for null on nullable types or use safe calls (`?.`)
- Prefer immutable collections (`List` over `MutableList`)

### Android-Specific Patterns
- **Fragments**: Use ViewBinding pattern with `_binding` and `binding` property
- **Adapters**: Use ListAdapter with DiffUtil for RecyclerViews
- **Lifecycle**: Always nullify binding in `onDestroyView()`
- **Permissions**: Use ActivityResultContracts for permission requests
- **Navigation**: Use Navigation Component with Safe Args

### Documentation
- Add KDoc comments for public APIs and complex functions
- Document parameters with `@param` tags
- Document return values with `@return` tags
- Example:
  ```kotlin
  /**
   * Load a playlist from a list of media files
   * @param files List of media files to load
   * @param startIndex Index of the file to start playing (default: 0)
   * @param folder Optional folder containing the playlist
   */
  fun loadPlaylist(files: List<MediaFile>, startIndex: Int = 0, folder: File? = null)
  ```

### Error Handling
- Use try-catch for operations that can fail (e.g., file I/O, permissions)
- Show user-friendly error messages via Toast or Snackbar
- Log errors using Android's Log class for debugging
- Use Result types or sealed classes for complex error scenarios

### Architecture
- **Package Structure**:
  - `data/` - Data models and file operations
  - `player/` - Media playback logic (MediaPlayerManager)
  - `ui/` - UI components organized by feature (filebrowser, player, nowplaying)
- Use Singleton pattern for managers (e.g., MediaPlayerManager)
- Follow MVVM pattern with ViewModels for UI logic
- Use LiveData/StateFlow for reactive UI updates

### Testing
- Write unit tests for business logic in `test/` directory
- Write instrumentation tests for UI components in `androidTest/` directory
- Use JUnit4 for unit tests (as configured)
- Mock external dependencies in tests
- Test edge cases: empty lists, null values, permissions denied

### Dependencies
Dependencies are managed in `gradle/libs.versions.toml`:
- Use version catalogs for all dependencies
- Group related dependencies logically
- Keep Media3/ExoPlayer dependencies together

### Version Control
- Do not commit: `.idea/`, `.gradle/`, `build/`, `local.properties`
- Check `.gitignore` for full list

## Project Structure

```
app/src/
├── main/java/com/example/mp3player/
│   ├── data/              # MediaFile, FileScanner, etc.
│   ├── player/            # MediaPlayerManager (ExoPlayer wrapper)
│   ├── ui/                # UI fragments and adapters
│   │   ├── filebrowser/
│   │   ├── player/
│   │   └── nowplaying/
│   └── MainActivity.kt
├── test/                  # Unit tests
└── androidTest/           # Instrumentation tests
```
