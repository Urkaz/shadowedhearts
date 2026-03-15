#version 150

uniform sampler2D DiffuseSampler;   // main color buffer
uniform sampler2D uDepth;           // optional depth buffer
uniform float GameTime;             // seconds
uniform float uHeat;                // 0..1
uniform vec2 ScreenSize;            // screen size
uniform vec2 uScanCenter;           // 0..1 center for radial mask (optional)
uniform float uUseDepth;            // 0 = ignore depth, 1 = use depth

in vec2 texCoord0;
out vec4 fragColor;

float radialMask(vec2 uv, vec2 center) {
    float d = distance(uv, center);
    // mask is 1 near center, 0 far away
    return 1.0 - smoothstep(0.15, 0.55, d);
}

// Tiny hash noise (fast, no textures)
float hash21(vec2 p) {
    p = fract(p * vec2(123.34, 345.45));
    p += dot(p, p + 34.345);
    return fract(p.x * p.y);
}

// Smooth value noise
float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    float a = hash21(i);
    float b = hash21(i + vec2(1,0));
    float c = hash21(i + vec2(0,1));
    float d = hash21(i + vec2(1,1));
    vec2 u = f*f*(3.0-2.0*f);
    return mix(mix(a,b,u.x), mix(c,d,u.x), u.y);
}

// FBM for richer shimmer
float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 4; i++) {
        v += a * noise(p);
        p *= 2.02;
        a *= 0.5;
    }
    return v;
}

vec2 heatDistort(vec2 uv, float t, float heat) {
    // mostly-vertical waves + drifting noise
    float bands = sin((uv.y * 60.0) + t * (2.5 + 6.0*heat)) * 0.5 + 0.5;

    vec2 p = uv * vec2(8.0, 18.0);
    p += vec2(0.0, t * (0.8 + 2.2*heat)); // upward drift

    float n1 = fbm(p);
    float n2 = fbm(p + vec2(5.2, 1.3));

    // Combine into offset direction: stronger horizontal wobble, slight vertical jitter
    float x = (n1 - 0.5) * (0.6 + 1.4*bands);
    float y = (n2 - 0.5) * (0.15 + 0.35*bands);

    // Strength in UV units (will scale later)
    return vec2(x, y);
}

void main() {
    // Use provided texture coordinates for fullscreen quad
    vec2 uv = texCoord0;

    float heat = clamp(uHeat, 0.0, 1.0);

    // Nonlinear ramp: nothing -> sudden “overheat shimmer”
    float strength = smoothstep(0.45, 0.95, heat);
    strength *= strength; // sharpen the onset

    // Optional radial mask around scanner center
    float mask = 1.0;
    //radialMask(uv, uScanCenter);

    // Optional depth scaling (if you have depth)
    float depthFactor = 1.0;
    if (uUseDepth > 0.5) {
        float d = texture(uDepth, uv).r;
        depthFactor = smoothstep(0.2, 0.95, d); // more shimmer far away
    }

    // Final distortion amount in pixels -> convert to UV
    float px = mix(0.0, 6.0, strength) * mask * depthFactor; // up to ~6px
    vec2 offset = heatDistort(uv, GameTime, heat) * (px / ScreenSize);

    // Two-tap sampling reduces “tearing” artifacts
    vec3 c0 = texture(DiffuseSampler, uv + offset).rgb;
    vec3 c1 = texture(DiffuseSampler, uv + offset * 0.65).rgb;
    vec3 col = mix(c0, c1, 0.4);

    vec3 heatTint = mix(vec3(1.0), vec3(1.2, 0.85, 0.6), heat);
    col *= heatTint;

    float smear = heat * 3.0 / ScreenSize.y;
    col = mix(col,
    texture(DiffuseSampler, uv - vec2(0, smear)).rgb,
    heat * 0.4);

    // Optional micro-shimmer at extreme heat
    float micro = smoothstep(0.85, 1.0, heat);
    float flick = (noise(uv * ScreenSize * 0.15 + GameTime * 18.0) - 0.5) * micro * 0.03;
    col += flick;

    // Output the refracted scene color directly (no additive blending)
    fragColor = vec4(col, 1.0);
}