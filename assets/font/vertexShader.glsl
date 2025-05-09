#version 330 core

// Input vertex attributes
layout (location = 0) in vec2 aPos;      // Vertex position (screen coordinates)
layout (location = 1) in vec2 aTexCoord; // Texture coordinates

// Uniform variables
uniform mat4 uProjection; // Projection matrix (orthographic for 2D)

// Output vertex attributes (passed to fragment shader)
out vec2 vTexCoord;

void main() {
    // Transform vertex position to clip space
    gl_Position = uProjection * vec4(aPos, 0.0, 1.0);
    vTexCoord = aTexCoord; // Pass texture coordinates to fragment shader
}
