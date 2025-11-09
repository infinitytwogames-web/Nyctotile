package org.infinitytwo.umbralore.core.ui;

import org.infinitytwo.umbralore.core.RGB;
import org.infinitytwo.umbralore.core.event.input.MouseButtonEvent;
import org.infinitytwo.umbralore.core.event.input.MouseHoverEvent;
import org.infinitytwo.umbralore.core.renderer.FontRenderer;
import org.infinitytwo.umbralore.core.renderer.UIBatchRenderer;
import org.infinitytwo.umbralore.core.ui.component.Text;
import org.infinitytwo.umbralore.core.ui.display.Screen;
import org.infinitytwo.umbralore.core.ui.position.Anchor;
import org.infinitytwo.umbralore.core.ui.position.Pivot;
import org.infinitytwo.umbralore.core.ui.builder.UIBuilder;
import org.infinitytwo.umbralore.core.ui.position.TruncateMode;
import org.joml.Vector2i;

import java.nio.file.Path;

public class Label extends UI {
    protected FontRenderer textRenderer;
    protected Text text;
    protected String ellipsis = "...";
    protected final Path path;
    
    public Label(Screen renderer, Path path) {
        super(renderer.getUIBatchRenderer());
        this.textRenderer = new FontRenderer(path.toString(),16);
        text = new Text(textRenderer, renderer);
        this.path = path;
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
    
    @Override
    public void setHeight(int height) {
        super.setHeight(height);
        textRenderer = new FontRenderer(path.toAbsolutePath().toString(), (float) height /2);
        text.setRenderer(textRenderer);
    }
    
    @Override
    public void setBackgroundColor(float r, float g, float b, float a) {
        super.setBackgroundColor(r, g, b, a);
        text.setColor(new RGB(1,1,1));
    }
    
    @Override
    public void onMouseClicked(MouseButtonEvent e) {

    }

    @Override
    public void onMouseHover(MouseHoverEvent e) {

    }

    @Override
    public void onMouseHoverEnded() {

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
                int ellipsisWidth = (int) renderer.getStringWidth(ellipsis);
                int remainingWidth = maxWidth - ellipsisWidth;

                // Find how many characters can fit on both sides combined
                int totalChars = 0;
                while (totalChars < fullText.length() && renderer.getStringWidth(fullText.substring(0, totalChars + 1)) <= remainingWidth) {
                    totalChars++;
                }

                // Split the available characters evenly
                int startChars = totalChars / 2;
                int endChars = totalChars - startChars;

                String start = fullText.substring(0, startChars);
                String end = fullText.substring(fullText.length() - endChars);

                return start + ellipsis + end;
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
    
    public void setTextPosition(Anchor anchor, Pivot pivot) {
        text.setPosition(anchor,pivot);
    }
    
    public static class LabelBuilder<T extends Label> extends UIBuilder<T> {
        public LabelBuilder(UIBatchRenderer renderer, T element) {
            super(element);
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
