package org.infinitytwo.nyctotile.core.constants;

import org.infinitytwo.nyctotile.core.Window;
import org.joml.Matrix4f;

public final class Constants {
    public static final String fontFilePath = "src/main/resources/assets/fonts/Main.ttf";
//    public static final Matrix4f uiProjectionMatrix = AdvancedMath.createVirtualProjection(1920,1080);
    public static final float UI_DESIGN_HEIGHT = 1080.0f;

    public static Matrix4f getTextProjectionMatrix(Window window) {
         return new Matrix4f().ortho2D(0, window.getWidth(), window.getHeight(), 0);
    }

    public static Matrix4f getTextProjectionMatrix(int width, int height) {
        return new Matrix4f().ortho2D(0, width, height, 0);
    }
}
