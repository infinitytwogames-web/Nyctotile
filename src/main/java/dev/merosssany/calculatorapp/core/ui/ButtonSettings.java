package dev.merosssany.calculatorapp.core.ui;

import dev.merosssany.calculatorapp.core.render.ShaderProgram;

import java.io.IOException;

public class ButtonSettings {
    public Runnable onClick;
    public Runnable onHover;
    public float padding;
    public String fontFilePath;
    public ShaderProgram program;

    public ButtonSettings(Runnable onClick, Runnable onHover, float padding, String fontFilePath, ShaderProgram program) {
        this.onClick = onClick;
        this.onHover = onHover;
        this.padding = padding;
        this.fontFilePath = fontFilePath;
        this.program = program;
    }

    public ButtonSettings(Runnable onClick, Runnable onHover, float padding, String fontFilePath, String vertexShaderPath, String fragmentShaderPath) throws IOException {
        this.onClick = onClick;
        this.onHover = onHover;
        this.padding = padding;
        this.fontFilePath = fontFilePath;
        this.program = new ShaderProgram(ShaderProgram.load(vertexShaderPath),ShaderProgram.load(fragmentShaderPath));
    }
}
