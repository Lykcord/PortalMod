package net.portalmod.common.sorted.autoportal;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.world.phys.Vec3;

import org.joml.Matrix4f;

public class AutoPortalRenderState extends BlockEntityRenderState {
    public Matrix4f basis;
    public Vec3 offset;
    public int colorIndex;
    public boolean closed;
}
