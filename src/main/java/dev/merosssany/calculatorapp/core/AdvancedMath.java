package dev.merosssany.calculatorapp.core;

import dev.merosssany.calculatorapp.core.position.Vector2D;

public abstract class AdvancedMath {
    public static int clamp(int value,int min, int max) {
        if (value < min) return min;
        return Math.min(value, max);
    }

    public static float clamp(float value, float min, float max) {
        if (value < min) return min;
        return Math.min(value, max);
    }

    public static double clamp(double value, double min, double max) {
        if (value < min) return min;
        return Math.min(value, max);
    }

    public static long clamp(long value, long min, long max) {
        if (value < min) return min;
        return Math.min(value, max);
    }

    public static short clamp(short value, short min, short max) {
        if (value < min) return min;
        return (short) Math.min(value,max);
    }

    public static float scale(float increment, float other) {
        return  increment * other;
    }

    public <T extends Number & Comparable<T>> boolean isVectorPointIncludedIn(Vector2D<T> topRight, Vector2D<T> point, Vector2D<T> bottomLeft) {
        float minX = topRight.getX().floatValue();
        float maxX = bottomLeft.getX().floatValue();
        float minY = bottomLeft.getY().floatValue(); // Bottom boundary (assuming UI y increases downwards or is already flipped)
        float maxY = topRight.getY().floatValue(); // Top boundary (assuming UI y increases downwards or is already flipped)

        float targetX = point.getX().floatValue();
        float targetY = point.getY().floatValue();

        return targetX >= minX && targetX <= maxX &&
                targetY <= maxY && targetY >= minY; // Adjusted Y comparisons
    }

    public <T extends Number & Comparable<T>> boolean isVectorPointIncludedAround(Vector2D<T> topRight, Vector2D<T> point, Vector2D<T> bottomLeft) {
        T minX = (point.getX().compareTo(bottomLeft.getX()) < 0) ? point.getX() : bottomLeft.getX();
        T maxX = (point.getX().compareTo(bottomLeft.getX()) > 0) ? point.getX() : bottomLeft.getX();
        T minY = (point.getY().compareTo(bottomLeft.getY()) < 0) ? point.getY() : bottomLeft.getY();
        T maxY = (point.getY().compareTo(bottomLeft.getY()) > 0) ? point.getY() : bottomLeft.getY();

        return topRight.getX().compareTo(minX) > 0 && topRight.getX().compareTo(maxX) < 0 &&
                topRight.getY().compareTo(minY) > 0 && topRight.getY().compareTo(maxY) < 0;
    }
}
