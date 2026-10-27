package net.portalmod.mixins.renderer;

import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.world.entity.LivingEntity;
import net.portalmod.common.entities.TestElementEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidMobRenderer.class)
public class HumanoidMobRendererMixin {
    @Inject(method = "extractHumanoidRenderState", at = @At("RETURN"))
    private static void pmHeldEntityPose(
            LivingEntity entity,
            HumanoidRenderState state,
            float partialTicks,
            ItemModelResolver resolver,
            CallbackInfo callback) {
        if (TestElementEntity.isHoldable(entity)) {
            state.isPassenger = false;
        }
    }
}
