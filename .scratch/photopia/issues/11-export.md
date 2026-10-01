# 11: Export — PNG/JPEG + MediaStore + Share

**What to build:** A user can export the finished composition as a PNG (transparency preserved) or JPEG (configurable quality). The file is saved to the device gallery so it appears in Photos and other gallery apps, and can optionally be shared directly to another app via the system share sheet. The exported image exactly matches what the user sees on canvas.

**Blocked by:** 03 — Multi-Layer Compositing + Blend Mode Shaders, 04 — Layer Panel

**Status:** ready-for-agent

- [ ] Export is triggered from a toolbar button or overflow menu on the canvas screen
- [ ] Export options bottom sheet: format selector (PNG / JPEG), JPEG quality slider (10–100, default 90), "Save to Gallery" and "Share" buttons
- [ ] Export pipeline (runs on a background coroutine, GL work dispatched to GL thread):
  1. Engine `flatten()` composites all visible layers into an off-screen FBO (same pipeline as ticket 03, not the preview FBO)
  2. `glReadPixels` reads the FBO into a `ByteBuffer` (RGBA8)
  3. `Bitmap.createBitmap` wraps the buffer; for JPEG the alpha channel is stripped (RGB bitmap) to avoid JPEG alpha corruption
  4. `Bitmap.compress(PNG/JPEG, quality, stream)` encodes the bitmap
  5. Written to `MediaStore.Images.Media` via `ContentResolver.insert` + `openOutputStream` (scoped storage, no `WRITE_EXTERNAL_STORAGE` permission needed on API 29+)
  6. `MediaScannerConnection` not required on API 29+ (MediaStore insert is sufficient)
- [ ] A progress indicator is shown during export; the UI remains interactive (export runs off the main thread)
- [ ] On success: a snackbar with "Saved to Gallery" and an optional "Share" action appears; tapping Share fires `Intent.ACTION_SEND` with the MediaStore `Uri` and `FileProvider` backing
- [ ] On failure (storage full, permission denied): a snackbar with the error reason is shown; no crash
- [ ] **Instrumented integration test**: given two known-colour layers (solid red bottom, 50%-opacity solid blue top, both fully visible), export as PNG, decode the output file, assert the centre pixel RGBA matches the mathematically expected composite colour for Normal blend mode
