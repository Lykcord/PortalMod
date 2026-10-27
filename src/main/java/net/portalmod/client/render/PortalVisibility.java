package net.portalmod.client.render;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.jspecify.annotations.Nullable;

/** Conservative screen bounds of the exact quad submitted for a portal opening. */
public final class PortalVisibility {
    private static final float GUARD_PIXELS = 2;

    private PortalVisibility() {}

    /** All four corners behind a linear shader clip plane means no fragment can survive. */
    public static boolean fullyClipped(Matrix4fc modelView, Vector4fc plane) {
        for (int x = 0; x <= 1; x++) {
            for (int y = 0; y <= 2; y += 2) {
                var corner = modelView.transform(new Vector4f(x, y, 0, 1));
                if (!corner.isFinite() || plane.dot(corner) >= -.0001F) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Null retains the parent viewport across the eye plane; an empty rectangle rejects a fully behind-eye quad. */
    public static float @Nullable [] projectOpening(Matrix4fc modelView, Matrix4fc projection, int width, int height) {
        var clipTransform = new Matrix4f(projection).mul(modelView);
        var corners = new Vector4f[4];
        boolean hasVisibleCorner = false, crossesEyePlane = false;
        int index = 0;
        for (int x = 0; x <= 1; x++) {
            for (int y = 0; y <= 2; y += 2) {
                var corner = clipTransform.transform(new Vector4f(x, y, 0, 1));
                if (!corner.isFinite()) {
                    return null;
                }
                corners[index++] = corner;
                hasVisibleCorner |= corner.w > 0;
                crossesEyePlane |= corner.w <= .01F;
            }
        }
        if (!hasVisibleCorner) {
            return new float[4];
        }
        if (crossesEyePlane) {
            return null;
        }
        float left = Float.POSITIVE_INFINITY, bottom = left, right = Float.NEGATIVE_INFINITY, top = right;
        for (var corner : corners) {
            float ndcX = corner.x / corner.w, ndcY = corner.y / corner.w;
            left = Math.min(left, ndcX);
            right = Math.max(right, ndcX);
            bottom = Math.min(bottom, ndcY);
            top = Math.max(top, ndcY);
        }
        // Linear filtering and camera bob can straddle a pixel at an aperture edge.
        float horizontalPadding = 2 * GUARD_PIXELS / Math.max(1, width),
                verticalPadding = 2 * GUARD_PIXELS / Math.max(1, height);
        return new float[] {
            left - horizontalPadding, bottom - verticalPadding, right + horizontalPadding, top + verticalPadding
        };
    }
}
