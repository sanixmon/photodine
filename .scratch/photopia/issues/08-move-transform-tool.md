# 08: Move + Transform Tool

**What to build:** A user can select the Move tool and freely reposition, scale, and rotate the active layer. A bounding box gives clear visual feedback of the current transform. Transforms are non-destructive to the layer texture — the transform matrix is stored on the `Layer` and applied at composite time.

**Blocked by:** 04 — Layer Panel

**Status:** ready-for-agent

- [ ] `ActiveTool.Move` is implemented in `:feature:tools`; selecting it exits brush/eraser stroke capture mode
- [ ] While Move tool is active, the canvas intercepts touch events for transform gestures instead of stroke input:
  - Single-finger drag → translate (`LayerTransform.translateX/Y`)
  - Two-finger pinch → uniform scale (`LayerTransform.scale`), clamped to 0.05×–20×
  - Two-finger rotate → rotation in degrees (`LayerTransform.rotationDeg`)
- [ ] `LayerTransform` is applied as a 3×3 affine matrix uniform in the compositing shader for that layer; the underlying texture pixels are not modified
- [ ] A bounding box (four corner handles + centre cross) is rendered as a Compose overlay on top of `CanvasView` while the Move tool is active, accurately reflecting the current transform (accounting for canvas pan/zoom)
- [ ] Transforms are registered as `Command` objects in `HistoryManager` (from ticket 07 — acceptable to stub if 07 is not yet merged; stub should be wired, not silent no-op): `undo()` restores the previous `LayerTransform`
- [ ] Switching away from the Move tool commits the transform; the bounding box overlay disappears
- [ ] Switching the active layer while Move tool is active immediately shows the new layer's bounding box
