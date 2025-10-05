package org.infinitytwo.umbralore.data;

import org.infinitytwo.umbralore.RGB;

public class TextComponent {
    private final String text;
    private final RGB color;

    public TextComponent(String text, RGB color) {
        this.text = text;
        this.color = color;
    }

    public String getText() {
        return text;
    }

    public RGB getColor() {
        return color;
    }

    @Override
    public String toString() {
        return text;
    }
}
