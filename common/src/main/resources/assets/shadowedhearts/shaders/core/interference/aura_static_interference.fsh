#version 150

// Screen-space electromagnetic static overlay for Aura Scanner
// Inputs provided by Minecraft POSITION_TEX vertex program
uniform vec2 ScreenSize;
uniform float GameTime; // in days, fractional
in vec2 texCoord0;
out vec4 fragColor;

// Custom uniform driven by HUD logic (0..1)
uniform float uIntensity;

// hash noise
float hash(vec2 p)
{
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

// Pseudo-random with time for flicker
float noiseHF(vec2 uv, float t) {
    // Quantize to an 16x16 pixel grid for chunkier, TV-like static
    // Each noise sample represents a block of 16x16 real pixels
    const float CELL = 16.0;
    vec2 scaled = floor((uv * ScreenSize) / CELL);
    return hash(scaled + t * 50.0);
}

// Horizontal glitch bands
float glitchBand(vec2 uv, float t) {
    float band = sin(uv.y * 300.0 + t * 40.0);
    return smoothstep(0.8, 1.0, band);
}

// Subtle global flicker
float flicker(float t) {
    return 0.9 + 0.1 * sin(t * 20.0);
}

void main() {
    // Convert GameTime to seconds-ish range
    float t = fract(GameTime) * 1200.0;

    vec2 uv = texCoord0; // 0..1

    // Add noise to the scrolling direction: predominantly vertical drift
    // with slight, cell-quantized horizontal wobble to avoid shimmering.
    const float DIR_CELL = 64.0;                    // direction field grid size in pixels
    vec2 dirCell = floor((uv * ScreenSize) / DIR_CELL);
    float nDir = hash(dirCell + t * 0.1);           // stable per-cell noise, slowly evolving
    vec2 scrollDir = normalize(vec2(0.35 * (nDir - 0.5), 1.0)); // mostly up with small x component
    vec2 uvScroll = uv + scrollDir * (t * 0.0008);  // subtle drift along noisy direction

    float g = noiseHF(uvScroll, t);
    float band = glitchBand(uvScroll, t);

    // Electromagnetic feel: blend structured banding into grain
    float staticNoise = g;
    //mix(g, band, 0.4);

    float intensity = clamp(uIntensity, 0.0, 1.0);

    // Purple spectral tint for ghost vibe
    vec3 ghostTint = vec3(0.40, 0.00, 0.60);

    // Brightness modulation
    float f = flicker(t);
    float staticOverlay = staticNoise * intensity * f;

    // Start from transparent, add tinted static
    vec3 col = mix(vec3(0.0), ghostTint, intensity * 0.15);
    col += staticOverlay * 0.25;

    fragColor = vec4(col, intensity); // alpha gates composite on UI layer
}
