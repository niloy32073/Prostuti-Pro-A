package com.prostuti.student.ui.screen.playground.graph.space

internal object SpaceShaders {

    const val VERTEX = """#version 300 es
layout(location = 0) in vec3 aPos;
layout(location = 1) in vec3 aNormal;
uniform mat4 uMvp;
uniform float uScale;
out vec3 vPos;
out vec3 vNormal;
void main() {
    vPos = aPos * uScale;
    vNormal = aNormal;
    gl_Position = uMvp * vec4(vPos, 1.0);
}
"""

    /**
     * Blinn–Phong with a key light near the camera and a soft fill from the other side. Normals are
     * flipped towards the viewer, so both sides of a surface are lit (implicit meshes have no
     * consistent "outside"). Fragments outside the box are discarded: exact clipping at any zoom.
     */
    const val FRAGMENT = """#version 300 es
precision highp float;
in vec3 vPos;
in vec3 vNormal;
uniform vec4 uColor;
uniform float uBox;
uniform int uLit;
uniform int uHeight;
uniform vec2 uHeightRange;
uniform vec3 uLightDir;
uniform vec3 uEye;
out vec4 fragColor;

vec3 heightColor(float t) {
    vec3 low = vec3(0.16, 0.30, 0.78);
    vec3 mid = vec3(0.10, 0.68, 0.62);
    vec3 high = vec3(0.98, 0.84, 0.24);
    return t < 0.5 ? mix(low, mid, t * 2.0) : mix(mid, high, (t - 0.5) * 2.0);
}

void main() {
    if (uBox > 0.0 && any(greaterThan(abs(vPos), vec3(uBox)))) discard;
    vec3 base = uColor.rgb;
    if (uHeight == 1) {
        float t = clamp((vPos.z - uHeightRange.x) / max(uHeightRange.y - uHeightRange.x, 1e-6), 0.0, 1.0);
        base = heightColor(t);
    }
    if (uLit == 0) {
        fragColor = vec4(base, uColor.a);
        return;
    }
    vec3 n = normalize(vNormal);
    vec3 v = normalize(uEye - vPos);
    if (dot(n, v) < 0.0) n = -n;
    float diffuse = max(dot(n, uLightDir), 0.0);
    float fill = max(dot(n, normalize(vec3(-uLightDir.x, -uLightDir.y, 0.4))), 0.0) * 0.18;
    vec3 h = normalize(uLightDir + v);
    float specular = pow(max(dot(n, h), 0.0), 56.0) * 0.28;
    float rim = pow(1.0 - max(dot(n, v), 0.0), 3.0) * 0.08;
    vec3 color = base * (0.30 + 0.62 * diffuse + fill) + vec3(specular + rim);
    fragColor = vec4(color, uColor.a);
}
"""
}
