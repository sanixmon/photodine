# 09: Crop Tool

**What to build:** A user can trim the canvas to a new rectangular region. Dragging four handles on a crop overlay defines the region; all layers are cropped simultaneously when the user confirms. Cancel restores the original canvas. The result is a smaller canvas with all layer content outside the crop boundary permanently removed.

**Blocked by:** 04 — Layer Panel

**Status:** ready-for-agent

- [ ] `ActiveTool.Crop` is implemented in `:feature:tools`; selecting it displays the crop overlay
- [ ] Crop overlay: a Compose layer drawn over the full canvas showing a semi-transparent dark vignette outside the crop region and four draggable corner handles inside; handles are touch-target padded (min 48×48dp)
- [ ] The crop region is initialised to the full canvas dimensions; all four handles are individually draggable; the region is constrained to stay within canvas bounds and maintain a minimum size of 10×10px
- [ ] Confirm button (✓) commits the crop:
  - For each layer, a new `GL_TEXTURE_2D` of the new canvas dimensions is allocated; the crop region of the existing layer texture is blit into it via FBO; the old texture is freed
  - Canvas width/height in the engine is updated to the new dimensions
  - The `TextureView` layout is updated to reflect the new aspect ratio
- [ ] Cancel button (✗) dismisses the overlay with no texture changes
- [ ] Crop commit is registered as a `Command` in `HistoryManager` (from ticket 07 — stub if not yet merged): `undo()` restores all layer textures from their pre-crop snapshots and the previous canvas dimensions
- [ ] After crop, the canvas view re-centres and fits the new canvas dimensions on screen
