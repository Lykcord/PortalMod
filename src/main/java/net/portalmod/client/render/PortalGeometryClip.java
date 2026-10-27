package net.portalmod.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.rendertype.RenderType;

import org.joml.Vector4fc;

public final class PortalGeometryClip implements AutoCloseable {
    private static final ThreadLocal<PortalGeometryClip> ACTIVE = new ThreadLocal<>();
    private final PortalGeometryClip previous;
    private final Vector4fc plane;
    private ClippedVertexConsumer consumer;

    public PortalGeometryClip(Vector4fc plane) {
        this.plane = plane;
        previous = ACTIVE.get();
        ACTIVE.set(this);
    }

    public static void flush() {
        var active = ACTIVE.get();
        if (active != null && active.consumer != null) {
            active.consumer.finish();
            active.consumer = null;
        }
    }

    public static VertexConsumer wrap(VertexConsumer original, RenderType type) {
        var active = ACTIVE.get();
        if (active == null) {
            return original;
        }
        active.consumer = new ClippedVertexConsumer(original, type.primitiveTopology(), active.plane);
        return active.consumer;
    }

    @Override
    public void close() {
        try {
            flush();
        } finally {
            if (previous == null) {
                ACTIVE.remove();
            } else {
                ACTIVE.set(previous);
            }
        }
    }
}
