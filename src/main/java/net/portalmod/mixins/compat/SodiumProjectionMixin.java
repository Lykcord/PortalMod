package net.portalmod.mixins.compat;

import net.minecraft.client.renderer.GameRenderer;
import net.portalmod.client.render.PortalSceneRenderer;

import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Sodium reads a separate matrix cache, so the GPU projection uniform alone is insufficient. */
@Mixin(value = GameRenderer.class, priority = 900)
public abstract class SodiumProjectionMixin {
    @Inject(method = "sodium$getProjectionMatrix", at = @At("HEAD"), cancellable = true, remap = false)
    private void pmPortalTerrainProjection(CallbackInfoReturnable<Matrix4fc> callback) {
        var projection = PortalSceneRenderer.activeProjection();
        if (projection != null) {
            callback.setReturnValue(projection);
        }
    }
}
