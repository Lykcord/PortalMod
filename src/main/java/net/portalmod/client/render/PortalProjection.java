package net.portalmod.client.render;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.joml.Vector4fc;

public final class PortalProjection {
    private PortalProjection() {}

    /** Keep the rendered x/y aperture while giving the CPU frustum conventional forward depth. */
    public static Matrix4f forCulling(Matrix4fc reversedProjection, boolean zeroToOne) {
        var result = new Matrix4f(reversedProjection);
        var depth = result.getRow(2, new Vector4f());
        return result.setRow(2, zeroToOne ? result.getRow(3, new Vector4f()).sub(depth) : depth.negate());
    }

    public static Matrix4f clipAtPlane(Matrix4fc projection, Vector4fc plane, boolean zeroToOne) {
        var result = new Matrix4f(projection);
        // A plane through the eye produces a singular projection during portal crossings.
        if (plane.w() >= -.0001f) {
            return result;
        }
        float farDepth = zeroToOne ? 0 : -1;
        var oppositeCorner =
                new Matrix4f(projection)
                        .invert()
                        .transform(new Vector4f(Math.signum(plane.x()), Math.signum(plane.y()), farDepth, 1));
        float denominator = plane.dot(oppositeCorner);
        if (!Float.isFinite(denominator) || denominator <= .000001f) {
            return result;
        }
        float scale = (1 - farDepth) / denominator;
        // 26.3 uses reversed depth: the near clip plane is w - z >= 0.
        return result.setRow(2, result.getRow(3, new Vector4f()).sub(new Vector4f(plane).mul(scale)));
    }
}
