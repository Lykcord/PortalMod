package net.portalmod.mixins.compat;

import net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager;
import net.portalmod.client.compat.PortalRendererCompatibility;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Async cull results can belong to the previous portal camera, including during main-view restoration. */
@Mixin(value = RenderSectionManager.class, remap = false)
public abstract class SodiumRenderListsMixin {
    @ModifyVariable(method = "finalizeRenderLists", at = @At("HEAD"), argsOnly = true)
    private boolean pmBuildCurrentViewLists(boolean immediate) {
        // Only select the visible list synchronously. Mesh compilation keeps its normal budget.
        return immediate
                || PortalRendererCompatibility.isPreparingTerrainView()
                || PortalRendererCompatibility.needsMainViewRefresh();
    }
}
