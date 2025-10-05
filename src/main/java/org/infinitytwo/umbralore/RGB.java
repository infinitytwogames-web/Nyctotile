package org.infinitytwo.umbralore;

public class RGB {
    public float red;
    public float green;
    public float blue;

    public RGB(float red, float green, float blue) {
        this.blue = blue;
        this.green = green;
        this.red = red;
    }

    public RGB() {
        this.red = 0;
        this.green = 0;
        this.blue = 0;
    }

    public RGB add(float num) {
        this.red = red + num;
        this.green = green + num;
        this.blue = blue + num;
        return this;
    }

    public float getRed() {
        return red;
    }

    public void setRed(float red) {
        this.red = red;
    }

    public float getGreen() {
        return green;
    }

    public void setGreen(float green) {
        this.green = green;
    }

    public float getBlue() {
        return blue;
    }

    public void setBlue(float blue) {
        this.blue = blue;
    }

    public float r() {
        return getRed();
    }

    public float g() {
        return getGreen();
    }

    public float b() {
        return getBlue();
    }

    public void r(int r) {
        setRed(r);
    }

    public void g(int g) {
        setGreen(g);
    }

    public void b(int b) {
        setBlue(b);
    }

    public void set(float r, float g, float b) {
        this.red = r;
        this.blue = b;
        this.green = g;
    }
}