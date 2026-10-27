package net.portalmod.common.sorted.turret;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.portalmod.PortalMod;
import net.portalmod.common.entities.TestElementEntityRenderer;
import net.portalmod.core.math.Mat4;

import org.joml.Quaternionf;

public class TurretRenderer extends TestElementEntityRenderer<TurretEntity, TurretRenderState, TurretModel> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(PortalMod.MODID, "textures/entity/turret/turret.png");
    private static final Identifier DEAD_TEXTURE =
            Identifier.fromNamespaceAndPath(PortalMod.MODID, "textures/entity/turret/turret_off.png");
    private static final RenderType LASER =
            RenderTypes.entityTranslucentEmissive(
                    Identifier.fromNamespaceAndPath(PortalMod.MODID, "textures/entity/turret/laser.png"));

    public TurretRenderer(EntityRendererProvider.Context context) {
        super(context, new TurretModel(), .5F);
        addLayer(new TurretEyeLayer(this));
    }

    @Override
    public TurretRenderState createRenderState() {
        return new TurretRenderState();
    }

    @Override
    public Identifier getTextureLocation(TurretRenderState state) {
        return state.turretState == TurretState.DEAD ? DEAD_TEXTURE : TEXTURE;
    }

    @Override
    public void extractRenderState(TurretEntity turret, TurretRenderState state, float partialTick) {
        super.extractRenderState(turret, state, partialTick);
        state.turretState = turret.getState();
        state.entityYaw = turret.getYRot();
        state.turretToTarget = turret.turretToTarget;
        state.recoil =
                state.turretState == TurretState.SHOOTING && turret.shouldShoot()
                        ? ((turret.tickCount + turret.getId()) % 2 + partialTick) * .1F - .07F
                        : 0;
        float fallTick = Math.min(turret.getAnimationTicks() + partialTick, turret.fallDuration);
        state.tipDirection = turret.getTipDirectionRight() ? 1 : -1;
        state.fallAmount =
                (state.turretState == TurretState.DEAD
                                ? 90
                                : 90 * fallTick * fallTick / (turret.fallDuration * turret.fallDuration))
                        * state.tipDirection;
        state.fallProgress = state.turretState == TurretState.FALLING ? fallTick / turret.fallDuration : 1;
        state.renderLaser = !state.fizzling && state.turretState.isStanding() && turret.getHurtTime() == 0;
        if (state.fizzling) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        float rotation = -Mth.lerp(partialTick, turret.yBodyRotO, turret.yBodyRot) * Mth.DEG_TO_RAD;
        float x = Mth.sin(rotation), z = Mth.cos(rotation);
        state.localEye = new Vec3(x * 2.5 / 16, turret.getEyeHeight(), z * 2.5 / 16);
        Vec3 eye = turret.getPosition(partialTick).add(state.localEye);
        Vec3 target =
                turret.hasTarget() && turret.shouldLaserMove()
                        ? turret.targetEntity.getPosition(partialTick).add(0, turret.targetEntity.getBbHeight() * .5, 0)
                        : eye.add(new Vec3(x, 0, z).scale(5));
        if (turret.lastLaserPos == Vec3.ZERO) {
            turret.lastLaserPos = target;
        }
        if (turret.shouldLaserEase()) {
            double delta = client.getDeltaTracker().getGameTimeDeltaTicks();
            target = turret.lastLaserPos.add(target.subtract(turret.lastLaserPos).scale(1 - Math.exp(-.5 * delta)));
        }
        state.turretToTarget = target.subtract(eye);
        turret.turretToTarget = state.turretToTarget;
        net.portalmod.core.math.Vec3 forward = new net.portalmod.core.math.Vec3(state.turretToTarget).normalize();
        net.portalmod.core.math.Vec3 toCamera =
                new net.portalmod.core.math.Vec3(client.gameRenderer.mainCamera().position()).sub(eye).normalize();
        net.portalmod.core.math.Vec3 up = toCamera.clone().sub(forward.clone().mul(toCamera.dot(forward))).normalize();
        net.portalmod.core.math.Vec3 right = up.clone().cross(forward).normalize();
        state.laserBasis.set(
                new Mat4(
                                right.x, up.x, forward.x, 0, right.y, up.y, forward.y, 0, right.z, up.z, forward.z, 0,
                                0, 0, 0, 1)
                        .to4f());
        Vec3 end = eye.add(state.turretToTarget.scale(client.options.getEffectiveRenderDistance() * 2));
        state.laserLength = (float) turret.traceAsFarAsPossible(eye, end).getSecond().subtract(eye).length();
        state.laserOpacity = 1 - ((float) Math.sin(System.currentTimeMillis() / 1000.0) + 1) / 2 * .3F;
        turret.lastLaserPos = target;
    }

    @Override
    public void submit(
            TurretRenderState state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
        poses.pushPose();
        if (!state.turretState.isStanding()) {
            var look = state.horizontalLook;
            float tip = state.tipDirection;
            poses.translate(look.z * .8F * tip * state.fallProgress, 0, -look.x * .8F * tip * state.fallProgress);
            poses.translate(-look.z * .2F * tip, 0, look.x * .2F * tip);
            if (look.lengthSqr() > 0) {
                poses.rotate(
                        new Quaternionf()
                                .rotationAxis(state.fallAmount * Mth.DEG_TO_RAD, (float) look.x, 0, (float) look.z));
            }
            poses.translate(look.z * .2F * tip, 0, -look.x * .2F * tip);
        }
        super.submit(state, poses, collector, camera);
        poses.popPose();
        if (!state.renderLaser) {
            return;
        }

        poses.pushPose();
        poses.translate(state.localEye);
        poses.mulPose(state.laserBasis);
        float length = state.laserLength, opacity = state.laserOpacity;
        collector.submitCustomGeometry(
                poses,
                LASER,
                (pose, vertices) -> {
                    vertices.addVertex(pose, 1 / 32F, 0, 0)
                            .setUv(0, 0)
                            .setColor(1, 1, 1, opacity)
                            .setOverlay(OverlayTexture.NO_OVERLAY)
                            .setLight(LightCoordsUtil.FULL_BRIGHT)
                            .setNormal(pose, 0, 1, 0);
                    vertices.addVertex(pose, -1 / 32F, 0, 0)
                            .setUv(0, 1)
                            .setColor(1, 1, 1, opacity)
                            .setOverlay(OverlayTexture.NO_OVERLAY)
                            .setLight(LightCoordsUtil.FULL_BRIGHT)
                            .setNormal(pose, 0, 1, 0);
                    vertices.addVertex(pose, -1 / 32F, 0, length)
                            .setUv(length / 4, 1)
                            .setColor(1, 1, 1, opacity)
                            .setOverlay(OverlayTexture.NO_OVERLAY)
                            .setLight(LightCoordsUtil.FULL_BRIGHT)
                            .setNormal(pose, 0, 1, 0);
                    vertices.addVertex(pose, 1 / 32F, 0, length)
                            .setUv(length / 4, 0)
                            .setColor(1, 1, 1, opacity)
                            .setOverlay(OverlayTexture.NO_OVERLAY)
                            .setLight(LightCoordsUtil.FULL_BRIGHT)
                            .setNormal(pose, 0, 1, 0);
                });
        poses.popPose();
    }

    @Override
    public boolean shouldRender(TurretEntity turret, Frustum frustum, double x, double y, double z, float partialTick) {
        return true;
    }
}
