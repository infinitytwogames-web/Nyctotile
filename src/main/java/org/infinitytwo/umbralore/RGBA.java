package org.infinitytwo.umbralore;

public class RGBA extends RGB {
    public float alpha = 0;

    public RGBA(int red, int green, int blue, float alpha) {
        super(red, green, blue);
        this.alpha = alpha;
    }

    public RGBA() {
        super();
    }

    public RGBA(float red, float green, float blue, float alpha) {
        super(red,green,blue);
        this.alpha = alpha;
    }

    public float getAlpha() {
        return alpha;
    }

    public void setAlpha(float alpha) {
        this.alpha = alpha;
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
}
