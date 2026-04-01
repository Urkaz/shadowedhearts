#version 150

// === Fullscreen quad: Position is in NDC (-1..1) ===
in vec3 Position;

// === Inverse matrices for ray reconstruction ===
uniform mat4 uInvProj;      // inverse projection matrix
uniform mat4 uInvView;      // inverse view matrix (view -> camera-relative world-aligned)

// === Varyings to fragment ===
out vec3 vRayDirWS;          // camera-relative world-aligned ray direction

void main() {
    gl_Position = vec4(Position.xy, 0.0, 1.0);

    // Reconstruct camera-relative world-aligned ray direction from NDC.
    // First unproject from clip to view space, then rotate to world-aligned space.
    // This matches how fog_cylinder gets world-space ray directions from proxy geometry.
    vec4 clipPos = vec4(Position.xy, 1.0, 1.0);
    vec4 viewPos = uInvProj * clipPos;
    viewPos.xyz /= viewPos.w;

    // Rotate from view space to world-aligned space (direction only, no translation)
    vRayDirWS = mat3(uInvView) * viewPos.xyz;
}
