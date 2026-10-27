package net.portalmod.mixins.renderer;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.portalmod.client.compat.PortalRendererCompatibility;
import net.portalmod.client.render.PortalCamera;
import net.portalmod.common.sorted.portal.PortalEntity;
import net.portalmod.core.config.PortalModConfigManager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LevelExtractor.class)
public class LevelExtractorMixin {
    @WrapOperation(
            method = "extractVisibleEntities",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;isDetached()Z"))
    private boolean pmHideCameraInsideOwnBody(
            Camera camera, Operation<Boolean> original, @Local(argsOnly = true) DeltaTracker delta) {
        boolean detached = original.call(camera);
        if (camera.entity() == null) {
            return detached;
        }
        var entity = camera.entity();
        var offset = entity.getPosition(camera.getCameraEntityPartialTicks(delta)).subtract(entity.position());
        // A portal camera acts detached so it can see the player's distant body.
        // During a crossing that same camera can be inside its interpolated head.
        boolean insideBody =
                entity.getBoundingBox()
                        .move(offset)
                        .inflate(Camera.PROJECTION_Z_NEAR * 1.5)
                        .contains(camera.position());
        if (camera instanceof PortalCamera) {
            return detached && !insideBody;
        }
        // Only a portal handoff should reveal the local body in first person.
        return detached
                || PortalModConfigManager.RENDER_SELF.get()
                        && PortalRendererCompatibility.isMainCameraWarped()
                        && !insideBody;
    }

    @WrapOperation(
            method = "getViewBlockingState",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/block/state/BlockState;isViewBlocking(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/AABB;)Z"))
    private static boolean pmPortalBlockOverlay(
            BlockState state, BlockGetter view, BlockPos pos, AABB bounds, Operation<Boolean> original) {
        return original.call(state, view, pos, bounds)
                && (!(view instanceof Level level) || PortalEntity.shouldRenderBlockOverlay(level, pos));
    }
}
