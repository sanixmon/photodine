# 03: Multi-Layer Compositing + Blend Mode Shaders

**What to build:** The engine can hold multiple layers and composite them correctly. Each layer has a blend mode; the final image the user sees is the result of merging all visible layers bottom-to-top using the correct Porter-Duff blend formula. This ticket has no new UI — it is engine infrastructure validated entirely by instrumented pixel tests.

**Blocked by:** 02 — GLES Engine + Blank Canvas

**Status:** ready-for-agent

- [ ] Engine supports a flat ordered list of `Layer` objects; each layer is an independent `GL_TEXTURE_2D` RGBA8 texture
- [ ] Layer data model (from spec, not production code — encode the shape):
  ```
  data class Layer(
    val id: UUID,
    val textureId: Int,
    val blendMode: BlendMode,
    val opacity: Float,   // 0f–1f
    val visible: Boolean,
    val transform: LayerTransform
  )
  enum class BlendMode { NORMAL, MULTIPLY, SCREEN, OVERLAY, DARKEN, LIGHTEN }
  ```
- [ ] Compositing pass: bottom-to-top ping-pong FBO, one GLSL fragment shader draw per layer; invisible layers are skipped entirely
- [ ] Six blend mode GLSL shaders implemented as standalone `.glsl` files in `:core:engine`: Normal, Multiply, Screen, Overlay, Darken, Lighten; shaders are compiled once at engine init and cached by `BlendMode` enum key
- [ ] Layer opacity is applied as a uniform in the fragment shader (multiplied into the layer alpha before blending)
- [ ] `LayerTransform` (translate, scale, rotation) applied as a 3×3 matrix uniform on the layer's texture quad before compositing
- [ ] Flatten API: composites all visible layers into an off-screen FBO and returns a `ByteBuffer` of RGBA8 pixels — used by export (ticket 11); NOT called during interactive preview
- [ ] **Instrumented pixel tests** (on-device, off-screen EGL context): solid red bottom layer + 50%-opacity solid blue top layer → composite pixel at centre matches mathematically expected RGBA for each of the six blend modes; test covers opacity=0 (invisible) and opacity=1 (fully opaque) edge cases
