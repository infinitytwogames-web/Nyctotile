package dev.merosssany.calculatorapp.core.position;

import java.util.Objects;
import java.util.Vector;

public class Vector2D<T extends Number> implements Cloneable {
    private T x;
    private T y;

    public Vector2D(T x, T y) {
        this.x = x;
        this.y = y;
    }

    public Vector2D(Vector<T> vector) {
        if (vector == null || vector.size() < 2) {
            throw new IllegalArgumentException("Vector must have at least two elements to initialize Vector2D.");
        }
        this.x = vector.get(0);
        this.y = vector.get(1);
    }

    @Override
    public Vector2D<T> clone() throws CloneNotSupportedException {
        return (Vector2D<T>) super.clone();
    }

    public T getX() {
        return x;
    }

    public void setX(T x) {
        this.x = x;
    }

    public T getY() {
        return y;
    }

    public void setY(T y) {
        this.y = y;
    }

    public Vector2D<Double> add(Vector2D<? extends Number> other) {
        double sumX = this.x.doubleValue() + other.getX().doubleValue();
        double sumY = this.y.doubleValue() + other.getY().doubleValue();
        return new Vector2D<>(sumX, sumY);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Vector2D<?> other = (Vector2D<?>) obj;
        return Objects.equals(this.x, other.x) && Objects.equals(this.y, other.y);
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }
}
