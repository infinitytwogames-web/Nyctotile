#version 330 core

// Input from vertex shader
in vec2 vTexCoord;

// Uniform variables
uniform sampler2D uFontTexture; // The font texture atlas
uniform vec4 uTextColor;    // The color of the text

// Output
out vec4 fragColor;

void main() {
    // Sample the font texture
    vec4 texColor = texture(uFontTexture, vTexCoord);

    // Apply text color and alpha
    fragColor = uTextColor * texColor.a;
}