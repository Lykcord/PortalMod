package net.portalmod.client.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.world.phys.Vec3;
import net.portalmod.client.render.PortalSceneRenderer;
import net.portalmod.common.sorted.portal.ITeleportable2;

import java.lang.ref.WeakReference;

/** Optional renderer integrations are kept out of the vanilla renderer's class-loading path. */
public final class PortalRendererCompatibility {
    private static final ThreadLocal<Integer> EXTRACTION_DEPTH = ThreadLocal.withInitial(() -> 0);
    private static final boolean SODIUM = FabricLoader.getInstance().isModLoaded("sodium");
    private static boolean preparingTerrainView;
    private static WeakReference<Object> cameraLevel = new WeakReference<>(null);
    private static Vec3 previousCameraPosition;
    private static boolean rootCameraWarped;
    private static long refreshVisibilityUntil;

    private PortalRendererCompatibility() {}

    public static void beginExtraction() {
        EXTRACTION_DEPTH.set(EXTRACTION_DEPTH.get() + 1);
    }

    public static void endExtraction() {
        int depth = EXTRACTION_DEPTH.get() - 1;
        if (depth == 0) {
            EXTRACTION_DEPTH.remove();
        } else {
            EXTRACTION_DEPTH.set(depth);
        }
    }

    public static boolean isPortalView() {
        var client = Minecraft.getInstance();
        return client.isSameThread()
                && client.level != null
                && (EXTRACTION_DEPTH.get() > 0 || PortalSceneRenderer.activeTarget() != null || needsMainViewRefresh());
    }

    public static void onCameraUpdated(Camera camera, boolean warped) {
        var level = camera.entity() == null ? null : camera.entity().level();
        long nowNanos = System.nanoTime();
        if (cameraLevel.get() != level) {
            cameraLevel = new WeakReference<>(level);
            previousCameraPosition = null;
            rootCameraWarped = false;
            refreshVisibilityUntil = 0;
        }
        boolean entityCrossedPortal =
                camera.entity() instanceof ITeleportable2 portable && portable.hasJustUsedPortal();
        if (warped
                || rootCameraWarped != warped
                || entityCrossedPortal
                || previousCameraPosition != null && previousCameraPosition.distanceToSqr(camera.position()) > 1) {
            refreshVisibilityUntil = nowNanos + 150_000_000L;
        }
        rootCameraWarped = warped;
        previousCameraPosition = camera.position();
    }

    public static boolean needsMainViewRefresh() {
        return rootCameraWarped || System.nanoTime() < refreshVisibilityUntil;
    }

    public static void prepareTerrain(Camera camera, LevelRenderState state, boolean portal) {
        if (!SODIUM) {
            return;
        }
        boolean wasPreparingTerrain = preparingTerrainView;
        preparingTerrainView = true;
        try {
            SodiumPortalBridge.prepareTerrain(camera, state, portal);
        } finally {
            preparingTerrainView = wasPreparingTerrain;
        }
    }

    public static boolean isPreparingTerrainView() {
        return preparingTerrainView;
    }
}
