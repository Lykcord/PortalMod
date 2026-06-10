package net.portalmod.fabric.mixin.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.portalmod.fabric.client.render.PortalWorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Renders all portal views into offscreen framebuffers before the vanilla frame is extracted,
 * so the per-frame level render state is free for the main camera afterwards.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererPortalViewMixin {
    @Inject(method = "extract", at = @At("HEAD"))
    private void portalmod$renderPortalViews(DeltaTracker deltaTracker, boolean advanceGameTime, CallbackInfo callback) {
        PortalWorldRenderer.renderPortalViews((GameRenderer) (Object) this, deltaTracker, advanceGameTime);
    }
}
