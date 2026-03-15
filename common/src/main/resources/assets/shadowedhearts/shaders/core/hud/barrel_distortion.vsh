#version 150

in vec3 Position;
in vec4 Color;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

// custom
uniform vec2 Center;      // in the same coordinate space as Position.xy after ModelView
uniform float Curvature;  // strength
uniform float Angle;      // radians
uniform float Radius;

out vec4 vertexColor;

void main() {
    vec4 mv = ModelViewMat * vec4(Position, 1.0);

    vec2 p = mv.xy - Center;

    // rotation
    float c = cos(Angle);
    float s = sin(Angle);
    p = vec2(c * p.x - s * p.y, s * p.x + c * p.y);

    // Cylindrical wrap around the vertical axis through Center.
    // Curvature is radians-per-unit in this coordinate space.
    float k = Curvature;
    if (abs(k) > 1e-6) {
        float theta = p.x * k;
        float r = Radius / abs(k);

        // Wrap x along an arc of a cylinder of radius r.
        p.x = sin(theta) * r;

        // Use the resulting z offset as depth.
        float zOff = (cos(theta)) * r;
        mv.z += zOff;

        // With an ortho GUI projection, z won't create perspective by itself,
        // so approximate it by scaling based on cylinder depth.
        float persp = r / (r + zOff);
        p *= persp;
    }

    mv.xy = p + Center;
    gl_Position = ProjMat * mv;

    vertexColor = Color;
}