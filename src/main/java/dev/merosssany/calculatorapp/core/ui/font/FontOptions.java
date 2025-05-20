package dev.merosssany.calculatorapp.core.ui.font;

import dev.merosssany.calculatorapp.core.render.ShaderProgram;

public class FontOptions {
    public float display;
    public int fontSize;
    public String fontPath;
    public ShaderProgram program;

    public FontOptions(float display, int fontSize, String fontPath, ShaderProgram program) {
        this.display = display;
        this.fontSize = fontSize;
        this.fontPath = fontPath;
        this.program = program;
    }

    public FontOptions(float display, int fontSize, String fontPath) {
        this.display = display;
        this.fontSize = fontSize;
        this.fontPath = fontPath;
    }

    public FontOptions(float display, int fontSize) {
        this.display = display;
        this.fontSize = fontSize;
    }
}
