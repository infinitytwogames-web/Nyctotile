package dev.merosssany.calculatorapp.core;

import dev.merosssany.calculatorapp.core.position.Vector2D;
import dev.merosssany.calculatorapp.core.render.Window;
import org.joml.Matrix4f;

public abstract class AdvancedMath {

    // Clamp
    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(value, max));
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(value, max));
    }

    public static long clamp(long value, long min, long max) {
        return Math.max(min, Math.min(value, max));
    }

    public static short clamp(short value, short min, short max) {
        return (short) Math.max(min, Math.min(value, max));
    }

    // Lerp (Linear Interpolation)
    public static float lerp(float a, float b, float t) {
        return a + t * (b - a);
    }

    public static double lerp(double a, double b, double t) {
        return a + t * (b - a);
    }

    // Inverse Lerp
    public static float inverseLerp(float a, float b, float value) {
        return (value - a) / (b - a);
    }

    public static double inverseLerp(double a, double b, double value) {
        return (value - a) / (b - a);
    }

    // Map (Remap a value from one range to another)
    public static float map(float value, float inMin, float inMax, float outMin, float outMax) {
        return outMin + ((value - inMin) * (outMax - outMin)) / (inMax - inMin);
    }

    public static double map(double value, double inMin, double inMax, double outMin, double outMax) {
        return outMin + ((value - inMin) * (outMax - outMin)) / (inMax - inMin);
    }

    // Normalize (value to 0..1 based on a range)
    public static float normalize(float value, float min, float max) {
        return clamp((value - min) / (max - min), 0f, 1f);
    }

    public static double normalize(double value, double min, double max) {
        return clamp((value - min) / (max - min), 0.0, 1.0);
    }

    // Snap to nearest step
    public static float snap(float value, float step) {
        return Math.round(value / step) * step;
    }

    public static double snap(double value, double step) {
        return Math.round(value / step) * step;
    }

    // Scale
    public static float scale(float a, float b) {
        return a * b;
    }

    public static double scale(double a, double b) {
        return a * b;
    }

    // Round to N decimal places
    public static float round(float value, int places) {
        float scale = (float) Math.pow(10, places);
        return Math.round(value * scale) / scale;
    }

    public static double round(double value, int places) {
        double scale = Math.pow(10, places);
        return Math.round(value * scale) / scale;
    }

    public static <T extends Number & Comparable<T>> boolean isVectorPointIncludedIn(Vector2D<T> topRight, Vector2D<T> point, Vector2D<T> bottomLeft) {
        float minX = topRight.getX().floatValue();
        float maxX = bottomLeft.getX().floatValue();
        float minY = bottomLeft.getY().floatValue(); // Bottom boundary (assuming UI y increases downwards or is already flipped)
        float maxY = topRight.getY().floatValue(); // Top boundary (assuming UI y increases downwards or is already flipped)

        float targetX = point.getX().floatValue();
        float targetY = point.getY().floatValue();

        return targetX >= minX && targetX <= maxX &&
                targetY <= maxY && targetY >= minY; // Adjusted Y comparisons
    }

    public static <T extends Number & Comparable<T>> boolean isVectorPointIncludedAround(Vector2D<T> topRight, Vector2D<T> point, Vector2D<T> bottomLeft) {
        T minX = (point.getX().compareTo(bottomLeft.getX()) < 0) ? point.getX() : bottomLeft.getX();
        T maxX = (point.getX().compareTo(bottomLeft.getX()) > 0) ? point.getX() : bottomLeft.getX();
        T minY = (point.getY().compareTo(bottomLeft.getY()) < 0) ? point.getY() : bottomLeft.getY();
        T maxY = (point.getY().compareTo(bottomLeft.getY()) > 0) ? point.getY() : bottomLeft.getY();

        return topRight.getX().compareTo(minX) > 0 && topRight.getX().compareTo(maxX) < 0 &&
                topRight.getY().compareTo(minY) > 0 && topRight.getY().compareTo(maxY) < 0;
    }

    /**
     * Creates a 2D translation matrix.
     *
     * @param tx The translation distance along the x-axis.
     * @param ty The translation distance along the y-axis.
     * @return A 4x4 translation matrix.
     */
    public static Matrix4f translate2D(float tx, float ty) {
        Matrix4f matrix = new Matrix4f();
        matrix.identity();
        matrix.translate(tx, ty, 0);
        return matrix;
    }

    /**
     * Creates a 2D scaling matrix.
     *
     * @param sx The scaling factor along the x-axis.
     * @param sy The scaling factor along the y-axis.
     * @return A 4x4 scaling matrix.
     */
    public static Matrix4f scale2D(float sx, float sy) {
        Matrix4f matrix = new Matrix4f();
        matrix.identity();
        matrix.scale(sx, sy, 1);
        return matrix;
    }

    /**
     * Creates a 2D rotation matrix (around the Z-axis).
     *
     * @param angle The rotation angle in radians.
     * @return A 4x4 rotation matrix.
     */
    public static Matrix4f rotate2D(float angle) {
        Matrix4f matrix = new Matrix4f();
        matrix.identity();
        matrix.rotate(angle, 0, 0, 1);
        return matrix;
    }

    /**
     * Creates a 2D orthographic projection matrix.
     *
     * @param left The left edge of the projection.
     * @param right The right edge of the projection.
     * @param bottom The bottom edge of the projection.
     * @param top The top edge of the projection.
     * @return A 4x4 orthographic projection matrix.
     */
    public static Matrix4f orthographicProjection2D(float left, float right, float bottom, float top) {
        Matrix4f matrix = new Matrix4f();
        matrix.identity();
        matrix.setOrtho(left, right, bottom, top, -1, 1); // zNear = -1, zFar = 1
        return matrix;
    }

    /**
     * Calculates the combined model matrix from translation, scaling, and rotation.
     *
     * @param translationMatrix The translation matrix.
     * @param scalingMatrix     The scaling matrix.
     * @param rotationMatrix      The rotation matrix.
     * @return A 4x4 model matrix representing the combined transformation (Scale -> Rotate -> Translate).
     */
    public static Matrix4f calculateModelMatrix(Matrix4f translationMatrix, Matrix4f scalingMatrix, Matrix4f rotationMatrix) {
        Matrix4f modelMatrix = new Matrix4f();
        modelMatrix.identity();
        modelMatrix.mul(scalingMatrix);
        modelMatrix.mul(rotationMatrix);
        modelMatrix.mul(translationMatrix);
        return modelMatrix;
    }

    public static Vector2D<Integer> ndcToPixel(float ndcX, float ndcY, Window window) {
        int windowWidth = window.getWidth();
        int windowHeight = window.getHeight();

        // NDC to Normalized (0 to 1)
        float normalizedX = (ndcX + 1.0f) / 2.0f;
        float normalizedY = (1.0f - ndcY) / 2.0f; // <--- THIS LINE IS CRUCIAL FOR Y-INVERSION

        // Normalized (0 to 1) to Pixel
        int pixelX = (int) (normalizedX * windowWidth);
        int pixelY = (int) (normalizedY * windowHeight);

        return new Vector2D<>(pixelX, pixelY);
    }
}
