package org.infinitytwo.nyctotile.core;

import org.joml.*;
import org.joml.Math;

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
    
    public static Vector3f toFloat(Vector3i vector) {
        return new Vector3f(vector);
    }
    
    public static Vector3i toInt(Vector3f vector) {
        return new Vector3i(Math.round(vector.x), Math.round(vector.y), Math.round(vector.z));
    }
    
    public static String toString(Vector3i vector) {
        return "x: "+vector.x + ", y: "+ vector.y+ ", z: "+vector.z;
    }
    
    public static Vector2i copy(Vector2i vector) {
        return new Vector2i(vector);
    }
}