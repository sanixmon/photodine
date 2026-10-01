# 04: Layer Panel

**What to build:** A user can manage all their layers from a bottom sheet panel without losing view of the canvas. They can add blank layers, delete, duplicate, reorder by drag, toggle visibility, adjust opacity, change blend mode, and select the active layer. Every change is reflected instantly in the composite canvas render.

**Blocked by:** 03 — Multi-Layer Compositing + Blend Mode Shaders

**Status:** ready-for-agent

- [ ] `:feature:layers` implements MVI: `LayerIntent` sealed class (AddLayer, DeleteLayer, DuplicateLayer, ReorderLayer, SetVisibility, SetOpacity, SetBlendMode, SelectLayer), `LayerState` (list of layers, activeLayerId), `LayerViewModel`
- [ ] Layer panel is a persistent bottom sheet (not modal) anchored at the bottom of the canvas screen; it is collapsible so the user can paint full-screen
- [ ] Layer list renders a thumbnail of each layer's texture (small GPU readback on demand, not every frame), the layer name, visibility eye icon, and active-layer highlight
- [ ] Drag-to-reorder via long-press + drag within the layer list; reorder is committed on drop and triggers a re-composite
- [ ] Tapping a layer row makes it the active layer (subsequent tool actions target this layer)
- [ ] Per-layer controls accessible via tapping a disclosure area on the row: opacity slider (0–100%), blend mode picker (six modes from spec), visibility toggle
- [ ] Add layer button appends a blank transparent RGBA8 texture above the current active layer
- [ ] Delete and duplicate via swipe-action or long-press context menu on a row
- [ ] **Unit tests on the MVI reducer**: given each `LayerIntent`, assert the correct `LayerState` is produced — covers add, delete, duplicate, reorder (including boundary: move top layer up, move bottom layer down), opacity clamp (0f–1f), blend mode assignment, visibility toggle
