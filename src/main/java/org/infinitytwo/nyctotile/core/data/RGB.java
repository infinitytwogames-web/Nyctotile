package org.infinitytwo.nyctotile.core.data;

import org.joml.Math;

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
    
    // Assuming the RGBA class has public fields or accessor methods (r(), g(), b())
    public RGB(RGBA color) {
        this(color.r(), color.g(), color.b());
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
        return this; // Make chainable
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
    
    public RGB set(float r, float g, float b) {
        // Uses clamped setters
        r(r); g(g); b(b);
        return this; // Make chainable
    }
    
    // --- Utility Methods ---
    
    public static RGB getContrastColor(float red, float green, float blue) {
        float luminosity = (0.2126f * red) +
                (0.7152f * green) +
                (0.0722f * blue);
        
        float threshold = 0.5f;
        
        if (luminosity < threshold) {
            return new RGB(1.0f, 1.0f, 1.0f);
        } else {
            return new RGB(0.0f, 0.0f, 0.0f);
        }
    }
    
    public RGB getContrastColor() {
        return getContrastColor(this.red, this.green, this.blue);
    }
    
    public RGB copy() {
        return new RGB(red, green, blue);
    }
    
    public static RGB lerp(RGB a, RGB b, float t) {
        t = clamp(0,1,t);
        return new RGB(
                Math.lerp(a.red,b.red,t),
                Math.lerp(a.green,b.green,t),
                Math.lerp(a.blue,b.blue,t)
        );
    }
    
    // --- Standard Overrides (Required for Data Integrity) ---
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RGB rgb = (RGB) o;
        // Use Float.compare for safe floating-point equality checks
        return Float.compare(rgb.red, red) == 0 &&
                Float.compare(rgb.green, green) == 0 &&
                Float.compare(rgb.blue, blue) == 0;
    }
    
    @Override
    public int hashCode() {
        int result = Float.hashCode(red);
        result = 31 * result + Float.hashCode(green);
        result = 31 * result + Float.hashCode(blue);
        return result;
    }
    
    @Override
    public String toString() {
        return String.format("RGB(%.3f, %.3f, %.3f)", red, green, blue);
    }
}