package dev.merosssany.calculatorapp.core.render;

public abstract class ShaderFiles {
    public static final String uiVertex = """
            #version 330 core
            layout(location = 0) in vec2 aPos;
            uniform vec2 uPosition;
            uniform vec2 uSize;
            uniform mat4 uProj;
            void main() {
                vec2 scaled = aPos * uSize + uPosition;
                gl_Position = uProj * vec4(scaled, 0.0, 1.0);
            }
            """;

    public static final String uiFragment = """
            #version 330 core
            
            uniform vec4 uColor;  // RGBA background color
            
            out vec4 fragColor;
            
            void main() {
                fragColor = uColor;
            }""";
}
