package net.portalmod.common.entities;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LightLayer;

import org.joml.Quaternionf;

public abstract class TestElementEntityRenderer<
                T extends TestElementEntity, S extends TestElementRenderState, M extends EntityModel<? super S>>
        extends LivingEntityRenderer<T, S, M> {
    protected TestElementEntityRenderer(EntityRendererProvider.Context context, M model, float shadowRadius) {
        super(context, model, shadowRadius);
    }

    @Override
    public void extractRenderState(T entity, S state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.hurtTime = entity.getHurtTime() - partialTick;
        state.damage = Math.max(0, entity.getDamage() - partialTick);
        state.hurtDirection = entity.getHurtDir();
        state.wiggle = entity.getWiggle() - partialTick;
        state.horizontalLook = entity.getLookAngle().multiply(1, 0, 1).normalize();
        state.fizzling = entity.isFizzling();
        state.lightCoords =
                entity.getFizzleLight(
                        LightCoordsUtil.pack(
                                entity.level().getBrightness(LightLayer.BLOCK, entity.blockPosition()),
                                entity.level().getBrightness(LightLayer.SKY, entity.blockPosition())));
    }

    @Override
    public void submit(S state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
        poses.pushPose();
        if (state.horizontalLook.lengthSqr() > 0) {
            float angle = 0;
            if (state.hurtTime > 0) {
                angle += Mth.sin(state.hurtTime) * state.hurtTime * state.damage / 10 * state.hurtDirection;
            }
            if (state.wiggle > 0) {
                angle += Mth.sin(state.wiggle * 1.5F) * state.wiggle * .5F * state.hurtDirection;
            }
            poses.rotate(
                    new Quaternionf()
                            .rotationAxis(
                                    (float) Math.toRadians(angle),
                                    (float) state.horizontalLook.x,
                                    0,
                                    (float) state.horizontalLook.z));
        }
        super.submit(state, poses, collector, camera);
        poses.popPose();
    }

    @Override
    protected boolean shouldShowName(T entity, double distanceSq) {
        return super.shouldShowName(entity, distanceSq)
                && entity.hasCustomName()
                && Minecraft.getInstance().crosshairPickEntity == entity;
    }
}
