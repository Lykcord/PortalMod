package net.portalmod.common.sorted.button;

import com.mojang.math.Transformation;

import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.portalmod.core.math.Mat4;

import org.joml.Vector3f;

public class SuperButtonModelTransform implements ModelState {
    private final Mat4 matrix;

    public SuperButtonModelTransform(Direction facing, QuadBlockCorner corner) {
        this.matrix = this.getMatrix(facing, corner);
    }

    private Mat4 getMatrix(Direction facing, QuadBlockCorner corner) {
        Mat4 matrix = Mat4.identity();
        Axis axis = facing.getAxis();

        if (facing.getAxisDirection() == AxisDirection.NEGATIVE) {
            if (facing.getAxis() == Axis.Y) {
                matrix.rotateDeg(new Vector3f(0, 0, 1), 180);
            } else {
                matrix.rotateDeg(new Vector3f(0, 1, 0), 180);
            }
        }

        int axisFactor = axis == Axis.X ? 0 : -90;
        if (axis == Axis.X) {
            matrix.rotateDeg(new Vector3f(0, 0, -1), 90);
        }
        if (axis == Axis.Z) {
            matrix.rotateDeg(new Vector3f(1, 0, 0), 90);
        }

        return matrix.rotateDeg(new Vector3f(0, 1, 0), corner.getRot() + axisFactor);
    }

    @Override
    public Transformation transformation() {
        var transform = matrix.to4f();
        // Native face winding requires exact cardinal rotations, without quaternion rounding error.
        for (int column = 0; column < 4; column++) {
            for (int row = 0; row < 4; row++) {
                transform.set(column, row, Math.round(transform.get(column, row)));
            }
        }
        return new Transformation(transform);
    }
}
