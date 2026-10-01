# 10: Gallery Import

**What to build:** A user can import any photo from their device gallery — either as the base layer when starting a new canvas, or as a new layer inserted above the current active layer on an existing canvas. If the photo is larger than 2048px on either axis it is automatically downscaled to fit. The imported photo immediately appears as a composited layer ready for painting or transforming.

**Blocked by:** 04 — Layer Panel

**Status:** ready-for-agent

- [ ] Import is triggered from two entry points: (a) the new canvas screen offers "Open from Gallery" as an alternative to blank canvas presets; (b) a "+" menu on the canvas screen offers "Import as Layer"
- [ ] Both paths use `ActivityResultContracts.GetContent("image/*")` to launch the system photo picker
- [ ] Selected `Uri` is decoded on a background coroutine via `ImageDecoder.decodeBitmap` (API 29+) with a target size listener that downscales proportionally if either dimension exceeds 2048px before decoding (avoids loading oversized bitmaps into RAM)
- [ ] Decoded `Bitmap` is uploaded to a new `GL_TEXTURE_2D` via `GLUtils.texImage2D` on the GL thread; the upload happens while a loading indicator is shown on the canvas screen
- [ ] **New canvas from photo**: engine is initialised with canvas dimensions matching the imported image (clamped to 2048×2048); the imported texture becomes layer 0 (bottom)
- [ ] **Import as layer on existing canvas**: a new `Layer` is created with the imported texture and inserted directly above the current active layer; `LayerTransform` is initialised to centre the imported image on the canvas (scaled down proportionally if the import is smaller than the canvas)
- [ ] If the user cancels the system picker (returns null `Uri`), no layer is created and no error is shown
- [ ] EXIF orientation is applied during decode (handle portrait photos correctly)
- [ ] Import action is registered as a `Command` in `HistoryManager` (stub if ticket 07 not yet merged): `undo()` deletes the imported layer and frees its texture
