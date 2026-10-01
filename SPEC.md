# Photodine v1 — Product & Engineering Spec

## Problem Statement

Mobile photo editing on Android is dominated by closed-source, subscription-gated apps (Adobe Photoshop Express, Lightroom Mobile) or oversimplified filter apps. There is no high-quality, open-source, layer-based photo editor on Android that treats the user as a creative professional while remaining free and community-driven. Users who want real compositing control — layers, blend modes, non-destructive per-layer opacity — have no viable open alternative.

## Solution

Photodine is an open-source, layer-based photo editor for Android built with Kotlin and Jetpack Compose. It provides a GPU-accelerated compositing engine, a natural brush tool with smooth stroke interpolation, and an intuitive mobile-native UI. Users can import photos from their gallery, composite them as layers, paint on top, and export the final result as PNG or JPEG. The project is MIT-licensed and hosted on GitHub, funded by community sponsorship.

## User Stories

### Canvas & Project

1. As a user, I want to create a new blank canvas at a preset size (1080×1080, 1920×1080, 2048×2048), so that I can start a project without guessing dimensions.
2. As a user, I want to create a new canvas at a custom width and height (up to 2048px on either axis), so that I can work at the exact resolution I need.
3. As a user, I want to import a photo from my device gallery as the base layer of a new canvas, so that I can edit an existing photo immediately.
4. As a user, I want to import a photo from my device gallery as a new layer on top of my existing canvas, so that I can composite multiple photos together.
5. As a user, I want to pinch-to-zoom and pan the canvas freely during editing, so that I can work on fine details without losing context.
6. As a user, I want the canvas to display at a resolution matching my screen pixel density, so that I see an accurate preview of my work at all times.

### Layer Management

7. As a user, I want to see all my layers listed in a bottom sheet panel, so that I can manage them without losing view of the canvas.
8. As a user, I want to add a new blank layer above the current layer, so that I can paint on a separate surface without affecting existing content.
9. As a user, I want to delete a layer, so that I can remove content I no longer need.
10. As a user, I want to duplicate a layer, so that I can iterate on a copy without destroying the original.
11. As a user, I want to reorder layers by dragging them in the layer panel, so that I can control which content appears in front.
12. As a user, I want to toggle a layer's visibility on and off, so that I can preview the composition with and without specific layers.
13. As a user, I want to set a layer's opacity via a slider (0–100%), so that I can blend layers at any transparency.
14. As a user, I want to select a blend mode per layer from: Normal, Multiply, Screen, Overlay, Darken, Lighten, so that I can control how the layer composites with the layers beneath it.
15. As a user, I want to tap a layer thumbnail in the panel to make it the active layer, so that subsequent tool actions affect only that layer.

### Brush Tool

16. As a user, I want to select the brush tool and paint on the active layer, so that I can add content with a round hard brush.
17. As a user, I want to set the brush size via a slider, so that I can paint with fine detail or broad strokes.
18. As a user, I want to set the brush opacity via a slider, so that I can paint with partial transparency.
19. As a user, I want my brush strokes to be smoothed via Catmull-Rom spline interpolation, so that strokes feel natural and not jagged even during fast gestures.
20. As a user with a Samsung S Pen or USI stylus, I want brush size and opacity to respond to hardware pressure via `MotionEvent.AXIS_PRESSURE`, so that I get a natural drawing experience.

### Eraser Tool

21. As a user, I want to select the eraser tool and erase pixels on the active layer, so that I can remove painted content non-destructively relative to other layers.
22. As a user, I want to set eraser size via a slider, so that I can erase with precision or speed.
23. As a user, I want the eraser to respect layer boundaries — it only affects the active layer, so that lower layers remain intact.

### Move & Transform Tool

24. As a user, I want to select the move tool and drag the active layer to reposition it, so that I can adjust composition placement.
25. As a user, I want to pinch-scale the active layer while in transform mode, so that I can resize a layer independently of the canvas.
26. As a user, I want to rotate the active layer with a two-finger rotate gesture, so that I can adjust layer orientation.
27. As a user, I want to see a bounding box around the layer while transforming, so that I have clear visual feedback of the transform state.

### Crop Tool

28. As a user, I want to select the crop tool and drag handles to define a crop region, so that I can trim the canvas to my desired framing.
29. As a user, I want to confirm or cancel a crop operation, so that I can preview and undo before committing.
30. As a user, I want crop to affect all layers simultaneously (canvas-level crop), so that the entire composition is trimmed consistently.

### Color Picker

31. As a user, I want to open a color picker and select a color via an HSV wheel, so that I can choose any hue with fine control.
32. As a user, I want to type a hex color code directly, so that I can use exact brand or reference colors.
33. As a user, I want to adjust RGB channel values individually via sliders, so that I can make precise color adjustments.
34. As a user, I want to use the eyedropper tool to sample any color currently visible on the canvas, so that I can match colors from imported photos.
35. As a user, I want to see a palette of my recently used colors, so that I can quickly reuse colors across sessions.

### Undo & Redo

36. As a user, I want to undo the last action via a button or gesture, so that I can recover from mistakes.
37. As a user, I want to redo an undone action, so that I can restore intentional changes.
38. As a user, I want up to 50 undo steps preserved per session, so that I have sufficient history for a realistic editing workflow.
39. As a user, I want undo/redo to cover all destructive actions: brush strokes, eraser strokes, layer add/delete/reorder/duplicate/opacity/blend mode/visibility changes, transforms, and crops.

### Export

40. As a user, I want to export my composition as a PNG with full transparency preserved, so that I can use the result in other apps with alpha intact.
41. As a user, I want to export my composition as a JPEG with a configurable quality level, so that I can produce smaller files for sharing.
42. As a user, I want the exported file to be saved to my device gallery (MediaStore), so that it appears in Photos and other gallery apps.
43. As a user, I want to share the exported file directly to another app (share sheet), so that I can send my work without saving to gallery first.
44. As a user, I want export to flatten all visible layers in order, respecting each layer's opacity and blend mode, so that the output matches what I see on canvas.

### General UX

45. As a user, I want the active tool options (size, opacity) to appear in a bottom sheet that does not obscure the canvas center, so that I can adjust settings while seeing the canvas.
46. As a user, I want the layer panel and tool options panel to be dismissible, so that I can use the full screen for painting.
47. As a user, I want the app to work fully offline with no account required, so that I have no dependency on external services.
48. As a user, I want the app to be available for free on GitHub (sideload or F-Droid), so that I can install it without the Play Store if I choose.

## Implementation Decisions

### Module Structure
The project uses a Gradle multi-module layout from the start to enable isolated contribution and clear ownership boundaries:

- **`:app`** — Application entry point, Compose `NavHost`, DI graph wiring, top-level MVI store
- **`:core:engine`** — OpenGL ES 3.0 compositing pipeline: FBO management, shader compilation/cache, texture lifecycle, layer flattening. No Android UI dependencies; testable in isolation via off-screen EGL context
- **`:core:ui`** — Shared Compose design system: tokens (color, spacing, type), reusable components (sliders, bottom sheets, icon buttons)
- **`:feature:canvas`** — `CanvasView` (`TextureView` hosted in `AndroidView`), gesture handling (pan/zoom via `PointerInput`, stroke capture via `MotionEvent` batching), Catmull-Rom spline interpolation, real-time preview render loop
- **`:feature:layers`** — Layer panel bottom sheet, drag-to-reorder, blend mode picker, opacity slider, visibility toggle, layer thumbnail rendering
- **`:feature:tools`** — Tool state (active tool enum), brush/eraser parameter state (size, opacity), transform mode state, crop overlay
- **`:feature:colorpicker`** — HSV wheel, hex input, RGB sliders, eyedropper activation event, recent colors ring buffer
- **`:feature:export`** — Flatten pipeline (delegating to `:core:engine`), PNG/JPEG encoding, MediaStore write, Android Share intent

### App Architecture — MVI
All features follow unidirectional MVI: `Intent → Reducer → State → UI`. Each feature module exposes a `ViewModel` with a `StateFlow<UiState>` and an `Intent` sealed class. Side effects (navigation, export I/O, MediaStore) are emitted via a `SharedFlow<Effect>`.

### Rendering Engine — OpenGL ES 3.0
- Each layer is stored as a GPU `GL_TEXTURE_2D` (RGBA8, 2048×2048 max).
- Compositing uses ping-pong FBOs: layers are merged bottom-to-top into an accumulator texture, each pass executing a GLSL fragment shader that implements the layer's blend mode.
- Blend mode shaders are compiled once and cached by mode enum at engine init.
- The `TextureView`'s `SurfaceTexture` is the final render target; no intermediate CPU readback during preview.
- Export (flatten to bitmap) uses a separate off-screen FBO rendered to a `GL_RGBA8` pixel buffer, then read back via `glReadPixels` once per export — not during interactive preview.

### Stroke Rendering
- `MotionEvent` historical points are collected per `ACTION_MOVE` batch.
- Points are fitted to a Catmull-Rom spline; stamps (textured quads) are placed at intervals of `spacing = brushSize * 0.2` along the curve arc length.
- Each stamp is rendered as a pre-multiplied alpha quad into the active layer's FBO using `GL_ONE, GL_ONE_MINUS_SRC_ALPHA` blending.
- Eraser uses the same stamp pipeline with a `GL_ZERO, GL_ONE_MINUS_SRC_ALPHA` blend (alpha clear).

### Layer Data Model
```
// Decision-rich type shape (not production code)
data class Layer(
  val id: UUID,
  val textureId: Int,          // OpenGL texture handle
  val blendMode: BlendMode,
  val opacity: Float,          // 0f–1f
  val visible: Boolean,
  val transform: LayerTransform // translate, scale, rotation
)

enum class BlendMode { NORMAL, MULTIPLY, SCREEN, OVERLAY, DARKEN, LIGHTEN }
```
Flat list; no group/clip layers in v1.

### Undo/Redo — Command Pattern
Every mutating action produces a `Command` object with `execute()` and `undo()`. Commands that affect GPU texture state snapshot the affected texture region (dirty rect) as a CPU-side `ByteBuffer` for reversal. A fixed-size deque of 50 commands is kept in the `:core:engine` `HistoryManager`; oldest commands are evicted and their snapshots freed.

### Color Picker — Eyedropper
Eyedropper triggers a single `glReadPixels` call on the composite FBO at the tapped pixel coordinate. This is the only interactive CPU readback path and is intentionally one-shot (not streaming).

### Import Flow
Gallery import uses `ActivityResultContracts.GetContent("image/*")`. The selected `Uri` is decoded via `ImageDecoder` (API 29+) into a hardware `Bitmap`, uploaded to a new `GL_TEXTURE_2D` via `GLUtils.texImage2D`, and appended as a new `Layer`. If the imported image exceeds 2048px on either axis it is downscaled to fit before upload.

### Export Flow
1. Compose all visible layers into an off-screen FBO (reusing `:core:engine` flatten pipeline).
2. `glReadPixels` → `ByteBuffer` → `Bitmap.wrapHardwareBuffer` or `Bitmap.createBitmap`.
3. Compress to PNG or JPEG via `Bitmap.compress`.
4. Write to `MediaStore.Images` using `ContentResolver` (scoped storage, API 29+).
5. Optionally fire `Intent.ACTION_SEND` with the `Uri` for share sheet.

### Minimum SDK
API 29 (Android 10). Gives: `ImageDecoder`, `BlendMode` (unused in v1 but available), scoped storage `MediaStore` API, guaranteed OpenGL ES 3.2 support across target devices.

## Testing Decisions

### What Makes a Good Test
Tests verify externally observable behavior, not implementation details. Tests do not assert on private state, internal class names, or shader source text. A test should fail when user-visible behavior breaks and pass when it works correctly — regardless of how the internals are structured.

### Modules to Test

**`:core:engine`**
- Off-screen EGL context (robolectric or on-device instrumented) to verify:
  - Layer compositing produces correct output pixels for each blend mode (pixel-level assertions on known input textures)
  - Flatten produces correct RGBA output when layers have varying opacities and visibility
  - Undo/redo via `HistoryManager` correctly restores texture state after a stroke
- These are the highest-value tests: the engine is the only module whose correctness cannot be visually spot-checked without human review

**`:feature:layers`**
- Unit tests on the MVI reducer: given a state and an intent (add layer, delete, reorder, opacity change), assert the correct new state is emitted
- No UI tests needed; state machine is the seam

**`:feature:export`**
- Integration test: given a known two-layer input (solid red bottom, 50%-opacity solid blue top), assert the exported PNG pixel at center is the correct composite color

**`:feature:colorpicker`**
- Unit tests on HSV↔RGB conversion math

### Prior Art
No existing tests in the repo (greenfield). Test conventions to establish: JUnit 5 + Kotlin, `kotlinx-coroutines-test` for `StateFlow`, `mockk` for mocks, on-device instrumented tests for anything requiring an EGL/OpenGL context.

### Seam
The primary test seam is **`:core:engine`'s public API** — the compositing and history functions. All feature-level tests go through MVI reducer state, not Compose UI. This gives maximum coverage at minimum seam count. UI/Compose tests are explicitly out of scope for v1 unless a Compose-level regression is found.

## Out of Scope

- Native project file format (`.ora` or proprietary) — export is flatten-only in v1
- Tiled rendering for canvases larger than 2048px on either axis
- Adjustment layers (brightness, contrast, curves, HSL)
- Selection tools (rectangular, lasso, magic wand)
- Text layers
- Vector/shape tools
- Velocity-based or pressure-simulated brush dynamics beyond hardware stylus `AXIS_PRESSURE`
- Non-destructive filter pipeline
- Cloud sync or account system
- iOS / cross-platform (KMP, Flutter)
- In-app tutorial or onboarding
- Play Store distribution (F-Droid / sideload sufficient for v1)
- Localization (English only for v1)

## Further Notes

- **Open-source strategy**: MIT license maximizes adoption and forkability. GitHub Sponsors page should be set up at repo creation. `CONTRIBUTING.md` and `ARCHITECTURE.md` are first-class deliverables alongside code.
- **Blend mode shader authorship**: Each blend mode shader should be a standalone `.glsl` file in `:core:engine/src/main/glsl/` to make community contribution of new blend modes a self-contained, reviewable PR.
- **Driver fragmentation risk**: OpenGL ES 3.0 driver quality varies across Mali (Samsung/MediaTek budget devices), Adreno (Snapdragon), and PowerVR (older chipsets). The engine should include a driver capability check at startup and a fallback code path (CPU compositing via `android.graphics.BlendMode`) for devices that fail GLES 3.0 conformance tests.
- **Future project file**: When native save/load is added, OpenRaster (`.ora`) is the preferred format — it is an open ZIP-based standard with existing Rust/Python parsers and aligns with the project's open-source ethos.
- **Name**: Project name is **Photodine**. Package namespace: `dev.photodine`.
