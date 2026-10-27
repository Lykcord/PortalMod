package net.portalmod.client.compat;

import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.caffeinemc.mods.sodium.client.render.viewport.ViewportProvider;
import net.caffeinemc.mods.sodium.client.util.FlawlessFrames;
import net.caffeinemc.mods.sodium.client.util.FogParameters;
import net.caffeinemc.mods.sodium.mixin.core.render.world.FrustumAccessor;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.state.level.LevelRenderState;

/** Rebuilds Sodium's view-dependent render lists for each portal camera, then for the main view. */
final class SodiumPortalBridge {
    private SodiumPortalBridge() {}

    static void prepareTerrain(Camera camera, LevelRenderState state, boolean portal) {
        var renderer = SodiumWorldRenderer.instanceNullable();
        if (renderer == null) {
            return;
        }
        var frustum = camera.getCullFrustum();
        var fog = state.cameraRenderState.fogData;
        var parameters =
                new FogParameters(
                        fog.color.x,
                        fog.color.y,
                        fog.color.z,
                        fog.color.w,
                        fog.environmentalStart,
                        fog.environmentalEnd,
                        fog.renderDistanceStart,
                        fog.renderDistanceEnd);
        renderer.setupTerrain(
                camera,
                ((ViewportProvider) frustum).sodium$createViewport(),
                parameters,
                !portal && state.cameraRenderState.smartCull,
                FlawlessFrames.isActive(),
                ((FrustumAccessor) frustum).sodium$getMatrix());
    }
}
