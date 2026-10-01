# 02: GLES Engine + Blank Canvas

**What to build:** The app opens to a usable blank canvas. A user can create a new canvas at a preset size (1080×1080, 1920×1080, 2048×2048) or a custom size up to 2048px on either axis. The canvas renders via OpenGL ES 3.0 and supports smooth pinch-to-zoom and two-finger pan. This is the rendering foundation every subsequent ticket builds on.

**Blocked by:** 01 — Project Scaffold

**Status:** ready-for-agent

- [ ] `:core:engine` initialises an OpenGL ES 3.0 context; startup performs a GLES 3.0 conformance capability check and logs a warning (with CPU-fallback flag) if the device driver fails
- [ ] A single RGBA8 `GL_TEXTURE_2D` (up to 2048×2048) is allocated as the sole layer texture; a ping-pong FBO pair is created for compositing
- [ ] The composite result is rendered to a `TextureView` surface with no intermediate CPU readback; frame rate targets 60 fps during idle (no input)
- [ ] `:feature:canvas` wraps the `TextureView` in a Compose `AndroidView` (`CanvasView`); pan (one-finger drag) and zoom (pinch) are handled via `MotionEvent` on the view — zoom clamped to 0.1×–32×
- [ ] New canvas flow: bottom sheet or dialog offers preset sizes + custom W×H input; tapping a preset or confirming custom size initialises the engine and navigates to the canvas screen
- [ ] Canvas background renders as a checkerboard pattern (indicating transparency) behind the layer texture
- [ ] `:core:engine` public API is defined (interfaces/classes) so downstream modules can depend on it without implementation details leaking; documented in ARCHITECTURE.md
