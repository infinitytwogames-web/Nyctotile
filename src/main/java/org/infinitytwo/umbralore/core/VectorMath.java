package org.infinitytwo.umbralore.core;

import org.joml.Vector2i;
import org.joml.Vector3f;

import java.nio.ByteBuffer;

public final class VectorMath {
    public static boolean isPointWithinRectangle(Vector2i topLeft, Vector2i point, Vector2i bottomRight) {
        int minX = topLeft.x();
        int maxX = bottomRight.x();
        int minY = topLeft.y();
        int maxY = bottomRight.y();

        int x = point.x();
        int y = point.y();

        return x >= minX && x <= maxX && y >= minY && y <= maxY;
    }

    public static boolean isPointWithinRectangle(Vector2i topRight, int pointX, int pointY, Vector2i bottomLeft) {
        float minX = bottomLeft.x();
        float maxX = topRight.x();
        float minY = topRight.y();
        float maxY = bottomLeft.y();

        return (float) pointX >= minX && (float) pointX <= maxX &&
                (float) pointY >= minY && (float) pointY <= maxY;
    }

    public static String toStringAsId(Vector2i v) {
        return v.x+"-"+v.y;
    }

    public static byte[] serialize(Vector3f v) {
        ByteBuffer buffer = ByteBuffer.allocate(3 * Float.BYTES);
        buffer.putFloat(v.x); buffer.putFloat(v.y); buffer.putFloat(v.z);
        return buffer.array();
    }
}