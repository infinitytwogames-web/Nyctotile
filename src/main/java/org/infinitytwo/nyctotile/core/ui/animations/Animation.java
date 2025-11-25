package org.infinitytwo.nyctotile.core.ui.animations;

import org.joml.Vector2i;

import static java.lang.Math.pow;
import static org.joml.Math.*;

/**
 * Provides a collection of static methods for applying common easing functions
 * to two-dimensional {@code Vector2i} objects, facilitating complex UI and game animations.
 * * <p>All methods are overloaded: one version mutates a provided 'current' vector
 * for performance, and the other creates a new vector for convenience.</p>
 */
public class Animation {
    private static final float PI_F = (float) PI;
    private static final float BACK_S = 1.70158f;
    private static final float BACK_C3 = BACK_S + 1.0f; // For easeOutBack
    
    // --- Linear Easing ---
    
    /**
     * Calculates the current position using a **linear** interpolation curve (constant speed).
     * * @param start The starting position vector.
     * @param current The vector to be mutated with the new calculated position.
     * @param end The target final position vector.
     * @param delta The normalized time elapsed (typically 0.0 to 1.0).
     * @return The mutated {@code current} vector containing the new position.
     */
    public static Vector2i linear(Vector2i start, Vector2i current, Vector2i end, float delta) {
        return current.set((int) lerp(start.x, end.x, delta), (int) lerp(start.y, end.y, delta));
    }
    
    /**
     * Calculates the current position using a **linear** interpolation curve, returning a new vector.
     * * @param start The starting position vector.
     * @param end The target final position vector.
     * @param delta The normalized time elapsed (typically 0.0 to 1.0).
     * @return A new {@code Vector2i} containing the calculated position.
     */
    public static Vector2i linear(Vector2i start, Vector2i end, float delta) {
        return linear(start, new Vector2i(), end, delta);
    }
    
    // --- Sine Easing (easeInOut) ---
    
    /**
     * Calculates the current position using the **Sine easing** curve (ease-in/out).
     * The movement smoothly accelerates from the start and smoothly decelerates to the end.
     * * @param start The starting position vector.
     * @param current The vector to be mutated with the new calculated position.
     * @param end The target final position vector.
     * @param delta The normalized time elapsed (typically 0.0 to 1.0).
     * @return The mutated {@code current} vector containing the new position.
     */
    public static Vector2i sine(Vector2i start, Vector2i current, Vector2i end, float delta) {
        // -0.5 * (cos(PI * delta) - 1)
        float eased = -0.5f * (cos(PI_F * delta) - 1.0f);
        return current.set((int) lerp(start.x, end.x, eased), (int) lerp(start.y, end.y, eased));
    }
    
    /**
     * Calculates the current position using the **Sine easing** curve, returning a new vector.
     * * @param start The starting position vector.
     * @param end The target final position vector.
     * @param delta The normalized time elapsed (typically 0.0 to 1.0).
     * @return A new {@code Vector2i} containing the calculated position.
     */
    public static Vector2i sine(Vector2i start, Vector2i end, float delta) {
        return sine(start, new Vector2i(), end, delta);
    }
    
    // --- Back Easing (easeOut) ---
    
    /**
     * Calculates the current position using the **Back easing** curve (ease-out).
     * The movement slightly overshoots the target {@code end} position before settling back,
     * creating a springy, satisfying finish.
     * * @param start The starting position vector.
     * @param current The vector to be mutated with the new calculated position.
     * @param end The target final position vector.
     * @param delta The normalized time elapsed (typically 0.0 to 1.0).
     * @return The mutated {@code current} vector containing the new position.
     */
    public static Vector2i back(Vector2i start, Vector2i current, Vector2i end, float delta) {
        // Standard easeOutBack formula using common constants s and s+1
        float eased = (float) (1.0f + BACK_C3 * pow(delta - 1.0f, 3) + BACK_S * pow(delta - 1.0f, 2));
        return current.set((int) lerp(start.x, end.x, eased), (int) lerp(start.y, end.y, eased));
    }
    
    /**
     * Calculates the current position using the **Back easing** curve, returning a new vector.
     * * @param start The starting position vector.
     * @param end The target final position vector.
     * @param delta The normalized time elapsed (typically 0.0 to 1.0).
     * @return A new {@code Vector2i} containing the calculated position.
     */
    public static Vector2i back(Vector2i start, Vector2i end, float delta) {
        return back(start, new Vector2i(), end, delta);
    }
    
    // --- Bounce Easing (easeOut) ---
    
    /**
     * Calculates the current position using the **Bounce easing** curve (ease-out).
     * The object simulates bouncing as it approaches the final position.
     * * @param start The starting position vector.
     * @param current The vector to be mutated with the new calculated position.
     * @param end The target final position vector.
     * @param delta The normalized time elapsed (typically 0.0 to 1.0).
     * @return The mutated {@code current} vector containing the new position.
     */
    public static Vector2i bounce(Vector2i start, Vector2i current, Vector2i end, float delta) {
        float eased = bounceEaseOut(delta);
        return current.set((int) lerp(start.x, end.x, eased), (int) lerp(start.y, end.y, eased));
    }
    
    /**
     * Internal helper function to calculate the bounce ease-out factor.
     */
    private static float bounceEaseOut(float t) {
        // Constants defined with f for safety and readability
        if (t < 1.0f / 2.75f) {
            return 7.5625f * t * t;
        } else if (t < 2.0f / 2.75f) {
            t -= 1.5f / 2.75f;
            return 7.5625f * t * t + 0.75f;
        } else if (t < 2.5f / 2.75f) {
            t -= 2.25f / 2.75f;
            return 7.5625f * t * t + 0.9375f;
        } else {
            t -= 2.625f / 2.75f;
            return 7.5625f * t * t + 0.984375f;
        }
    }
    
    /**
     * Calculates the current position using the **Bounce easing** curve, returning a new vector.
     * * @param start The starting position vector.
     * @param end The target final position vector.
     * @param delta The normalized time elapsed (typically 0.0 to 1.0).
     * @return A new {@code Vector2i} containing the calculated position.
     */
    public static Vector2i bounce(Vector2i start, Vector2i end, float delta) {
        return bounce(start, new Vector2i(), end, delta);
    }
    
    // --- Elastic Easing (easeOut) ---
    
    /**
     * Calculates the current position using the **Elastic easing** curve (ease-out).
     * The movement simulates a spring, overshooting and oscillating as it settles at the {@code end} position.
     * * @param start The starting position vector.
     * @param current The vector to be mutated with the new calculated position.
     * @param end The target final position vector.
     * @param delta The normalized time elapsed (typically 0.0 to 1.0).
     * @return The mutated {@code current} vector containing the new position.
     */
    public static Vector2i elastic(Vector2i start, Vector2i current, Vector2i end, float delta) {
        // (2 * PI) / 3
        final float c4 = (2.0f * PI_F) / 3.0f;
        float eased;
        
        if (delta == 0.0f) {
            eased = 0.0f;
        } else if (delta == 1.0f) {
            eased = 1.0f;
        } else {
            // pow(2, -10 * delta) * sin((delta * 10 - 0.75) * c4) + 1
            eased = (float) (pow(2.0f, -10.0f * delta) * sin((delta * 10.0f - 0.75f) * c4) + 1.0f);
        }
        return current.set((int) lerp(start.x, end.x, eased), (int) lerp(start.y, end.y, eased));
    }
    
    /**
     * Calculates the current position using the **Elastic easing** curve, returning a new vector.
     * * @param start The starting position vector.
     * @param end The target final position vector.
     * @param delta The normalized time elapsed (typically 0.0 to 1.0).
     * @return A new {@code Vector2i} containing the calculated position.
     */
    public static Vector2i elastic(Vector2i start, Vector2i end, float delta) {
        return elastic(start, new Vector2i(), end, delta);
    }
    
    // --- Quad Easing (easeInOut) ---
    
    /**
     * Calculates the current position using the **Quadratic easing** curve (ease-in/out).
     * The movement uses a power of 2 function, providing a strong but smooth transition.
     * * @param start The starting position vector.
     * @param current The vector to be mutated with the new calculated position.
     * @param end The target final position vector.
     * @param delta The normalized time elapsed (typically 0.0 to 1.0).
     * @return The mutated {@code current} vector containing the new position.
     */
    public static Vector2i quad(Vector2i start, Vector2i current, Vector2i end, float delta) {
        float eased;
        if (delta < 0.5f) {
            // easeInQuad: 2 * delta * delta
            eased = 2.0f * delta * delta;
        } else {
            // easeOutQuad: 1 - pow(-2 * delta + 2, 2) / 2
            eased = (float) (1.0f - pow(-2.0f * delta + 2.0f, 2) / 2.0f);
        }
        return current.set((int) lerp(start.x, end.x, eased), (int) lerp(start.y, end.y, eased));
    }
    
    /**
     * Calculates the current position using the **Quadratic easing** curve, returning a new vector.
     * * @param start The starting position vector.
     * @param end The target final position vector.
     * @param delta The normalized time elapsed (typically 0.0 to 1.0).
     * @return A new {@code Vector2i} containing the calculated position.
     */
    public static Vector2i quad(Vector2i start, Vector2i end, float delta) {
        return quad(start, new Vector2i(), end, delta);
    }
    
    // --- Cubic Easing (easeInOut) ---
    
    /**
     * Calculates the current position using the **Cubic easing** curve (ease-in/out).
     * The movement uses a power of 3 function, offering a more dramatic acceleration and deceleration than Quad.
     * * @param start The starting position vector.
     * @param current The vector to be mutated with the new calculated position.
     * @param end The target final position vector.
     * @param delta The normalized time elapsed (typically 0.0 to 1.0).
     * @return The mutated {@code current} vector containing the new position.
     */
    public static Vector2i cubic(Vector2i start, Vector2i current, Vector2i end, float delta) {
        float eased;
        if (delta < 0.5f) {
            // easeInCubic: 4 * delta^3
            eased = 4.0f * delta * delta * delta;
        } else {
            // easeOutCubic: 1 - pow(-2 * delta + 2, 3) / 2
            eased = (float) (1.0f - pow(-2.0f * delta + 2.0f, 3) / 2.0f);
        }
        return current.set((int) lerp(start.x, end.x, eased), (int) lerp(start.y, end.y, eased));
    }
    
    /**
     * Calculates the current position using the **Cubic easing** curve, returning a new vector.
     * * @param start The starting position vector.
     * @param end The target final position vector.
     * @param delta The normalized time elapsed (typically 0.0 to 1.0).
     * @return A new {@code Vector2i} containing the calculated position.
     */
    public static Vector2i cubic(Vector2i start, Vector2i end, float delta) {
        return cubic(start, new Vector2i(), end, delta);
    }
    
    // --- Quart Easing (easeInOut) ---
    
    /**
     * Calculates the current position using the **Quartic easing** curve (ease-in/out).
     * The movement uses a power of 4 function, resulting in a very sharp transition in speed.
     * * @param start The starting position vector.
     * @param current The vector to be mutated with the new calculated position.
     * @param end The target final position vector.
     * @param delta The normalized time elapsed (typically 0.0 to 1.0).
     * @return The mutated {@code current} vector containing the new position.
     */
    public static Vector2i quart(Vector2i start, Vector2i current, Vector2i end, float delta) {
        float eased;
        if (delta < 0.5f) {
            // easeInQuart: 8 * delta^4
            eased = (float) (8.0f * pow(delta, 4));
        } else {
            // easeOutQuart: 1 - pow(-2 * delta + 2, 4) / 2
            eased = (float) (1.0f - pow(-2.0f * delta + 2.0f, 4) / 2.0f);
        }
        return current.set((int) lerp(start.x, end.x, eased), (int) lerp(start.y, end.y, eased));
    }
    
    /**
     * Calculates the current position using the **Quartic easing** curve, returning a new vector.
     * * @param start The starting position vector.
     * @param end The target final position vector.
     * @param delta The normalized time elapsed (typically 0.0 to 1.0).
     * @return A new {@code Vector2i} containing the calculated position.
     */
    public static Vector2i quart(Vector2i start, Vector2i end, float delta) {
        return quart(start, new Vector2i(), end, delta);
    }
    
    // --- Quint Easing (easeInOut) ---
    
    /**
     * Calculates the current position using the **Quintic easing** curve (ease-in/out).
     * The movement uses a power of 5 function, providing the most abrupt change in speed among the polynomial easings.
     * * @param start The starting position vector.
     * @param current The vector to be mutated with the new calculated position.
     * @param end The target final position vector.
     * @param delta The normalized time elapsed (typically 0.0 to 1.0).
     * @return The mutated {@code current} vector containing the new position.
     */
    public static Vector2i quint(Vector2i start, Vector2i current, Vector2i end, float delta) {
        float eased;
        if (delta < 0.5f) {
            // easeInQuint: 16 * delta^5
            eased = (float) (16.0f * pow(delta, 5));
        } else {
            // easeOutQuint: 1 - pow(-2 * delta + 2, 5) / 2
            eased = (float) (1.0f - pow(-2.0f * delta + 2.0f, 5) / 2.0f);
        }
        return current.set((int) lerp(start.x, end.x, eased), (int) lerp(start.y, end.y, eased));
    }
    
    /**
     * Calculates the current position using the **Quintic easing** curve, returning a new vector.
     * * @param start The starting position vector.
     * @param end The target final position vector.
     * @param delta The normalized time elapsed (typically 0.0 to 1.0).
     * @return A new {@code Vector2i} containing the calculated position.
     */
    public static Vector2i quint(Vector2i start, Vector2i end, float delta) {
        return quint(start, new Vector2i(), end, delta);
    }
    
    // --- Expo Easing (easeInOut) ---
    
    /**
     * Calculates the current position using the **Exponential easing** curve (ease-in/out).
     * The movement uses a power of 2 raised to a high exponent, resulting in a very fast ramp up and cool down.
     * * @param start The starting position vector.
     * @param current The vector to be mutated with the new calculated position.
     * @param end The target final position vector.
     * @param delta The normalized time elapsed (typically 0.0 to 1.0).
     * @return The mutated {@code current} vector containing the new position.
     */
    public static Vector2i expo(Vector2i start, Vector2i current, Vector2i end, float delta) {
        float eased;
        if (delta == 0.0f) {
            eased = 0.0f;
        } else if (delta == 1.0f) {
            eased = 1.0f;
        } else if (delta < 0.5f) {
            // easeInExpo: pow(2, 20 * delta - 10) / 2
            eased = (float) (pow(2.0f, 20.0f * delta - 10.0f) / 2.0f);
        } else {
            // easeOutExpo: (2 - pow(2, -20 * delta + 10)) / 2
            eased = (float) ((2.0f - pow(2.0f, -20.0f * delta + 10.0f)) / 2.0f);
        }
        return current.set((int) lerp(start.x, end.x, eased), (int) lerp(start.y, end.y, eased));
    }
    
    /**
     * Calculates the current position using the **Exponential easing** curve, returning a new vector.
     * * @param start The starting position vector.
     * @param end The target final position vector.
     * @param delta The normalized time elapsed (typically 0.0 to 1.0).
     * @return A new {@code Vector2i} containing the calculated position.
     */
    public static Vector2i expo(Vector2i start, Vector2i end, float delta) {
        return expo(start, new Vector2i(), end, delta);
    }
    
    // --- Circ Easing (easeInOut) ---
    
    /**
     * Calculates the current position using the **Circular easing** curve (ease-in/out).
     * The movement simulates a segment of a circular arc, providing smooth, slightly slower acceleration/deceleration.
     * * @param start The starting position vector.
     * @param current The vector to be mutated with the new calculated position.
     * @param end The target final position vector.
     * @param delta The normalized time elapsed (typically 0.0 to 1.0).
     * @return The mutated {@code current} vector containing the new position.
     */
    public static Vector2i circ(Vector2i start, Vector2i current, Vector2i end, float delta) {
        float eased;
        if (delta < 0.5f) {
            // easeInCirc: (1 - sqrt(1 - pow(2 * delta, 2))) / 2
            eased = (float) ((1.0f - sqrt(1.0f - pow(2.0f * delta, 2))) / 2.0f);
        } else {
            // easeOutCirc: (sqrt(1 - pow(-2 * delta + 2, 2)) + 1) / 2
            eased = (float) ((sqrt(1.0f - pow(-2.0f * delta + 2.0f, 2)) + 1.0f) / 2.0f);
        }
        return current.set((int) lerp(start.x, end.x, eased), (int) lerp(start.y, end.y, eased));
    }
    
    /**
     * Calculates the current position using the **Circular easing** curve, returning a new vector.
     * * @param start The starting position vector.
     * @param end The target final position vector.
     * @param delta The normalized time elapsed (typically 0.0 to 1.0).
     * @return A new {@code Vector2i} containing the calculated position.
     */
    public static Vector2i circ(Vector2i start, Vector2i end, float delta) {
        return circ(start, new Vector2i(), end, delta);
    }
}