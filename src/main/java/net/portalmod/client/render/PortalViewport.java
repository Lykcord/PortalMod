package net.portalmod.client.render;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;

/** A portal's visible rectangle in the original camera's normalized device coordinates. */
public record PortalViewport(float left, float bottom, float right, float top) {
    public static final PortalViewport FULL = new PortalViewport(-1, -1, 1, 1);

    public PortalViewport {
        if (!Float.isFinite(left)
                || !Float.isFinite(bottom)
                || !Float.isFinite(right)
                || !Float.isFinite(top)
                || right <= left
                || top <= bottom) {
            throw new IllegalArgumentException("Empty portal viewport");
        }
    }

    public PortalViewport intersect(float[] rect) {
        float l = Math.max(left, rect[0]), b = Math.max(bottom, rect[1]);
        float r = Math.min(right, rect[2]), t = Math.min(top, rect[3]);
        return r <= l || t <= b ? null : new PortalViewport(l, b, r, t);
    }

    public Matrix4f projection(Matrix4fc base) {
        return new Matrix4f()
                .scaling(2 / (right - left), 2 / (top - bottom), 1)
                .translate(-(right + left) / 2, -(top + bottom) / 2, 0)
                .mul(base);
    }

    public Vector4f sampleFrom(PortalViewport parent) {
        return new Vector4f(
                (parent.right - parent.left) / (right - left),
                (parent.top - parent.bottom) / (top - bottom),
                (parent.left - left) / (right - left),
                (parent.bottom - bottom) / (top - bottom));
    }

    public int width(int fullWidth) {
        return Math.max(1, (int) Math.ceil(fullWidth * (right - left) / 2));
    }

    public int height(int fullHeight) {
        return Math.max(1, (int) Math.ceil(fullHeight * (top - bottom) / 2));
    }

    // Keep slightly different apertures in the same texture-size bucket. Camera motion
    // otherwise allocates new color, depth and transparency targets almost every frame.
    public int targetWidth(int fullWidth) {
        return bucket(width(fullWidth), fullWidth);
    }

    public int targetHeight(int fullHeight) {
        return bucket(height(fullHeight), fullHeight);
    }

    private static int bucket(int exact, int full) {
        return Math.min(Math.max(1, full), (exact + 31) & ~31);
    }
}
