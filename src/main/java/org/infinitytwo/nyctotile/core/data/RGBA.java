package org.infinitytwo.nyctotile.core.data;

import static org.joml.Math.clamp;

public class RGBA extends RGB {
    protected float alpha = 0;
    
    public RGBA(int red, int green, int blue, float alpha) {
        super(red, green, blue);
        this.alpha = alpha;
    }
    
    public RGBA() {
        super();
    }
    
    public RGBA(float red, float green, float blue, float alpha) {
        super(red, green, blue);
        this.alpha = clamp(0, 1, alpha);
    }
    
    public RGBA(RGBA color) {
        super(color);
        alpha = color.alpha;
    }
    
    public RGBA(RGB rgb, float alpha) {
        this.set(rgb);
        this.alpha = clamp(0, 1, alpha);
    }
    
    public static RGBA fromRGBA(int red, int green, int blue, float alpha) {
        float r = (float) red / 255f;
        float g = (float) green / 255f;
        float b = (float) blue / 255f;
        return new RGBA(r, g, b, alpha);
    }
    
    public static RGBA getContrastColor(float red, float green, float blue, float alpha) {
        float luminosity = (0.2126f * red) +
                (0.7152f * green) +
                (0.0722f * blue);
        
        float threshold = 0.5f;
        
        if (luminosity < threshold) {
            return new RGBA(1.0f, 1.0f, 1.0f, alpha);
        } else {
            return new RGBA(0.0f, 0.0f, 0.0f, alpha);
        }
    }
    
    public static RGBA getContrastColor(RGBA rgba) {
        return getContrastColor(rgba.r(),rgba.g(),rgba.b(),rgba.alpha);
    }
    
    // IDK, but I guess someone is lazy enough to type new RGBA()
    public static RGBA copy(RGBA color) {
        return new RGBA(color);
    }
    
    public float getAlpha() {
        return alpha;
    }
    
    public RGBA setAlpha(float alpha) {
        this.alpha = clamp(0, 1, alpha);
        return this;
    }
    
    @Override
    public RGBA add(float num) {
        super.add(num);
        this.alpha = clamp(0, 1, alpha + num);
        return this;
    }
    
    public RGBA set(float r, float g, float b, float a) {
        this.red = clamp(0, 1, r);
        this.green = clamp(0, 1, g);
        this.blue = clamp(0, 1, b);
        this.alpha = clamp(0, 1, a);
        return this;
    }
    
    public RGBA set(RGBA color) {
        red = color.red;
        green = color.green;
        blue = color.blue;
        alpha = color.alpha;
        return this;
    }
    
    public float a() {
        return getAlpha();
    }
    
    public RGBA a(int a) {
        return setAlpha(a);
    }
    
    @Override
    public RGBA getContrastColor() {
        RGB rgb = super.getContrastColor();
        return new RGBA(rgb.red, rgb.green, rgb.blue, a());
    }
    
    @Override
    public RGBA copy() {
        return new RGBA(super.copy(), alpha);
    }
}
