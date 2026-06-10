package net.portalmod.fabric.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.portalmod.fabric.client.render.PortalDuplicateRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Extracts and submits plane-clipped duplicates of entities straddling a portal, so they
 * render on both sides while passing through (Forge {@code DuplicateEntityRenderer} parity).
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererDuplicateMixin {
    @Shadow
    @Final
    private EntityRenderDispatcher entityRenderDispatcher;

    @Shadow
    private ClientLevel level;

    @Inject(method = "extractVisibleEntities", at = @At("TAIL"))
    private void portalmod$extractDuplicates(Camera camera, Frustum frustum, DeltaTracker deltaTracker, LevelRenderState output, CallbackInfo callback) {
        PortalDuplicateRenderer.extract(this.level, this.entityRenderDispatcher, camera, deltaTracker);
    }

    @Inject(method = "submitEntities", at = @At("TAIL"))
    private void portalmod$submitDuplicates(PoseStack poseStack, LevelRenderState levelRenderState, SubmitNodeCollector output, CallbackInfo callback) {
        PortalDuplicateRenderer.submit(this.entityRenderDispatcher, poseStack, levelRenderState, output);
    }
}
