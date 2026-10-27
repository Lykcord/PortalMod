package net.portalmod.client.render;

import static org.junit.Assert.*;

import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;

public class PortalProjectionTest {
    private static Matrix4f projection(boolean zeroToOne) {
        return new Matrix4f().setPerspective((float) Math.toRadians(70), 16f / 9, 500, .05f, zeroToOne);
    }

    @Test
    public void exitPlaneReplacesReversedNearDepthWithoutMovingScreenCoordinates() {
        for (boolean zeroToOne : new boolean[] {false, true}) {
            var original = projection(zeroToOne);
            var clipped = PortalProjection.clipAtPlane(original, new Vector4f(0, 0, -1, -2), zeroToOne);
            var onPlane = clipped.transform(new Vector4f(.5f, -.2f, -2, 1));
            assertEquals(onPlane.w, onPlane.z, .000001);
            var behind = clipped.transform(new Vector4f(0, 0, -1, 1));
            assertTrue(behind.z > behind.w);
            var kept = clipped.transform(new Vector4f(0, 0, -3, 1));
            assertTrue(kept.z < kept.w);
            assertTrue(kept.z >= (zeroToOne ? 0 : -kept.w));
            var point = new Vector4f(.5f, -.2f, -3, 1);
            var before = original.transform(new Vector4f(point));
            var after = clipped.transform(new Vector4f(point));
            assertEquals(before.x / before.w, after.x / after.w, 0);
            assertEquals(before.y / before.w, after.y / after.w, 0);
        }
    }

    @Test
    public void tiltedExitPlaneCutsCorrectHalfSpaceOnBothGraphicsDepthConventions() {
        var plane = new Vector4f(.3f, .2f, -1, -2);
        for (boolean zeroToOne : new boolean[] {false, true}) {
            var clipped = PortalProjection.clipAtPlane(projection(zeroToOne), plane, zeroToOne);
            for (float x : new float[] {-.5f, 0, .5f}) {
                float z = .3f * x + .2f * .25f - 2;
                var on = clipped.transform(new Vector4f(x, .25f, z, 1));
                assertEquals(on.w, on.z, .000001);
                var kept = clipped.transform(new Vector4f(x, .25f, z - .25f, 1));
                var removed = clipped.transform(new Vector4f(x, .25f, z + .25f, 1));
                assertTrue(kept.z < kept.w);
                assertTrue(removed.z > removed.w);
            }
        }
    }

    @Test
    public void eyePlaneAndInvalidPlanesKeepFiniteOriginalProjection() {
        for (var plane :
                new Vector4f[] {
                    new Vector4f(0, 0, -1, 0), new Vector4f(0, 0, 0, -1), new Vector4f(Float.NaN, 0, -1, -2)
                }) {
            var original = projection(true);
            assertEquals(original, PortalProjection.clipAtPlane(original, plane, true));
        }
    }
}
