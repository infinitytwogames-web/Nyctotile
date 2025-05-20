package dev.merosssany.calculatorapp.core.ui;

import dev.merosssany.calculatorapp.core.*;
import dev.merosssany.calculatorapp.core.position.UIVector2Df;
import dev.merosssany.calculatorapp.core.position.Vector2D;
import dev.merosssany.calculatorapp.core.position.Vector2Dx2;
import dev.merosssany.calculatorapp.core.render.ShaderProgram;
import dev.merosssany.calculatorapp.core.render.Window;
import dev.merosssany.calculatorapp.core.ui.font.FontRenderer;
import dev.merosssany.calculatorapp.core.ui.font.FontRendererGL;
import org.joml.Matrix4f;

import java.io.IOException;

import static dev.merosssany.calculatorapp.core.render.ShaderProgram.load;
import static org.lwjgl.opengl.GL11.*;

public class Button extends InteractableUI {
    private RGBA textColor;
    private ShaderProgram shader;
    private boolean isPressed = false;
    private Runnable mouseClick;
    private String text;
    private final float padding;
    private FontRendererGL fontRenderer;
    private float scaleDownFactor = 0.5f;
    private FontRenderer s;

    public Button(String text, float scale, RGBA color, UIVector2Df position, float width, float height, float padding, RGBA background, Window window) throws IOException {
        super(position, width, height, background, window);
        this.padding = padding;
        this.text = text;
        this.textColor = color;
        scaleDownFactor = scale;
        initBtn();
    }

    public Button(String text, float scale, RGBA color, Runnable onMouseRightClick, UIVector2Df position, float width, float height, float padding, RGBA background, Window window) throws IOException {
        super(position, width, height, background, window);
        this.padding = padding;
        this.mouseClick = onMouseRightClick;
        this.text = text;
        this.textColor = color;
        scaleDownFactor = scale;
        initBtn();
    }

    public RGBA getTextColor() {
        return textColor;
    }

    public void setTextColor(RGBA textColor) {
        this.textColor = textColor;
    }

    public Button(String text, RGBA color, UIVector2Df position, float width, float height, RGBA background, Window window, ButtonSettings settings) throws IOException {
        super(position, width, height, background, window);
        this.padding = settings.padding;
        this.text = text;
        this.shader = settings.program;
        this.mouseClick = settings.onClick;
        textColor = color;
        initBtn();
    }

    private void initBtn() throws IOException {
        s = new FontRenderer("src/main/resources/fonts/Main.ttf",32);
    }

    public boolean isPressed() {
        return isPressed;
    }

    @Override
    public void onMouseRightClick() {
        isPressed = true;
        if (mouseClick != null) mouseClick.run();
    }

    @Override
    public void draw(Matrix4f projMatrix) {
        super.draw(projMatrix);
        s.renderText(projMatrix,text,getPosition().getX(),getPosition().getY(),textColor.getRed(),textColor.getGreen(),textColor.getBlue());
    }


    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public float getPadding() {
        return padding;
    }

    private Vector2Dx2<Float> calculatePadding() {
        Vector2D<Float> position = this.getPosition(); // This is in NDC
        Vector2D<Float> x = new Vector2D<>(
                position.getX() + padding, position.getY() - padding
        );
        Vector2D<Float> y = new Vector2D<>(
                super.getEnd().getX() - padding, super.getEnd().getY() + padding
        );

        return new Vector2Dx2<>(x, y); // This is in NDC
    }

    @Override
    public void cleanup() {
        super.cleanup();
        fontRenderer.cleanup();
        s.cleanup();
    }
}
