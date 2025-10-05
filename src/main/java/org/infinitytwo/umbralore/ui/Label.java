package org.infinitytwo.umbralore.ui;

import org.infinitytwo.umbralore.RGB;
import org.infinitytwo.umbralore.renderer.FontRenderer;
import org.infinitytwo.umbralore.renderer.UIBatchRenderer;
import org.infinitytwo.umbralore.ui.component.Text;
import org.infinitytwo.umbralore.ui.position.Anchor;
import org.infinitytwo.umbralore.ui.position.Pivot;
import org.infinitytwo.umbralore.ui.builder.UIBuilder;
import org.infinitytwo.umbralore.ui.position.TruncateMode;
import org.joml.Vector2i;

public abstract class Label extends UI {
    protected FontRenderer textRenderer;
    protected Text text;
    protected String ellipsis = "...";

    public Label(Screen renderer, FontRenderer textRenderer, RGB color) {
        super(renderer.getUIBatchRenderer());
        this.textRenderer = textRenderer;
        text = new Text(textRenderer, renderer);
        text.setColor(color);
        text.setParent(this);
    }

    public void setTextPosition(Anchor anchor, Pivot pivot, Vector2i offset) {
        text.setPosition(anchor, pivot, offset);
    }

    @Override
    public void draw() {
        super.draw();
        text.draw();
    }

    public String getText() {
        return text.getText();
    }

    public void setText(String text) {
        this.text.setText(text);
    }

    public RGB getColor() {
        return text.getColor();
    }

    public void setColor(RGB color) {
        this.text.setColor(color);
    }

    public Vector2i getTextPosition() {
        return text.getPosition();
    }

    public Text getTextComponent() {
        return text;
    }

    @Override
    public void cleanup() {
        // only cleanup component state, not shared renderer
        text = null;
    }

    public String getVisibleText(FontRenderer renderer, String fullText, int maxWidth, TruncateMode mode) {
        if (renderer.getStringWidth(fullText) <= maxWidth) return fullText;

        switch (mode) {
            case END: {
                int cut = fullText.length();
                while (cut > 0 && renderer.getStringWidth(fullText.substring(0, cut) + ellipsis) > maxWidth) {
                    cut--;
                }
                return fullText.substring(0, cut) + ellipsis;
            }
            case MIDDLE: {
                return getVisibleText(renderer, fullText, fullText.length() / 2, maxWidth); // reuse your caret-based logic
            }
            default:
                return fullText;
        }
    }

    public String getVisibleText(FontRenderer renderer, String fullText, int caretIndex, int maxWidth) {
        int ellipsisWidth = (int) renderer.getStringWidth(ellipsis);
        if ((int) renderer.getStringWidth(fullText) <= maxWidth) return fullText;
        int left = caretIndex;
        int right = caretIndex;

        while (true) {
            int addLeft = (left > 0) ? 1 : 0;
            int addRight = (right < fullText.length()) ? 1 : 0;

            int newLeft = left - addLeft;
            int newRight = right + addRight;

            String candidate = fullText.substring(newLeft, newRight);
            boolean needsLeftEllipsis = newLeft > 0;
            boolean needsRightEllipsis = newRight < fullText.length();

            String withEllipsis = (needsLeftEllipsis ? ellipsis : "") + candidate + (needsRightEllipsis ? ellipsis : "");
            int candidateWidth = (int) renderer.getStringWidth(withEllipsis);

            if (candidateWidth > maxWidth) break;

            left = newLeft;
            right = newRight; // Stop if both sides reached the limits
            if (addLeft == 0 && addRight == 0) break;
        }

        String finalText = fullText.substring(left, right);
        if (left > 0) finalText = ellipsis + finalText;
        if (right < fullText.length()) finalText = finalText + ellipsis;

        return finalText;
    }

    public static class LabelBuilder<T extends Label> extends UIBuilder<T> {
        public LabelBuilder(UIBatchRenderer renderer, T element) {
            super(renderer, element);
        }

        public UIBuilder<T> textPosition(Anchor anchor, Pivot pivot, Vector2i offset) {
            ui.setTextPosition(anchor, pivot, offset);
            return this;
        }

        public UIBuilder<T> text(String text) {
            ui.setText(text);
            return this;
        }

        @Override
        public UIBuilder<T> applyDefault() {
            return this;
        }
    }
}
