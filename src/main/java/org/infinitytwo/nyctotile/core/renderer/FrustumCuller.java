package org.infinitytwo.nyctotile.core.renderer;

import org.joml.Matrix4f;
import org.joml.Vector3f;

class Plane {
    public float a, b, c, d;

    public void set(float a, float b, float c, float d) {
        float invLength = (float) (1.0 / Math.sqrt(a * a + b * b + c * c));
        this.a = a * invLength;
        this.b = b * invLength;
        this.c = c * invLength;
        this.d = d * invLength;
    }
}

public class FrustumCuller {
    // TODO: THIS CULLER IS TOO AGGRESSIVE
    private final Plane[] planes = new Plane[6]; // Left, Right, Bottom, Top, Near, Far

    public FrustumCuller() {
        for (int i = 0; i < 6; i++) {
            planes[i] = new Plane();
        }
    }

    /**
     * Updates the six frustum planes from the combined View-Projection matrix.
     */
    public void update(Matrix4f viewProjection) {
        // JOML provides access to the matrix elements as m(row, col)
        // Standard Frustum Plane Extraction (JOML uses column-major order internally)

        // Right plane (4th column - 1st column)
        planes[0].set(viewProjection.m30() - viewProjection.m00(),
                viewProjection.m31() - viewProjection.m01(),
                viewProjection.m32() - viewProjection.m02(),
                viewProjection.m33() - viewProjection.m03());

        // Left plane (4th column + 1st column)
        planes[1].set(viewProjection.m30() + viewProjection.m00(),
                viewProjection.m31() + viewProjection.m01(),
                viewProjection.m32() + viewProjection.m02(),
                viewProjection.m33() + viewProjection.m03());

        // Bottom plane (4th column + 2nd column)
//        planes[2].set(0, 0, 0, 1);
        // NOTE: We are subtracting here instead of adding (standard Top plane formula)
        planes[2].set(viewProjection.m30() - viewProjection.m10(),
                viewProjection.m31() - viewProjection.m11(),
                viewProjection.m32() - viewProjection.m12(),
                viewProjection.m33() - viewProjection.m13());

// Top plane (4th column + 2nd column) - Index 3
// NOTE: We are adding here instead of subtracting (standard Bottom plane formula)
        planes[3].set(viewProjection.m30() + viewProjection.m10(),
                viewProjection.m31() + viewProjection.m11(),
                viewProjection.m32() + viewProjection.m12(),
                viewProjection.m33() + viewProjection.m13());
        // Near plane (4th column + 3rd column)
//         planes[4].set(0, 0, 0, 1);
        planes[4].set(viewProjection.m30() + viewProjection.m20(),
                viewProjection.m31() + viewProjection.m21(), // CRITICAL FIX: Was m11, should be m21
                viewProjection.m32() + viewProjection.m22(),
                viewProjection.m33() + viewProjection.m23());

        // Far plane (4th column - 3rd column)
        planes[5].set(viewProjection.m30() - viewProjection.m20(),
                viewProjection.m31() - viewProjection.m21(), // CRITICAL FIX: Was m11, should be m21
                viewProjection.m32() - viewProjection.m22(),
                viewProjection.m33() - viewProjection.m23());
    }

    public boolean isVisible(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        for (Plane p : planes) {
            // Find the P-vertex (Positive vertex: the one furthest in the plane's normal direction)
            // If the plane normal (p.a, p.b, p.c) is positive, we use the MIN side of the box
            // because we are testing distance from the camera (world space).

            // CORRECTION: The AABB test finds the vertex furthest in the direction of the normal (positive side).
            // If this vertex is outside (distance < 0), the whole box is outside.

            float pX = (p.a >= 0) ? maxX : minX; // Use MAX side if normal is positive
            float pY = (p.b >= 0) ? maxY : minY;
            float pZ = (p.c >= 0) ? maxZ : minZ;

            // Calculate distance from the P-vertex to the plane (A*x + B*y + C*z + D)
            float distance = p.a * pX + p.b * pY + p.c * pZ + p.d;

            if (distance < 0) {
                return false; // Fully outside this plane (culled)
            }
        }
        return true; // Visible
    }

    public boolean isVisible(Vector3f min, Vector3f max) {
        return isVisible(min.x, min.y, min.z, max.x, max.y, max.z);
    }
}
