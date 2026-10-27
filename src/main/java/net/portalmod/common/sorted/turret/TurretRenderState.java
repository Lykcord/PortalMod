package net.portalmod.common.sorted.turret;

import net.minecraft.world.phys.Vec3;
import net.portalmod.common.entities.TestElementRenderState;

import org.joml.Matrix4f;

public final class TurretRenderState extends TestElementRenderState {
    public TurretState turretState = TurretState.RESTING;
    public float entityYaw;
    public float recoil;
    public float fallAmount;
    public float fallProgress;
    public int tipDirection;
    public Vec3 turretToTarget = Vec3.ZERO;
    public Vec3 localEye = Vec3.ZERO;
    public final Matrix4f laserBasis = new Matrix4f();
    public boolean renderLaser;
    public float laserLength;
    public float laserOpacity;
}
