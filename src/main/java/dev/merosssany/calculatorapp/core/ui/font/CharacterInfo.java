package dev.merosssany.calculatorapp.core.ui.font;

import org.joml.Vector2f;

public class CharacterInfo {
    protected int sourceX, sourceY, width, height, code;
    private final Vector2f[] positions = new Vector2f[4];

    public CharacterInfo(int sourceX, int sourceY, int width, int height) {
        this.sourceX = sourceX;
        this.sourceY = sourceY;
        this.width = width;
        this.height = height;
        this.code = code;
    }

    public void calculatePosition(int fontWidth, int fontHeight) {
        float x0 = (float)sourceX / (float)fontWidth;
        float x1 = (float)(sourceX + width) / (float)fontWidth;
        float y0 = (float)(sourceY - height) / (float)fontHeight;
        float y1 = (float)(sourceY) / (float)fontHeight;

        positions[0] = new Vector2f(x0, y1);
        positions[1] = new Vector2f(x1, y0);
    }

    public Vector2f[] getPositions() {
        return positions;
    }
}
