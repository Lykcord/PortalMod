package net.portalmod.mixins.compat;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.portalmod.client.compat.PortalRendererCompatibility;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Main-camera occlusion decisions cannot be reused for a camera on the other side of a portal. */
@Mixin(
        value = {Entity.class, BlockEntity.class},
        priority = 900)
public abstract class EntityCullingVisibilityMixin {
    @Inject(
            method = {"isCulled", "isOutOfCamera"},
            at = @At("HEAD"),
            cancellable = true,
            remap = false)
    private void pmPortalCameraVisibility(CallbackInfoReturnable<Boolean> callback) {
        if (PortalRendererCompatibility.isPortalView()) {
            callback.setReturnValue(false);
        }
    }
}
