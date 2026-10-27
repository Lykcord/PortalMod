package net.portalmod.core.math;

import static org.junit.Assert.assertEquals;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

public class Mat4Test {
    @Test
    public void exportsTranslationWithJomlColumnOrder() {
        Vector3f result = Mat4.createTranslation(3, -2, 5).to4f().transformPosition(new Vector3f(1, 2, 3));
        assertVector(result, 4, 0, 8);
    }

    @Test
    public void importsJomlRotationAndTranslation() {
        Matrix4f original = new Matrix4f().translation(3, -2, 5).rotateY(0.7f);
        Vec3 point = new Vec3(1, 2, 3).transform(new Mat4(original));
        Vector3f expected = original.transformPosition(new Vector3f(1, 2, 3));
        assertEquals(expected.x, point.x, 0.00001);
        assertEquals(expected.y, point.y, 0.00001);
        assertEquals(expected.z, point.z, 0.00001);
    }

    @Test
    public void importsJomlQuaternion() {
        Quaternionf rotation = new Quaternionf().rotationY((float) (Math.PI / 2));
        Vec3 point = new Vec3(1, 0, 0).transform(new Mat4(rotation));
        assertEquals(0, point.x, 0.00001);
        assertEquals(-1, point.z, 0.00001);
    }

    @Test
    public void rotatesRadiansBeyondOneTurn() {
        Vec3 point =
                new Vec3(1, 0, 0).transform(Mat4.identity().rotateRad(new Vector3f(0, 1, 0), (float) (Math.PI * 2.5)));
        assertEquals(0, point.x, 0.00001);
        assertEquals(-1, point.z, 0.00001);
    }

    @Test
    public void rotatesDegrees() {
        Vec3 point = new Vec3(1, 0, 0).transform(Mat4.identity().rotateDeg(new Vector3f(0, 1, 0), 450));
        assertEquals(0, point.x, 0.00001);
        assertEquals(-1, point.z, 0.00001);
    }

    private static void assertVector(Vector3f actual, float x, float y, float z) {
        assertEquals(x, actual.x, 0.00001);
        assertEquals(y, actual.y, 0.00001);
        assertEquals(z, actual.z, 0.00001);
    }
}
