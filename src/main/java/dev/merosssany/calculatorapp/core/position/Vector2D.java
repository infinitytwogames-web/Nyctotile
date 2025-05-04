package dev.merosssany.calculatorapp.core.position;

import java.util.Objects;
import java.util.Vector;

public class Vector2D<T extends Number & Comparable<T>> {
    public T x;
    public T y;

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

    public boolean isVectorPointIncludedIn(Vector2D<? extends Number> target, Vector2D<? extends Number> other) {
        float minX = this.getX().floatValue();
        float maxX = other.getX().floatValue();
        float minY = other.getY().floatValue(); // Bottom boundary (assuming UI y increases downwards or is already flipped)
        float maxY = this.getY().floatValue(); // Top boundary (assuming UI y increases downwards or is already flipped)

        float targetX = target.getX().floatValue();
        float targetY = target.getY().floatValue();

        return targetX >= minX && targetX <= maxX &&
                targetY <= maxY && targetY >= minY; // Adjusted Y comparisons
    }

    public boolean isVectorPointIncludedAround(Vector2D<T> target, Vector2D<T> other) {
        T minX = (target.getX().compareTo(other.getX()) < 0) ? target.getX() : other.getX();
        T maxX = (target.getX().compareTo(other.getX()) > 0) ? target.getX() : other.getX();
        T minY = (target.getY().compareTo(other.getY()) < 0) ? target.getY() : other.getY();
        T maxY = (target.getY().compareTo(other.getY()) > 0) ? target.getY() : other.getY();

        return this.x.compareTo(minX) > 0 && this.x.compareTo(maxX) < 0 &&
                this.y.compareTo(minY) > 0 && this.y.compareTo(maxY) < 0;
    }

    public static <T extends Number & Comparable<T>> boolean isVectorPointIncludedIn(Vector2D<T> from, Vector2D<T> target, Vector2D<T> to) {
        T minX = (target.getX().compareTo(to.getX()) <= 0) ? target.getX() : to.getX();
        T maxX = (target.getX().compareTo(to.getX()) > 0) ? target.getX() : to.getX();
        T minY = (target.getY().compareTo(to.getY()) <= 0) ? target.getY() : to.getY();
        T maxY = (target.getY().compareTo(to.getY()) > 0) ? target.getY() : to.getY();

        return from.x.compareTo(minX) >= 0 && from.x.compareTo(maxX) <= 0 &&
                from.y.compareTo(minY) >= 0 && from.y.compareTo(maxY) <= 0;
    }

    public static <T extends Number & Comparable<T>> boolean isVectorPointIncludedAround(Vector2D<T> from, Vector2D<T> target, Vector2D<T> to) {
        T minX = (target.getX().compareTo(to.getX()) < 0) ? target.getX() : to.getX();
        T maxX = (target.getX().compareTo(to.getX()) > 0) ? target.getX() : to.getX();
        T minY = (target.getY().compareTo(to.getY()) < 0) ? target.getY() : to.getY();
        T maxY = (target.getY().compareTo(to.getY()) > 0) ? target.getY() : to.getY();

        return from.x.compareTo(minX) > 0 && from.x.compareTo(maxX) < 0 &&
                from.y.compareTo(minY) > 0 && from.y.compareTo(maxY) < 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }
}
