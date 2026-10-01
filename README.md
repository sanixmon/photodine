# Photopia

Photopia is an open-source, layer-based photo editor for Android, built with
Kotlin and Jetpack Compose. Import photos, composite them as layers with blend
modes and opacity, paint with a pressure-aware brush, and export to PNG/JPEG.
MIT-licensed, offline-first, no account required.

## Requirements

- JDK 17 (CI uses Eclipse Temurin 17)
- Android SDK with platform `android-36` and build-tools `36.0.0`
- Gradle 8.9 (bootstrapped automatically via `./gradlew`)

## Build

```sh
./gradlew assembleDebug   # debug APK
./gradlew lint assembleDebug detekt   # what CI runs
```

Install the debug APK on a device/emulator and launch **Photopia**: a stub
`NavHost` with `new-canvas` and `canvas` routes is in place (ticket 01).

## Module map

| Module | Responsibility |
|---|---|
| `:app` | Entry point, Hilt app graph, Compose `NavHost`, top-level store |
| `:core:engine` | GLES 3.0 compositing pipeline, no Compose dependency |
| `:core:ui` | Shared Compose design system (theme, components) |
| `:feature:canvas` | Canvas view, gestures, stroke capture, preview render loop |
| `:feature:layers` | Layer panel, reorder, blend mode, opacity, visibility |
| `:feature:tools` | Brush/eraser parameters, transform mode, crop overlay |
| `:feature:colorpicker` | HSV wheel, hex input, RGB sliders, eyedropper, recents |
| `:feature:export` | Flatten, PNG/JPEG encode, MediaStore write, share intent |

Dependency rules: features never depend on each other; `:app` depends on all;
`:core:engine` has no Compose dependency. See [ARCHITECTURE.md](ARCHITECTURE.md).

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).
