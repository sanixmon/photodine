#version 300 es
precision mediump float;
uniform sampler2D uDst;
uniform sampler2D uSrc;
uniform float uOpacity;
in vec2 vUV;
out vec4 outColor;

void main() {
    vec4 d = texture(uDst, vUV);
    vec4 s = texture(uSrc, vUV);
    float a = s.a * uOpacity;
    vec3 b = d.rgb * s.rgb;
    float outA = a + d.a * (1.0 - a);
    vec3 outRgb = outA > 0.0 ? mix(d.rgb, b, a) : vec3(0.0);
    outColor = vec4(outRgb, outA);
}
