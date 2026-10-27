package net.portalmod.common.entities;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.phys.Vec3;

public class TestElementRenderState extends LivingEntityRenderState {
    public float hurtTime;
    public float damage;
    public float hurtDirection;
    public float wiggle;
    public boolean fizzling;
    public Vec3 horizontalLook = Vec3.ZERO;
}
