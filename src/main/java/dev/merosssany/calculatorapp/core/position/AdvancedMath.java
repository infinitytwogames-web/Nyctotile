package dev.merosssany.calculatorapp.core.position;

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
}
