package net.portalmod.mixins.renderer;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.portalmod.common.entities.TestElementEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntityRenderer.class)
public class LivingRendererMixin {
    // Held objects retain their portal-transformed yaw instead of inheriting the holder's torso yaw.
    @Redirect(
            method = "solveBodyRot",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/LivingEntity;getVehicle()Lnet/minecraft/world/entity/Entity;"))
    private static Entity pmHeldObjectRotation(LivingEntity entity) {
        return entity instanceof TestElementEntity element && element.isPickedUp() ? null : entity.getVehicle();
    }

    @Redirect(
            method =
                    "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isPassenger()Z"))
    private boolean pmShouldSit(LivingEntity entity) {
        return !(TestElementEntity.isHoldable(entity)) && entity.isPassenger();
    }
}
