package dev.merosssany.calculatorapp.core;

import dev.merosssany.calculatorapp.core.position.Vector2D;

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
}
