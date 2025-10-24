package org.infinitytwo.umbralore.core.ui.input;

import org.infinitytwo.umbralore.core.RGB;
import org.infinitytwo.umbralore.core.event.bus.EventBus;
import org.infinitytwo.umbralore.core.event.SubscribeEvent;
import org.infinitytwo.umbralore.core.event.input.CharacterInputEvent;
import org.infinitytwo.umbralore.core.event.input.KeyPressEvent;
import org.infinitytwo.umbralore.core.event.input.MouseButtonEvent;
import org.infinitytwo.umbralore.core.event.input.MouseHoverEvent;
import org.infinitytwo.umbralore.core.renderer.FontRenderer;
import org.infinitytwo.umbralore.core.ui.Caret;
import org.infinitytwo.umbralore.core.ui.Label;
import org.infinitytwo.umbralore.core.ui.display.Screen;
import org.infinitytwo.umbralore.core.ui.position.Anchor;
import org.infinitytwo.umbralore.core.ui.position.Pivot;
import org.joml.Vector2i;

import static org.infinitytwo.umbralore.core.VectorMath.isPointWithinRectangle;
import static org.infinitytwo.umbralore.core.Display.transformWindowToVirtual;
import static org.joml.Math.clamp;
import static org.lwjgl.glfw.GLFW.*;

public abstract class TextInput extends Label {
    private int index = 0;
    private final Caret caret;
    private boolean input;
    private final StringBuilder builder = new StringBuilder();
    private boolean submitted;
    private boolean disabled;
    private final Screen screen;

    public TextInput(FontRenderer renderer1, Screen screen, RGB color) {
        super(screen, renderer1, color);
        this.screen = screen;

        caret = new Caret(screen.getUIBatchRenderer(), (int) this.textRenderer.getFontHeight() + 3);
        caret.setActive(false);
        screen.register(caret);

        setTextPosition(new Anchor(0,0.5f),new Pivot(0,0.5f),new Vector2i(5,0));

        EventBus.register(this);
    }

    @Override
    public void onMouseClicked(MouseButtonEvent e) {
        if (submitted) {
            setText("");
            submitted = false;
        }

        index = getCaretIndexAtMouse(transformWindowToVirtual(screen.getWindow(),e.x,e.y).x,getPosition().x+5);
        focus();
    }

    @Override
    public void onMouseHover(MouseHoverEvent e) {

    }

    @Override
    public void onMouseHoverEnded() {

    }

    @Override
    public void setPosition(Anchor anchor, Pivot pivot) {
        super.setPosition(anchor, pivot);
        caret.setPosition(anchor,pivot,new Vector2i(-(width /2) +5, 0));
    }

    @Override
    public void setPosition(Anchor anchor, Pivot pivot, Vector2i offset) {
        super.setPosition(anchor, pivot, offset);
        caret.setPosition(anchor,pivot,new Vector2i(-(width /2) +5, 0));
    }

    @SubscribeEvent
    public void onKeyPress(KeyPressEvent e) {
        if (e.getAction() == GLFW_PRESS ||
            e.getAction() == GLFW_REPEAT
        ) {
            if (e.getKey() == GLFW_KEY_ESCAPE) unfocus();
            else if (e.getKey() == GLFW_KEY_RIGHT) index = clamp(index+1,0,builder.length());
            else if (e.getKey() == GLFW_KEY_LEFT) index = clamp(index -1, 0, builder.length());
            else if (e.getKey() == GLFW_KEY_BACKSPACE) backspace();
            else if (e.getKey() == GLFW_KEY_HOME) index = 0;
            else if (e.getKey() == GLFW_KEY_END) index = builder.length();
            else if (e.mods == GLFW_MOD_CONTROL) {
                if (e.getKey() == GLFW_KEY_A) {

                }
            }
            else if (e.getKey() == GLFW_KEY_ENTER) {
                submitted = true;
                submit(builder.toString());
                clean();
            }
            updateCursorPosition();
        }
    }

    private void unfocus() {
        input = false;
        caret.setActive(false);
        String visible = getVisibleText(textRenderer, builder.toString(), 0, width - 10);
        setText(visible);
        updateCursorPosition();
    }

    private void focus() {
        if (disabled) {
            unfocus();
            return;
        }
        input = true;
        caret.setActive(true);
        String visible = getVisibleText(textRenderer, builder.toString(), index, width - 10);
        text.setText(visible);
        updateCursorPosition();
    }

    public int getCaretIndexAtMouse(float mouseX, float textX) {
        float x = textX;

        for (int i = 0; i < text.getText().length(); i++) {
            char c = text.getText().charAt(i);
            float charWidth = textRenderer.getStringWidth(String.valueOf(c));

            // If mouseX is within this character
            if (mouseX < x + charWidth / 2f) {
                return i;
            }

            x += charWidth;
        }

        return text.getText().length(); // clicked past the end
    }

    @SubscribeEvent
    public void onMouseClickedA(MouseButtonEvent e) {
        if (e.action != GLFW_RELEASE) return;
        Vector2i mousePosition = transformWindowToVirtual(e.window, e.x, e.y);
        if (!isPointWithinRectangle(getPosition(), mousePosition, getEnd())) {
            unfocus();
        }
    }

    @SubscribeEvent
    public void onCharacterPressed(CharacterInputEvent e) {
        if (input) {
            builder.insert(index, e.character); // insert at index
            index++;
            setText(builder.toString());
            updateCursorPosition(); // keep cursor synced
            caret.reset();
        }
    }

    public void updateCursorPosition() {
        String visible = getVisibleText(textRenderer, builder.toString(), index, width - 10);
        setText(visible);

        // Calculate cursor X based on visible characters before the caret
        int globalOffset = getVisibleTextStartIndex(builder.toString(), visible);
        int localIndex = clamp(index - globalOffset, 0, visible.length());

        String leftPart = visible.substring(0, localIndex);
        int cursorX = (int) textRenderer.getStringWidth(leftPart);

        caret.setOffset(new Vector2i(-(width /2) + cursorX + 5, 0));
    }

    private int getVisibleTextStartIndex(String full, String visible) {
        int fullLen = full.length();
        int visibleLen = visible.replace("...", "").length(); // remove ellipsis
        int start = 0;

        for (int i = 0; i <= fullLen - visibleLen; i++) {
            String candidate = full.substring(i, i + visibleLen);
            if (visible.contains(candidate)) {
                return i;
            }
        }

        return 0;
    }

    private void backspace() {
        if (index == 0) return;
        index = clamp(index-1, 0, builder.length());
        builder.deleteCharAt(index);
        setText(builder.toString());
        updateCursorPosition();
    }

    private void clean() {
        unfocus();
        builder.setLength(0);
    }

    public abstract void submit(String data);

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }
}
