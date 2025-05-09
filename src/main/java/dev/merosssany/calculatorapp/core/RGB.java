package dev.merosssany.calculatorapp.core;

public class RGB {
    private float red;
    private float green;
    private float blue;

    public RGB(float red, float green, float blue) {
        this.blue = blue;
        this.green = green;
        this.red = red;
    }

    public RGB(int red, int green, int blue) {
        this.red = (float) red / 255;
        this.green = (float) green / 255;
        this.blue = (float) blue / 255;
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
}