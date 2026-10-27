package net.portalmod.common.sorted.button;

import static org.junit.Assert.*;

import net.minecraft.core.Direction;
import net.portalmod.core.math.Mat4;
import net.portalmod.core.math.Vec3;

import org.junit.jupiter.api.Test;

public class SuperButtonModelTransformTest {
    @Test
    public void everyCornerFacesOutwardOnAllSixMountingSides() {
        for (var face : Direction.values()) {
            for (var corner : QuadBlockCorner.values()) {
                var transform = new SuperButtonModelTransform(face, corner);
                var nativeMatrix = transform.transformation().getMatrix();
                for (int column = 0; column < 4; column++) {
                    for (int row = 0; row < 4; row++) {
                        assertEquals(Math.rint(nativeMatrix.get(column, row)), nativeMatrix.get(column, row), 0);
                    }
                }
                var matrix = new Mat4(transform.transformation().getMatrix());
                var normal = Vec3.yAxis().transform(matrix);
                assertEquals(face.getStepX(), normal.x, .00001);
                assertEquals(face.getStepY(), normal.y, .00001);
                assertEquals(face.getStepZ(), normal.z, .00001);
                assertEquals(1, Vec3.xAxis().transform(matrix).magnitude(), .00001);
                assertEquals(1, Vec3.zAxis().transform(matrix).magnitude(), .00001);
            }
        }
    }
}
