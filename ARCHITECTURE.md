# Photodine Architecture

## Dependency injection: Hilt

Hilt is the DI framework (Koin was considered and rejected). Reasons:

- Compile-time safety: missing bindings fail the build, not at runtime.
- First-class ViewModel support via `@HiltViewModel` in every feature module.
- Google-maintained, stable with KSP and the Android Gradle Plugin.
- Koin's runtime service-locator resolution trades that safety away for little
  benefit here; it remains an option only if the project ever goes
  Kotlin-multiplatform.

Wiring: `:app` declares `@HiltAndroidApp PhotodineApp`; `MainActivity` is an
`@AndroidEntryPoint` hosting the Compose `NavHost`. Each feature ViewModel is a
`@HiltViewModel` with an `@Inject` constructor. Singleton engine bindings live
in `:core:engine`'s `EngineModule` (`@Module @InstallIn(SingletonComponent)`).
No `@EntryPoint` bridges are needed because the module graph is plain Gradle
project dependencies (`:app` depends on all modules).

## Module map and dependency rules

```
:app -> core:ui, core:engine,
        feature:canvas, feature:layers, feature:tools,
        feature:colorpicker, feature:export
feature:canvas      -> core:engine, core:ui
feature:layers      -> core:engine, core:ui
feature:tools       -> core:engine, core:ui
feature:colorpicker -> core:ui
feature:export      -> core:engine
core:ui             -> Compose only
core:engine         -> no Compose, pure EGL/GLES + coroutines
```

Rules: features never depend on each other; cross-feature events travel via the
`:app` top-level store, navigation arguments, or `SharedFlow` effects.
`:core:engine` exposes `Compositor`, texture-store, and history interfaces —
the primary test seam.

## App architecture: MVI

Every feature follows unidirectional MVI: `Intent -> Reducer -> State -> UI`.
Each feature module exposes a `ViewModel` holding a `StateFlow<UiState>` plus an
`Intent` sealed class; `onIntent()` reduces to a new immutable state. One-shot
side effects (navigation, export I/O, MediaStore writes) are emitted via a
`SharedFlow<Effect>`. Reducers are pure and unit-tested without Compose.

## Rendering engine: OpenGL ES 3.0 (overview)

- Each layer is a GPU `GL_TEXTURE_2D` (RGBA8, max 2048px per axis).
- Compositing uses ping-pong FBOs: visible layers merge bottom-to-top into an
  accumulator texture, one cached GLSL program per `BlendMode`
  (Normal, Multiply, Screen, Overlay, Darken, Lighten). Each shader is a
  standalone `.glsl` file under `:core:engine/src/main/glsl/` so new blend
  modes arrive as self-contained PRs.
- Preview renders to the `TextureView` `SurfaceTexture`; no CPU readback on the
  interactive path. Brush stamps render into the active layer's FBO
  (`GL_ONE, GL_ONE_MINUS_SRC_ALPHA`; eraser clears alpha).
- Export flattens to an off-screen FBO and calls `glReadPixels` once per
  export; the eyedropper does a single 1x1 `glReadPixels` at the tap point.
- A startup GLES capability check selects a CPU-compositing fallback on devices
  failing GLES 3.0 conformance (Mali/Adreno/PowerVR driver fragmentation).

### Engine public API (`dev.photodine.core.engine`)

Downstream modules depend only on these abstractions (bound in
`EngineModule` as `@Singleton`); GL/EGL types never leak past them.

- `Compositor` — engine entry point and primary test seam. UI-safe: every
  method posts to the private `"PhotodineGL"` render thread and returns
  immediately. Lifecycle: `initialiseCanvas(w, h)` allocates textures,
  `attachSurface(SurfaceTexture, w, h)` / `updateSurfaceSize` /
  `detachSurface` track the `TextureView`, `setViewTransform(zoom, ox, oy)`
  pans/zooms (zoom clamped 0.1x-32x via `MIN_ZOOM`/`MAX_ZOOM`),
  `requestRender()` kicks a frame, `release()` tears down. `capability` /
  `cpuFallback` expose the startup GLES 3.0 check outcome.
- `TextureStore` — GPU layer-texture storage: one RGBA8 layer texture plus
  the ping-pong accumulator FBO pair. GL-thread confined; owned by
  `GlCompositor`, never injected into features directly.
- `CanvasSize` — validated `width x height` (1..`MAX_DIMENSION` = 2048 per
  axis) shared by the engine and the new-canvas flow, including `PRESETS`.
- `GlesCapability` — `GL_VERSION` string plus `isEs3` / `cpuFallback` flags.
- `BlendMode` — Normal/Multiply/Screen/Overlay/Darken/Lighten enum; cached
  programs per mode arrive with the compositing tickets.
- Internals (`EglManager`, `GlTextureStore`, `GlUtils`, display shaders) stay
  package-visible to `:core:engine`. Ticket 02 draws a trivial
  passthrough/checkerboard display program; blend `.glsl` stubs are untouched.

### Canvas gestures (`:feature:canvas`)

- `CanvasTextureView` (a `TextureView` in `AndroidView`) owns touch:
  one-finger drag pans, two-finger pinch zooms via raw `MotionEvent`
  tracking. Gesture math is the pure `ViewTransform` value type
  (`withPan` / focus-preserving `withZoom`), unit-tested without Android.
- `CanvasViewModel` mirrors size + transform as MVI state and forwards the
  `Compositor` to the view; `NewCanvasViewModel` validates the bottom-sheet
  preset/custom-size flow and emits `NavigateToCanvas(w, h)`.

## Versions

AGP 8.7.3, Kotlin 2.2.20, Compose BOM 2026.08.00, Hilt 2.59.2 (KSP),
minSdk 29, compileSdk/targetSdk 36. Full catalog: `gradle/libs.versions.toml`.
