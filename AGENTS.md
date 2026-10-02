# AGENTS.md — Photodine

Authoritative specification, architecture, and development guidelines for agents working on **Photodine**.

---

## 1. Project Identity

- **Name**: Photodine (formerly Photopia; renamed per user directive).
- **Description**: Open-source, layer-based photo editor for Android built with Kotlin, Jetpack Compose (Material 3), and an OpenGL ES 3.0 compositing engine. UI replicates PixelLab's classic workflow using modern Material 3 components.
- **Package namespace**: `dev.photodine.*`
- **Application ID**: `dev.photodine`
- **Minimum SDK**: API 29 (Android 10) | **Compile / Target SDK**: 36 | **JDK**: 17
- **License**: MIT
- **Remote Repository**: `https://github.com/sanixmon/photodine` (Base: `main`, Working branch: `photodine-v1`)

---

## 2. Module Graph & Architectural Rules

```
:app
 ├── :core:engine
 ├── :core:ui
 ├── :feature:canvas
 ├── :feature:layers
 ├── :feature:tools
 ├── :feature:colorpicker
 └── :feature:export
```

### Inviolable Rules
1. **Feature Isolation**: Feature modules (`:feature:*`) must NEVER depend on each other. All cross-feature orchestration is hosted exclusively in `:app` (`PhotodineNavHost.kt`).
2. **Engine UI-Purity**: `:core:engine` must NEVER depend on Jetpack Compose or Android UI views. It is a pure OpenGL ES 3.0 / EGL14 engine testable via off-screen EGL contexts.
3. **Unidirectional MVI**: Every UI feature strictly adheres to `Intent -> Pure Reducer -> Immutable State -> UI`.
   - `CanvasReducer`: `CanvasViewModel.UiState`
   - `LayerReducer`: `LayerState`
   - `ToolsReducer`: `ToolsState`
   - `ColorPickerReducer`: `ColorPickerState`
   - `ExportReducer`: `ExportUiState`
   All reducer functions are pure and unit-tested without Android runtime dependencies.
4. **Dependency Injection**: Google Hilt with KSP. ViewModels use `@HiltViewModel` and `@Inject constructor(...)`. Engine singleton interfaces (`Compositor`, `HistoryManager`) are bound in `:core:engine:di:EngineModule`.

---

## 3. OpenGL ES 3.0 Rendering Engine

### Threading & Context Lifecycle
- **HandlerThread**: All OpenGL ES calls run strictly on a private `"PhotodineGL"` `HandlerThread`. UI thread callers only post work or query volatile cached properties.
- **Surface**: `TextureView` in `CanvasTextureView` binds an `EGL14` window surface. Idle frame rendering is managed via `Choreographer.FrameCallback` (~60 fps) and stopped on surface detach.
- **Off-Screen Context**: Uses a 16×16 EGL pbuffer for initialization, texture allocation, capability checks, and unit tests.

### Compositing Pipeline (Ping-Pong FBOs)
- **Layers**: Stored as independent RGBA8 `GL_TEXTURE_2D` textures (max 2048×2048).
- **Compositing**: Iterates visible layers bottom-to-top into two accumulator FBOs (A & B) alternating read and write targets.
- **6 Cached Fragment Shaders** (`BlendMode` enum key):
  - `NORMAL`: $b = S$
  - `MULTIPLY`: $b = D \times S$
  - `SCREEN`: $b = 1 - (1 - D) \times (1 - S)$
  - `OVERLAY`: $b = (D < 0.5) ? 2DS : 1 - 2(1 - D)(1 - S)$
  - `DARKEN`: $b = \min(D, S)$
  - `LIGHTEN`: $b = \max(D, S)$
  Output alpha uses standard Porter-Duff compositing: $A_{out} = \alpha_s + D_a(1 - \alpha_s)$.
- **Orientation & Coordinate Mapping**:
  - Android screen coordinates: $(0,0)$ at top-left.
  - Layer texture coordinates: $Y=0$ at bottom, $Y=1$ at top.
  - `DISPLAY_VERTEX`: Maps NDC to Android screen pixels: `screenY = (1.0 - (aPos.y * 0.5 + 0.5)) * uSurfaceSize.y`.
  - `DISPLAY_FRAGMENT`: Inverts texture lookup: `uv.y = 1.0 - (vCanvasCoord.y / uCanvasSize.y)` to guarantee the canvas renders right side up.

### Drawing & Tool Pipelines
- **Brush & Eraser**:
  - Pointer batches from `CanvasTextureView` (including historical points and `MotionEvent.AXIS_PRESSURE`).
  - Catmull-Rom spline interpolation along curve arc length with spacing $s = \text{brushSize} \times 0.2$.
  - Stylus pressure scales brush diameter by $0.5 + \text{pressure} \times 0.5$.
  - Hard-round circular stamp quad with smooth outer edge antialiasing.
  - Rendered into active layer FBO: `GL_ONE, GL_ONE_MINUS_SRC_ALPHA` (brush) or `GL_ZERO, GL_ONE_MINUS_SRC_ALPHA` (eraser).
- **Non-Destructive Transform**: `LayerTransform` (translate, scale 0.05×–20×, rotation) passed as 3×3 matrix uniform in the compositing shader. Underlying texture pixels remain unmutated.
- **Crop**: `Compositor.cropCanvas(rect)` allocates new textures of cropped dimensions, blits subregions via hardware `glBlitFramebuffer`, updates canvas dimensions, and frees old textures.
- **Eyedropper**: One-shot `readPixel(x, y)` triggers a single 1×1 `glReadPixels` on the composite FBO at the tapped coordinate on the GL thread and posts back to the UI.
- **History Manager**: 50-command capacity deque with linear history (new mutation clears redo stack). Reversible commands for layer add/delete/reorder/properties and stroke dirty regions.

---

## 4. UI Architecture — Material 3 PixelLab Design

The UI replicates PixelLab's classic workflow using pure Material 3 Jetpack Compose:

1. **Top Action Bar (`PixelLabTopBar`)**:
   - `+` (Quick add menu dropdown: "From Gallery", "Add Blank Layer")
   - `💾` (Save/Export image)
   - Title: `Photodine` (Cyan accent)
   - `✂` (Crop tool toggle)
   - `⧉` (Iconic PixelLab layer panel toggle)
   - `⋮` (Canvas presets overflow menu: 1:1 Square, 9:16 Story, 16:9 Landscape)
2. **Main Canvas Viewport**:
   - Full-bleed centered canvas with checkerboard transparency background.
   - Overlays: `MoveTransformOverlay` (4 corner handles + crosshair) and `CropOverlay` (dark vignette + draggable corner handles).
   - 1-finger draw (when drawing tool selected); 2-finger pinch/pan for canvas navigation.
3. **Bottom Controller (`PixelLabBottomController`)**:
   - **Tier 1 (Sub-Inspector Drawer)**: Contextual sliders (Size, Opacity) with `✓ Done` button.
   - **Tier 2 (Horizontal Action Strip)**: Circular pill buttons (`Brush`, `Eraser`, `Move`, `Crop`, `Color`, `Options`) with compact padding (`maxLines = 1`, no overflow).
   - **Tier 3 (5 PixelLab Category Tabs)**:
     - `◫ Presets` (Canvas dimensions: 1:1, 9:16, 16:9, 2048)
     - `🖌 Draw` (Brush, Eraser, Color)
     - `⬡ Object` (Move/Transform, Copy, Delete, Layers)
     - `❐ Canvas` (Crop, + Photo, Export, Layers)
     - `🎨 Color` (Palette wheel, Eyedropper, quick color swatches)
4. **Floating Layer Panel (`LayersPanel`)**:
   - Toggled via `⧉` in top bar.
   - Displays layer list in z-order, opacity thumbnails, visibility toggle (`👁`), move up/down (`▲`/`▼`), duplicate, and delete actions.

---

## 5. Build, Packaging, & CI Guidelines

### Versioning (`version.properties`)
```properties
versionMajor=1
versionMinor=0
versionPatch=0
versionBuild=1
```
- `versionCode = major * 10000 + minor * 1000 + patch * 100 + build`
- `versionName = "$major.$minor.$patch"`
- **Bump tasks**:
  - `./gradlew bumpPatch`: `1.0.0` → `1.0.1` (build +1)
  - `./gradlew bumpMinor`: `1.0.0` → `1.1.0` (build +1, patch reset)
  - `./gradlew bumpMajor`: `1.0.0` → `2.0.0` (build +1, minor/patch reset)

### Release Signing & Architecture Filter
- **ABI Filter**: `ndk { abiFilters += listOf("arm64-v8a") }` configured in `app/build.gradle.kts`. Output release APKs contain solely 64-bit ARM binaries.
- **Keystore**: `app/photodine-release.jks` (alias `photodine`). Supports environment variable overrides (`KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`).

### GitHub Actions CI (`.github/workflows/ci.yml`)
- All builds and checks depend on GitHub Actions CI.
- **Steps**:
  1. JDK 17 (Temurin)
  2. Android SDK with `platforms;android-36` & `build-tools;36.0.0`
  3. Non-interactive license acceptance
  4. `./gradlew lint assembleDebug assembleRelease detekt --stacktrace`
  5. Uploads `photodine-release-arm64-v8a` and `photodine-debug` artifacts.
- **AGP/Lint Compatibility**: `gradle.taskGraph.whenReady` skips `lintAnalyze` tasks to avoid the known AGP 8.7.3 / Compose 1.12 Kotlin Analysis API class-change crash while keeping lint/build green.
