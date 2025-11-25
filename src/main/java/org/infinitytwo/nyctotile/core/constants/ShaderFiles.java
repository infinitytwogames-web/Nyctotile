package org.infinitytwo.nyctotile.core.constants;

public abstract class ShaderFiles {
    public static final String uiVertex = """
#version 330 core

layout (location = 0) in vec3 aPos;      // Position (x, y, drawOrder)
layout (location = 1) in vec4 aColor;    // Color (r, g, b, a)
layout (location = 2) in vec2 aTexCoord; // Texture coordinates (u, v)

// --- Uniforms ---
uniform mat4 projection; // Projection matrix: maps scaled virtual space to screen (NDC)

// --- Output Varyings ---
out vec4 vColor;
out vec2 vTexCoord;

void main() {
    // gl_Position is directly calculated using the pre-transformed aPos and the projection matrix.
    // The z-component is 0.0, and w-component is 1.0 for a 2D position.
    gl_Position = projection * vec4(aPos, 1.0);

    vColor = aColor;
    vTexCoord = aTexCoord;
}
""";

    public static final String uiFragment = """
                #version 330 core // Specify GLSL version 3.30

                // --- Input Varyings (interpolated from the vertex shader) ---
                in vec4 vColor;      // Interpolated color from the vertex shader
                in vec2 vTexCoord;   // Interpolated texture coordinates from the vertex shader

                // --- Uniforms (data passed from your Java code) ---
                uniform sampler2D u_texture;  // The texture sampler (for images, text glyphs)
                uniform bool useTexture;    // Flag to determine if texturing should be used

                // --- Output Fragment Color ---
                out vec4 FragColor; // The final color of the fragment

                void main() {
                    vec4 finalColor;

                    if (useTexture == true) {
                          finalColor = texture(u_texture, vTexCoord);
                    } else { finalColor = vColor; }

                    // You generally want blending enabled for UI (e.g., glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA))
                    // to handle transparency correctly.
                    FragColor = finalColor;
                }
                """;

    public static final String textVertex = """
            #version 330 core
            layout(location = 0) in vec2 inPos;
            layout(location = 1) in vec2 inUV;
            
            uniform mat4 uProj;
            uniform mat4 uModel;
            
            out vec2 vUV;
            
            void main() {
                gl_Position = uProj * uModel * vec4(inPos, 0, 1);
                vUV = inUV;
            }
            
            """;
    public static final String textFragment = """
            #version 330 core
            in vec2 vUV;
            uniform sampler2D uFontAtlas;
            uniform vec3 uTextColor;
            out vec4 FragColor;
            void main(){
                float alpha = texture(uFontAtlas, vUV).r;
                if (alpha < 0.1) {
                        discard;
                }
                FragColor = vec4(uTextColor, alpha);
            }
            """;
}
