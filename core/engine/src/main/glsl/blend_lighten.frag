#version 300 es
// Stub blend shader (see blend_normal.frag). b = max(D, S).
precision mediump float;
uniform sampler2D uDst;
uniform sampler2D uSrc;
uniform float uOpacity;
in vec2 vUV;
out vec4 outColor;
void main() {
    vec4 d = texture(uDst, vUV);
    vec4 s = texture(uSrc, vUV);
    outColor = vec4(mix(d.rgb, max(d.rgb, s.rgb), s.a * uOpacity), 1.0);
}
