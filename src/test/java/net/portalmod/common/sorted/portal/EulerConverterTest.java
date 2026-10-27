package net.portalmod.common.sorted.portal;

import static org.junit.Assert.*;

import net.portalmod.core.math.Vec3;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

public class EulerConverterTest {
    @Test
    public void nativeCameraQuaternionKeepsPortalForwardUpAndLeftAxesIncludingRoll() {
        for (float pitch : new float[] {-90, -75, 0, 30, 90}) {
            for (float yaw : new float[] {-180, -45, 0, 90, 180}) {
                for (float roll : new float[] {-120, 0, 45, 180}) {
                    var basis = EulerConverter.toVectors(pitch, yaw, roll);
                    var rotation = EulerConverter.toCameraRotation(pitch, yaw, roll);
                    assertVector(basis.getZ(), new Vec3(new Vector3f(0, 0, -1).rotate(rotation)));
                    assertVector(basis.getY(), new Vec3(new Vector3f(0, 1, 0).rotate(rotation)));
                    assertVector(basis.getX(), new Vec3(new Vector3f(-1, 0, 0).rotate(rotation)));
                }
            }
        }
    }

    @Test
    public void cameraRotationsRoundTripThroughPortalBasis() {
        for (float pitch : new float[] {-90, -75, 0, 30, 90}) {
            for (float yaw : new float[] {-180, -45, 0, 90, 180}) {
                for (float roll : new float[] {-120, 0, 45, 180}) {
                    var basis = EulerConverter.toVectors(pitch, yaw, roll);
                    var angles = EulerConverter.toEulerAngles(basis);
                    assertTrue(Float.isFinite(angles.getPitch()));
                    assertTrue(Float.isFinite(angles.getYaw()));
                    assertTrue(Float.isFinite(angles.getRoll()));
                    var restored = EulerConverter.toVectors(angles.getPitch(), angles.getYaw(), angles.getRoll());
                    assertVector(basis.getY(), restored.getY());
                    assertVector(basis.getZ(), restored.getZ());
                }
            }
        }
    }

    private static void assertVector(Vec3 expected, Vec3 actual) {
        assertEquals(expected.x, actual.x, .001);
        assertEquals(expected.y, actual.y, .001);
        assertEquals(expected.z, actual.z, .001);
    }
}
