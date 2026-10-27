package net.portalmod.client.render;

import static org.junit.Assert.*;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class PortalQuadRendererTest {
    @Test
    public void uniformBlockMatchesShaderLayoutAndProjectionFollowsModelView() {
        var bytes = ByteBuffer.allocateDirect(PortalQuadRenderer.UNIFORM_BYTES).order(ByteOrder.nativeOrder());
        var model = new Matrix4f().translation(2, 3, -4);
        var projection = new Matrix4f().scaling(.5f, 2, -.25f);
        PortalQuadRenderer.writeUniforms(
                bytes, model, projection, new Vector4f(1, 2, 3, 4), 2, 0, 8, new Vector4f(.1f, .2f, .3f, 1));
        var point = new Matrix4f().set(0, bytes).transformPosition(new Vector3f(1, 2, 0));
        assertEquals(1.5, point.x, 0);
        assertEquals(10, point.y, 0);
        assertEquals(1, point.z, 0);
        var local = new Matrix4f().set(64, bytes).transformPosition(new Vector3f(1, 2, 0));
        assertEquals(new Vector3f(3, 5, -4), local);
        assertEquals(1, bytes.getFloat(128), 0);
        assertEquals(4, bytes.getFloat(140), 0);
        assertEquals(2, bytes.getFloat(144), 0);
        assertEquals(1, bytes.getFloat(148), 0);
        assertEquals(8, bytes.getFloat(152), 0);
        assertEquals(.1f, bytes.getFloat(160), 0);
        assertEquals(1, bytes.getFloat(172), 0);
        assertEquals(1, bytes.getFloat(176), 0);
        assertEquals(1, bytes.getFloat(180), 0);
        assertEquals(0, bytes.getFloat(184), 0);
        assertEquals(0, bytes.getFloat(188), 0);
        assertEquals(0, bytes.position());
    }
}
