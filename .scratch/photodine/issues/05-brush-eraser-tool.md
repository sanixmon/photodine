# 05: Brush + Eraser Tool

**What to build:** A user can select the brush tool and paint smooth strokes onto the active layer in any colour, at any size and opacity. They can switch to the eraser and remove pixels from the active layer without affecting layers beneath. Strokes feel natural due to Catmull-Rom spline smoothing. S Pen and USI stylus pressure is honoured when available.

**Blocked by:** 04 — Layer Panel

**Status:** ready-for-agent

- [ ] `:feature:tools` defines `ActiveTool` enum (Brush, Eraser, Move, Crop — stubs for Move/Crop sufficient here), `BrushState` (size: 1–500px, opacity: 0–100%, colour: RGBA)
- [ ] `CanvasView` in `:feature:canvas` captures `MotionEvent` (`ACTION_DOWN`, `ACTION_MOVE` with `getHistoricalX/Y`, `ACTION_UP`) and forwards point batches to the stroke pipeline
- [ ] Stroke pipeline: raw points are fitted to a Catmull-Rom spline; hard-round brush stamps (textured quads, Gaussian alpha falloff baked into texture) are placed every `brushSize × 0.2` px along arc length
- [ ] Brush stamps rendered into the active layer's FBO using pre-multiplied alpha blending (`GL_ONE, GL_ONE_MINUS_SRC_ALPHA`)
- [ ] Eraser uses the identical stamp pipeline with `GL_ZERO, GL_ONE_MINUS_SRC_ALPHA` blend — only the active layer is affected; layers beneath are untouched
- [ ] S Pen / USI stylus: `MotionEvent.AXIS_PRESSURE` is read per point; when non-zero (hardware pressure present) it scales brush size by `0.5 + pressure × 0.5` — falls back silently to fixed size on touch input
- [ ] Tool options bottom sheet (dismissible, does not cover canvas centre): brush size slider, opacity slider, active colour swatch (tapping opens colour picker stub — full picker in ticket 06); eraser shows size slider only
- [ ] Brush tool defaults to black (`#000000`, 100% opacity, 20px); eraser defaults to 30px
- [ ] Active layer dirty region is composited and pushed to `TextureView` after each `ACTION_UP` (end of stroke); during a stroke, intermediate stamps are batched and pushed at ~60 fps via the GL thread
