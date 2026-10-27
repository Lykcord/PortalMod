package net.portalmod;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.locale.Language;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.PositionPath;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.portalmod.client.render.PortalCamera;
import net.portalmod.client.render.PortalSceneExtractor;
import net.portalmod.common.sorted.portal.ClientPortalManager;
import net.portalmod.common.sorted.portal.PortalEnd;
import net.portalmod.common.sorted.portal.PortalEntity;
import net.portalmod.common.sorted.portal.PortalPlacer;
import net.portalmod.core.init.BlockInit;
import net.portalmod.core.init.EntityInit;
import net.portalmod.core.init.ItemInit;
import net.portalmod.core.interfaces.ITeleportLerpable;

import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.io.IOException;
import java.util.UUID;

import javax.imageio.ImageIO;

public final class PortalEntityVisualClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
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
                                        for (int x = -5; x <= 5; x++) {
                                            for (int z = -5; z <= 5; z++) {
                                                player.level()
                                                        .setBlock(
                                                                new BlockPos(x, 99, z),
                                                                BlockInit.LUNECAST.get().defaultBlockState(),
                                                                2);
                                            }
                                        }
                                        var cube =
                                                EntityInit.STORAGE_CUBE
                                                        .get()
                                                        .create(player.level(), EntitySpawnReason.COMMAND);
                                        cube.snapTo(.5, 100, 1);
                                        player.level().addFreshEntity(cube);
                                        player.connection.teleport(.5, 100, 4, 180, 12);
                                        return cube.getId();
                                    });
            world.getConnection().waitForClientboundPackets();
            context.waitFor(
                    client -> client.level.getEntity(cubeId) != null && client.level.getEntity(cubeId).tickCount > 2);
            world.getConnection().waitForChunksRender();
            context.runOnClient(
                    client -> {
                        verifyReplay(client);
                        var camera =
                                new PortalCamera(
                                        client.level,
                                        client.player,
                                        new net.portalmod.core.math.Vec3(client.player.getEyePosition(1)),
                                        0,
                                        180,
                                        0,
                                        1);
                        var root = client.gameRenderer.gameRenderState().levelRenderState;
                        var scene =
                                PortalSceneExtractor.extract(
                                        client.level, camera, camera, client.getDeltaTracker(), root, 1, false);
                        if (scene.state().entityRenderStates.stream()
                                .anyMatch(state -> state instanceof AvatarRenderState)) {
                            throw new AssertionError("A portal camera inside its own head must not render that head");
                        }
                        var missing =
                                ItemInit.ITEMS.getRegistry().entrySet().stream()
                                        .filter(
                                                entry ->
                                                        !Language.getInstance()
                                                                .has(entry.getValue().get().getDescriptionId()))
                                        .map(entry -> entry.getValue().get().getDescriptionId())
                                        .toList();
                        if (!missing.isEmpty()) {
                            throw new AssertionError("Untranslated item labels: " + missing);
                        }
                        var cube = client.level.getEntity(cubeId);
                        var dispatcher = client.levelRenderer.entityRenderDispatcher();
                        var state = dispatcher.extractEntity(cube, 1);
                        if (state.outlineColor != 0) {
                            throw new AssertionError("Ordinary cube unexpectedly glows");
                        }
                        var submits = new SubmitNodeStorage();
                        dispatcher.submit(
                                state,
                                client.gameRenderer.gameRenderState().levelRenderState.cameraRenderState,
                                0,
                                0,
                                0,
                                new PoseStack(),
                                submits);
                        for (var group : submits.getSubmitsPerOrder().values()) {
                            group.outline.sortInto(
                                    (node, outline) -> {
                                        throw new AssertionError(
                                                "Cube glow layer submitted an unwanted glowing outline");
                                    });
                        }
                    });
            context.waitTicks(5);
            context.takeScreenshot("cube-no-unwanted-outline");
            UUID gun = UUID.randomUUID();
            world.getServer()
                    .runOnServer(
                            server -> {
                                var player = world.getConnection().getServerPlayer();
                                var level = player.level();
                                for (int y = 99; y <= 104; y++) {
                                    for (int side = -2; side <= 6; side++) {
                                        level.setBlock(
                                                new BlockPos(side, y, 0),
                                                BlockInit.LUNECAST.get().defaultBlockState(),
                                                2);
                                        level.setBlock(
                                                new BlockPos(30, y, side),
                                                BlockInit.LUNECAST.get().defaultBlockState(),
                                                2);
                                        level.setBlock(
                                                new BlockPos(22, y, side),
                                                Blocks.CONCRETE.cyan().defaultBlockState(),
                                                2);
                                    }
                                }
                                for (int x = 22; x <= 34; x++) {
                                    for (int z = -2; z <= 8; z++) {
                                        level.setBlock(
                                                new BlockPos(x, 99, z),
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
                                                new net.portalmod.core.math.Vec3(.5, 101, 1),
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
                                                new net.portalmod.core.math.Vec3(30, 101, 3.5),
                                                Direction.WEST,
                                                Direction.UP,
                                                false,
                                                null,
                                                null);
                                if (entry == null || exit == null) {
                                    throw new AssertionError("Entity view portals failed to place");
                                }
                                level.getEntity(cubeId).snapTo(27, 100, 3.5);
                                player.connection.teleport(.5, 100, 4, 180, 0);
                            });
            world.getConnection().waitForClientboundPackets();
            context.waitFor(
                    client ->
                            ClientPortalManager.getInstance().has(gun, PortalEnd.PRIMARY)
                                    && ClientPortalManager.getInstance().has(gun, PortalEnd.SECONDARY));
            world.getConnection().waitForChunksRender();
            context.waitTicks(25);
            assertCubePixels(context, cubeId, gun, "cube-through-portal");
            world.getServer()
                    .runOnServer(
                            server -> {
                                var cube = world.getConnection().getServerPlayer().level().getEntity(cubeId);
                                cube.snapTo(29.9, 100, 3.5);
                            });
            world.getConnection().waitForClientboundPackets();
            context.waitTicks(15);
            assertCubePixels(context, cubeId, gun, "cube-straddling-portal");
            world.getServer()
                    .runOnServer(
                            server -> {
                                var cube = world.getConnection().getServerPlayer().level().getEntity(cubeId);
                                cube.snapTo(29.6, 100, 3.5);
                                cube.setDeltaMovement(.4, 0, 0);
                            });
            context.waitFor(client -> client.level.getEntity(cubeId).getX() < 5, 100);
            world.getConnection().waitForClientboundPackets();
            context.waitTicks(5);
            var position = context.computeOnClient(client -> client.level.getEntity(cubeId).position());
            world.getServer()
                    .runOnServer(
                            server -> {
                                var cube = world.getConnection().getServerPlayer().level().getEntity(cubeId);
                                if (cube.position().distanceTo(position) > .5) {
                                    throw new AssertionError(
                                            "Cube teleport replay diverged from the server: "
                                                    + position
                                                    + " vs "
                                                    + cube.position());
                                }
                            });
            context.takeScreenshot("cube-after-portal-traversal");
        }
    }

    private static void verifyReplay(Minecraft client) {
        var remote = new RemotePlayer(client.level, new GameProfile(UUID.randomUUID(), "PortalDummy"));
        var cube = EntityInit.STORAGE_CUBE.get().create(client.level, EntitySpawnReason.COMMAND);
        for (var entity : new Entity[] {remote, cube}) {
            entity.snapTo(10, 100, 0);
            entity.setNoGravity(true);
            var history = ((ITeleportLerpable) entity).getLerpPositions();
            history.add(Pair.of(new Vec3(10, 100, 0), new Vec3(10.1, 100, 0)));
            history.add(Pair.of(new Vec3(10.1, 100, 0), new Vec3(10.2, 100, 0)));
            history.add(Pair.of(new Vec3(10.2, 100, 0), new Vec3(10.3, 100, 0)));
            entity.commonTick();
            entity.tick();
            if (history.size() != 2
                    || Math.abs(entity.getX() - 10.1) > .00001
                    || Math.abs(entity.getPosition(.5F).x - 10.05) > .00001) {
                throw new AssertionError(
                        "Portal replay must consume one sample and preserve smooth render interpolation for "
                                + entity.getClass().getSimpleName());
            }
        }
        ((ITeleportLerpable) cube).getLerpPositions().clear();
        remote.snapTo(10, 100, 0);
        cube.snapTo(10, 101, -1.5);
        if (!cube.startRiding(remote)) {
            throw new AssertionError("Cannot exercise remote held-cube interpolation");
        }
        cube.moveOrInterpolateTo(PositionPath.of(new Vec3(10.3, 101, -1.5)), 90, 0);
        cube.commonTick();
        var interpolated = cube.position();
        cube.rideTick();
        if (!cube.isInterpolating() || cube.position().distanceTo(interpolated) > .00001) {
            throw new AssertionError(
                    "Remote held cubes must keep native server interpolation instead of running another client's grab prediction");
        }
        cube.stopRiding();
    }

    private static void assertCubePixels(ClientGameTestContext context, int cubeId, UUID gun, String name) {
        var bounds =
                context.computeOnClient(
                        client -> {
                            var entry = ClientPortalManager.getInstance().get(gun, PortalEnd.PRIMARY);
                            var exit = ClientPortalManager.getInstance().get(gun, PortalEnd.SECONDARY);
                            var transform = PortalEntity.getPortalToPortalMatrix(exit, entry);
                            var state = client.gameRenderer.gameRenderState().levelRenderState.cameraRenderState;
                            var projection = new Matrix4f(state.projectionMatrix).mul(state.viewRotationMatrix);
                            var cube = client.level.getEntity(cubeId);
                            var result =
                                    new float[] {
                                        Float.POSITIVE_INFINITY,
                                        Float.POSITIVE_INFINITY,
                                        Float.NEGATIVE_INFINITY,
                                        Float.NEGATIVE_INFINITY
                                    };
                            for (double x : new double[] {cube.getX() - .3, cube.getX() + .3}) {
                                for (double y : new double[] {cube.getY() + .1, cube.getY() + .7}) {
                                    for (double z : new double[] {cube.getZ() - .3, cube.getZ() + .3}) {
                                        var point =
                                                new net.portalmod.core.math.Vec3(x, y, z)
                                                        .transform(transform)
                                                        .sub(state.pos);
                                        var screen =
                                                projection.transform(
                                                        new Vector4f(
                                                                (float) point.x, (float) point.y, (float) point.z, 1));
                                        result[0] = Math.min(result[0], screen.x / screen.w);
                                        result[1] = Math.min(result[1], screen.y / screen.w);
                                        result[2] = Math.max(result[2], screen.x / screen.w);
                                        result[3] = Math.max(result[3], screen.y / screen.w);
                                    }
                                }
                            }
                            return result;
                        });
        try {
            var image = ImageIO.read(context.takeScreenshot(name).toFile());
            int left = Math.max(0, (int) ((bounds[0] + 1) * image.getWidth() / 2));
            int right = Math.min(image.getWidth() - 1, (int) ((bounds[2] + 1) * image.getWidth() / 2));
            int top = Math.max(0, (int) ((1 - bounds[3]) * image.getHeight() / 2));
            int bottom = Math.min(image.getHeight() - 1, (int) ((1 - bounds[1]) * image.getHeight() / 2));
            int cubePixels = 0;
            for (int y = top; y <= bottom; y++) {
                for (int x = left; x <= right; x++) {
                    int color = image.getRGB(x, y), r = color >> 16 & 255, g = color >> 8 & 255, b = color & 255;
                    if (r > 40 && g > 40 && b > 40 && r > g * .8) {
                        cubePixels++;
                    }
                }
            }
            if (cubePixels < 50) {
                throw new AssertionError(
                        "Cube mesh is missing through the portal in " + name + ": pixels=" + cubePixels);
            }
        } catch (IOException error) {
            throw new AssertionError(error);
        }
    }
}
