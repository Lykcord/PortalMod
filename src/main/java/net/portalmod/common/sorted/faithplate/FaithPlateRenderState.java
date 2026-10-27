package net.portalmod.common.sorted.faithplate;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class FaithPlateRenderState extends BlockEntityRenderState {
    public boolean onWall;
    public boolean active;
    public float yaw;
    public long animationMillis = -1;
    public AABB trigger;
    public List<Vec3> path = List.of();
    public Target target;

    public record Target(Vec3 position, Direction face, int light, float offset) {}
}
