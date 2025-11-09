package org.infinitytwo.umbralore.core;

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
        super(red,green,blue);
        this.alpha = clamp(0,1,alpha);
    }

    public RGBA(RGBA color) {
        super(color);
        alpha = color.alpha;
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
            return new RGBA(1.0f, 1.0f, 1.0f,alpha);
        } else {
            return new RGBA(0.0f, 0.0f, 0.0f,alpha);
        }
    }
    
    public float getAlpha() {
        return alpha;
    }

    public void setAlpha(float alpha) {
        this.alpha = clamp(0,1,alpha);
    }

    @Override
    public RGBA add(float num) {
        super.add(num);
        this.alpha = alpha + num;
        return this;
    }

    public RGBA set(float r, float g, float b, float a) {
        this.red = r;
        this.green = g;
        this.blue = b;
        this.alpha = a;
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

    public void a(int a) {
        setAlpha(a);
    }
    
    @Override
    public RGBA getContrastColor() {
        RGB rgb = super.getContrastColor();
        return new RGBA(rgb.red, rgb.green, rgb.blue, a());
    }
}
