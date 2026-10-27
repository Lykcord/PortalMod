package net.portalmod;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntitySpawnReason;
import net.portalmod.client.compat.PortalRendererCompatibility;
import net.portalmod.common.sorted.portal.ClientPortalManager;
import net.portalmod.common.sorted.portal.PortalEnd;
import net.portalmod.common.sorted.portal.PortalPlacer;
import net.portalmod.core.init.BlockInit;
import net.portalmod.core.init.EntityInit;
import net.portalmod.core.math.Vec3;
import net.portalmod.mixins.accessors.ActiveRenderInfoAccessor;
import net.portalmod.mixins.accessors.LevelExtractorAccessor;

import org.joml.Matrix4f;

import java.util.UUID;

/** The visual camera can cross before the physical body while hovering around the plane. */
public final class PortalBoundaryVisibilityClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        UUID gun = UUID.randomUUID();
        try (var world =
                context.worldBuilder()
                        .adjustSettings(
                                settings -> settings.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE))
                        .create()) {
            int cubeId =
                    world.getServer()
                            .computeOnServer(
                                    server -> {
                                        var player = world.getConnection().getServerPlayer();
                                        var level = player.level();
                                        for (int x = -4; x <= 4; x++) {
                                            for (int z = -2; z <= 18; z++) {
                                                level.setBlock(
                                                        new BlockPos(x, 99, z),
                                                        BlockInit.LUNECAST.get().defaultBlockState(),
                                                        2);
                                            }
                                        }
                                        for (int x = -3; x <= 3; x++) {
                                            for (int y = 100; y <= 104; y++) {
                                                level.setBlock(
                                                        new BlockPos(x, y, 0),
                                                        BlockInit.LUNECAST.get().defaultBlockState(),
                                                        2);
                                                level.setBlock(
                                                        new BlockPos(x, y, 16),
                                                        BlockInit.LUNECAST.get().defaultBlockState(),
                                                        2);
                                            }
                                        }
                                        var entry =
                                                PortalPlacer.placePortal(
                                                        level,
                                                        PortalEnd.PRIMARY,
                                                        "blue",
                                                        gun,
                                                        new Vec3(.5, 101, 1),
                                                        Direction.SOUTH,
                                                        Direction.UP,
                                                        false,
                                                        null,
                                                        null);
                                        var exit =
                                                PortalPlacer.placePortal(
                                                        level,
                                                        PortalEnd.SECONDARY,
                                                        "orange",
                                                        gun,
                                                        new Vec3(.5, 101, 16),
                                                        Direction.NORTH,
                                                        Direction.UP,
                                                        false,
                                                        null,
                                                        null);
                                        if (entry == null || exit == null) {
                                            throw new AssertionError("Cannot place visibility handoff portals");
                                        }
                                        var cube =
                                                EntityInit.STORAGE_CUBE.get().create(level, EntitySpawnReason.COMMAND);
                                        cube.snapTo(.5, 100, 2);
                                        level.addFreshEntity(cube);
                                        player.connection.teleport(.5, 100, 1.02, 180, 0);
                                        return cube.getId();
                                    });
            world.getConnection().waitForClientboundPackets();
            context.waitFor(
                    client ->
                            client.player.getY() > 99
                                    && ClientPortalManager.getInstance().has(gun, PortalEnd.SECONDARY));
            world.getConnection().waitForChunksRender();
            context.waitTicks(15);
            context.runOnClient(
                    client -> {
                        var camera = new Camera();
                        camera.setLevel(client.level);
                        camera.setEntity(client.player);
                        var access = (ActiveRenderInfoAccessor) camera;
                        access.pmSetPosition(new net.minecraft.world.phys.Vec3(.5, client.player.getEyeY(), 15.98));
                        access.pmSetRotation(180, 0);
                        access.pmSetInitialized(true);
                        access.pmSetFov(client.gameRenderer.mainCamera().getFov());
                        access.pmSetDepthFar(512);
                        access.pmSetHudFov(70);
                        access.pmSetupPerspective(
                                Camera.PROJECTION_Z_NEAR,
                                512,
                                camera.getFov(),
                                client.getWindow().getWidth(),
                                client.getWindow().getHeight());
                        access.pmPrepareCullFrustum(
                                camera.getViewRotationMatrix(new Matrix4f()),
                                access.pmProjectionForCulling(),
                                camera.position());
                        if (camera.isDetached()) {
                            throw new AssertionError("Regression needs a first-person main camera");
                        }
                        var root = client.gameRenderer.gameRenderState().levelRenderState;
                        var output = new LevelRenderState();
                        camera.extractRenderState(output.cameraRenderState, client.getDeltaTracker());
                        var cube = client.level.getEntity(cubeId);
                        boolean culling = FabricLoader.getInstance().isModLoaded("entityculling");
                        try {
                            if (culling) {
                                try {
                                    cube.getClass().getMethod("setCulled", boolean.class).invoke(cube, true);
                                } catch (ReflectiveOperationException error) {
                                    throw new AssertionError(error);
                                }
                            }
                            PortalRendererCompatibility.onCameraUpdated(camera, true);
                            if (culling) {
                                try {
                                    if ((boolean) cube.getClass().getMethod("isCulled").invoke(cube)) {
                                        throw new AssertionError(
                                                "A stale source-camera cull flag must not hide a cube during the handoff");
                                    }
                                } catch (ReflectiveOperationException error) {
                                    throw new AssertionError(error);
                                }
                            }
                            client.levelRenderer.entityRenderDispatcher().prepare(camera, client.crosshairPickEntity);
                            PortalRendererCompatibility.prepareTerrain(camera, output, true);
                            ((LevelExtractorAccessor) client.levelExtractor)
                                    .pmExtractEntities(
                                            camera, camera.getCullFrustum(), client.getDeltaTracker(), output);
                            if (output.entityRenderStates.stream()
                                    .noneMatch(
                                            state ->
                                                    state
                                                            instanceof
                                                            net.minecraft.client.renderer.entity.state
                                                                    .AvatarRenderState)) {
                                throw new AssertionError(
                                        "Local body vanished when the visual camera crossed before the body");
                            }
                            if (output.entityRenderStates.stream()
                                    .noneMatch(state -> state.entityType == EntityInit.STORAGE_CUBE.get())) {
                                throw new AssertionError("Cube vanished when the main camera changed portal sides");
                            }
                            access.pmSetPosition(
                                    client.player.getEyePosition(
                                            camera.getCameraEntityPartialTicks(client.getDeltaTracker())));
                            access.pmPrepareCullFrustum(
                                    camera.getViewRotationMatrix(new Matrix4f()),
                                    access.pmProjectionForCulling(),
                                    camera.position());
                            var near = new LevelRenderState();
                            camera.extractRenderState(near.cameraRenderState, client.getDeltaTracker());
                            ((LevelExtractorAccessor) client.levelExtractor)
                                    .pmExtractEntities(camera, camera.getCullFrustum(), client.getDeltaTracker(), near);
                            if (near.entityRenderStates.stream()
                                    .anyMatch(
                                            state ->
                                                    state
                                                            instanceof
                                                            net.minecraft.client.renderer.entity.state
                                                                    .AvatarRenderState)) {
                                throw new AssertionError(
                                        "Restoring the body on the other side must not render the camera's own head");
                            }
                        } finally {
                            PortalRendererCompatibility.onCameraUpdated(client.gameRenderer.mainCamera(), false);
                            if (culling) {
                                try {
                                    cube.getClass().getMethod("setCulled", boolean.class).invoke(cube, false);
                                } catch (ReflectiveOperationException error) {
                                    throw new AssertionError(error);
                                }
                            }
                            client.levelRenderer
                                    .entityRenderDispatcher()
                                    .prepare(client.gameRenderer.mainCamera(), client.crosshairPickEntity);
                            PortalRendererCompatibility.prepareTerrain(client.gameRenderer.mainCamera(), root, false);
                        }
                    });
        }
    }
}
