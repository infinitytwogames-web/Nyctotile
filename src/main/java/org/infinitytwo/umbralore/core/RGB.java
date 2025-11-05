package org.infinitytwo.umbralore.core;

import static org.joml.Math.clamp;

public class RGB {
    protected float red;
    protected float green;
    protected float blue;
    
    // --- Constructors ---
    
    public RGB(float red, float green, float blue) {
        this.red = clamp(red, 0, 1);
        this.green = clamp(green, 0, 1);
        this.blue = clamp(blue, 0, 1);
    }
    
    public RGB() {
        this(0, 0, 0);
    }
    
    public RGB(RGBA color) {
        // Delegates to main constructor, ensuring proper component copying and clamping
        this(color.red, color.green, color.blue);
    }
    
    // --- Factory Method ---
    
    public static RGB fromRGB(int red, int green, int blue) {
        float r = (float) red / 255f;
        float g = (float) green / 255f;
        float b = (float) blue / 255f;
        return new RGB(r, g, b);
    }
    
    // --- Mutable Operations ---
    
    public RGB add(float num) {
        this.red = clamp(this.red + num, 0, 1);
        this.green = clamp(this.green + num, 0, 1);
        this.blue = clamp(this.blue + num, 0, 1);
        return this;
    }
    
    public RGB set(RGB other) {
        // Direct assignment is safe and faster since 'other' is already a valid RGB
        this.red = other.red;
        this.green = other.green;
        this.blue = other.blue;
        return this;
    }
    
    public RGB set(RGBA color) {
        // Direct assignment is safe since the constructor ensures the input RGBA components were clamped
        this.red = color.red;
        this.green = color.green;
        this.blue = color.blue;
        return this;
    }
    
    // --- Getters and Setters ---
    
    public float getRed() { return red; }
    public void setRed(float red) { this.red = clamp(red, 0, 1); }
    
    public float getGreen() { return green; }
    public void setGreen(float green) { this.green = clamp(green, 0, 1); }
    
    public float getBlue() { return blue; }
    public void setBlue(float blue) { this.blue = clamp(blue, 0, 1); }
    
    public float r() { return getRed(); }
    public float g() { return getGreen(); }
    public float b() { return getBlue(); }
    
    public void r(float r) { setRed(r); }
    public void g(float g) { setGreen(g); }
    public void b(float b) { setBlue(b); }
    
    public void set(float r, float g, float b) {
        // Uses clamped setters
        r(r); g(g); b(b);
    }
    
    // --- Utility Methods ---
    
    public RGB getContrastColor() {
        float luminosity = (0.2126f * this.red) +
                (0.7152f * this.green) +
                (0.0722f * this.blue);
        
        float threshold = 0.5f;
        
        if (luminosity < threshold) {
            return new RGB(1.0f, 1.0f, 1.0f);
        } else {
            return new RGB(0.0f, 0.0f, 0.0f);
        }
    }
    
    public RGB copy() {
        return new RGB(red, green, blue);
    }
    
    public static RGB lerp(RGB a, RGB b, float t) {
        t = clamp(t, 0f, 1f);
        return new RGB(
                a.red + (b.red - a.red) * t,
                a.green + (b.green - a.green) * t,
                a.blue + (b.blue - a.blue) * t
        );
    }
    
    @Override
    public String toString() {
        return String.format("RGB(%.3f, %.3f, %.3f)", red, green, blue);
    }
}