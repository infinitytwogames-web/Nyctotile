package dev.merosssany.calculatorapp.core;

public class RGBA extends RGB {
    private float alpha = 0;

    public RGBA(int red, int green, int blue, float alpha) {
        super(red, green, blue);
        this.alpha = alpha;
    }

    public RGBA() {
        super(1,1,1);
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
}
