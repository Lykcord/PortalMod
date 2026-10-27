package net.portalmod;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.block.Blocks;
import net.portalmod.client.compat.PortalRendererCompatibility;
import net.portalmod.core.config.PortalModConfigManager;
import net.portalmod.mixins.accessors.ActiveRenderInfoAccessor;
import net.portalmod.mixins.accessors.LevelExtractorAccessor;

import org.joml.Matrix4f;

public final class FirstPersonPoseClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        try (var world =
                context.worldBuilder()
                        .adjustSettings(
                                settings -> settings.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE))
                        .create()) {
            world.getServer()
                    .runOnServer(
                            server -> {
                                var player = world.getConnection().getServerPlayer();
                                for (int x = -4; x <= 4; x++) {
                                    for (int z = -4; z <= 6; z++) {
                                        player.level()
                                                .setBlock(new BlockPos(x, 99, z), Blocks.STONE.defaultBlockState(), 2);
                                    }
                                }
                                player.connection.teleport(.5, 100, 4, 180, 0);
                            });
            world.getConnection().waitForClientboundPackets();
            context.waitFor(client -> client.player.getY() > 99);
            world.getConnection().waitForChunksRender();
            context.waitTicks(10);
            context.runOnClient(
                    client -> {
                        var player = client.player;
                        var previousPose = player.getPose();
                        var previousCameraType = client.options.getCameraType();
                        boolean renderSelf = PortalModConfigManager.RENDER_SELF.get();
                        try {
                            client.options.setCameraType(CameraType.FIRST_PERSON);
                            PortalModConfigManager.RENDER_SELF.set(true);
                            for (Pose pose : new Pose[] {Pose.CROUCHING, Pose.SWIMMING}) {
                                player.setPose(Pose.STANDING);
                                var camera = new Camera();
                                camera.setLevel(client.level);
                                camera.setEntity(player);
                                var access = (ActiveRenderInfoAccessor) camera;
                                for (int tick = 0; tick < 24; tick++) {
                                    camera.tick();
                                }
                                player.setPose(pose);
                                camera.tick();
                                for (int sample = 0; sample <= 4; sample++) {
                                    float partialTick = sample / 4F;
                                    float eyeHeight =
                                            Mth.lerp(partialTick, access.pmGetEyeHeightOld(), access.pmGetEyeHeight());
                                    access.pmSetPosition(player.getPosition(partialTick).add(0, eyeHeight, 0));
                                    access.pmSetRotation(180, 0);
                                    access.pmSetInitialized(true);
                                    access.pmSetFov(70);
                                    access.pmSetDepthFar(512);
                                    access.pmSetHudFov(70);
                                    access.pmSetupPerspective(
                                            Camera.PROJECTION_Z_NEAR,
                                            512,
                                            70,
                                            client.getWindow().getWidth(),
                                            client.getWindow().getHeight());
                                    access.pmPrepareCullFrustum(
                                            camera.getViewRotationMatrix(new Matrix4f()),
                                            access.pmProjectionForCulling(),
                                            camera.position());
                                    var delta = new FrameDelta(partialTick);
                                    var output = new LevelRenderState();
                                    camera.extractRenderState(output.cameraRenderState, delta);
                                    var dispatcher = client.levelRenderer.entityRenderDispatcher();
                                    try {
                                        PortalRendererCompatibility.onCameraUpdated(camera, false);
                                        dispatcher.prepare(camera, client.crosshairPickEntity);
                                        ((LevelExtractorAccessor) client.levelExtractor)
                                                .pmExtractEntities(camera, camera.getCullFrustum(), delta, output);
                                        if (output.entityRenderStates.stream()
                                                .anyMatch(state -> state instanceof AvatarRenderState)) {
                                            throw new AssertionError(
                                                    "First-person body appeared during "
                                                            + pose
                                                            + " at partial tick "
                                                            + partialTick
                                                            + ", camera="
                                                            + camera.position()
                                                            + ", bounds="
                                                            + player.getBoundingBox());
                                        }
                                    } finally {
                                        dispatcher.prepare(
                                                client.gameRenderer.mainCamera(), client.crosshairPickEntity);
                                    }
                                }
                            }
                        } finally {
                            player.setPose(previousPose);
                            client.options.setCameraType(previousCameraType);
                            PortalModConfigManager.RENDER_SELF.set(renderSelf);
                            PortalRendererCompatibility.onCameraUpdated(client.gameRenderer.mainCamera(), false);
                        }
                    });
            for (int cycle = 0; cycle < 3; cycle++) {
                context.getInput().holdKey(options -> options.keyShift);
                try {
                    for (int tick = 0; tick < 8; tick++) {
                        context.waitTicks(1);
                        context.runOnClient(FirstPersonPoseClientTest::checkMainView);
                    }
                } finally {
                    context.getInput().releaseKey(options -> options.keyShift);
                }
                for (int tick = 0; tick < 8; tick++) {
                    context.waitTicks(1);
                    context.runOnClient(FirstPersonPoseClientTest::checkMainView);
                }
            }
        }
    }

    private static void checkMainView(Minecraft client) {
        if (client.gameRenderer.gameRenderState().levelRenderState.entityRenderStates.stream()
                .anyMatch(state -> state instanceof AvatarRenderState)) {
            throw new AssertionError("Sneaking rendered the local player in the first-person main view");
        }
    }

    private record FrameDelta(float partialTick) implements DeltaTracker {
        @Override
        public float getGameTimeDeltaTicks() {
            return 0;
        }

        @Override
        public float getGameTimeDeltaPartialTick(boolean ignoreFrozenGame) {
            return partialTick;
        }

        @Override
        public float getRealtimeDeltaTicks() {
            return 0;
        }
    }
}
