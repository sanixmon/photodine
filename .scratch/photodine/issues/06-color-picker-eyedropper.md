# 06: Color Picker + Eyedropper

**What to build:** A user can choose any brush colour via an HSV wheel, hex code, or RGB sliders. They can tap the eyedropper and then tap anywhere on the canvas to sample the exact colour at that point. Recently used colours are remembered so frequently used colours are one tap away.

**Blocked by:** 05 — Brush + Eraser Tool

**Status:** ready-for-agent

- [ ] `:feature:colorpicker` implements a full-screen bottom sheet or modal with three input surfaces: HSV wheel (hue ring + saturation/value triangle), hex input field (6-char, validates on submit), RGB sliders (0–255 per channel)
- [ ] All three input surfaces stay in sync — changing any one updates the others in real time
- [ ] Opacity (alpha) slider is present in the picker and directly sets `BrushState.opacity`
- [ ] Recent colours: a horizontal row of up to 10 swatches stored as a ring buffer in `BrushState`; tapping a swatch applies that colour and dismisses the picker; most recently used is added on picker close
- [ ] Eyedropper: tapping the eyedropper icon in the picker (or toolbar) dismisses the picker and puts the canvas in eyedropper mode; next canvas tap triggers a one-shot `glReadPixels` on the composite FBO at the tapped canvas coordinate (pixel-accurate after accounting for zoom/pan transform); sampled colour is applied as the new brush colour and eyedropper mode exits
- [ ] Eyedropper `glReadPixels` runs on the GL thread; result is posted back to the UI thread before updating `BrushState`
- [ ] Colour swatch in the tool options bottom sheet (from ticket 05) opens this picker
- [ ] **Unit tests**: HSV→RGB and RGB→HSV round-trip conversion for boundary values (pure red, pure green, pure blue, white, black, mid-grey); hex parse/format for valid and invalid strings
