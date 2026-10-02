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
    vec3 b = vec3(
        (d.r < 0.5) ? (2.0 * d.r * s.r) : (1.0 - 2.0 * (1.0 - d.r) * (1.0 - s.r)),
        (d.g < 0.5) ? (2.0 * d.g * s.g) : (1.0 - 2.0 * (1.0 - d.g) * (1.0 - s.g)),
        (d.b < 0.5) ? (2.0 * d.b * s.b) : (1.0 - 2.0 * (1.0 - d.b) * (1.0 - s.b))
    );
    float outA = a + d.a * (1.0 - a);
    vec3 outRgb = outA > 0.0 ? mix(d.rgb, b, a) : vec3(0.0);
    outColor = vec4(outRgb, outA);
}
