#version 300 es
// Stub blend shader (see blend_normal.frag). b = 1 - (1 - D) * (1 - S).
precision mediump float;
uniform sampler2D uDst;
uniform sampler2D uSrc;
uniform float uOpacity;
in vec2 vUV;
out vec4 outColor;
void main() {
    vec4 d = texture(uDst, vUV);
    vec4 s = texture(uSrc, vUV);
    vec3 b = 1.0 - (1.0 - d.rgb) * (1.0 - s.rgb);
    outColor = vec4(mix(d.rgb, b, s.a * uOpacity), 1.0);
}
