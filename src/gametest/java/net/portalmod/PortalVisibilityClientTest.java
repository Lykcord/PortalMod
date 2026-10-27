package net.portalmod;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.portalmod.client.render.PortalCamera;
import net.portalmod.client.render.PortalSceneExtractor;
import net.portalmod.common.sorted.portal.ClientPortalManager;
import net.portalmod.common.sorted.portal.PortalEnd;
import net.portalmod.common.sorted.portal.PortalEntity;
import net.portalmod.common.sorted.portal.PortalPlacer;
import net.portalmod.core.init.BlockInit;
import net.portalmod.core.init.EntityInit;
import net.portalmod.core.math.Vec3;

import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

import javax.imageio.ImageIO;

/** Checks pixels as well as render-state presence: a live surface can still show culled terrain. */
public final class PortalVisibilityClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        UUID gun = UUID.randomUUID();
        try (var world =
                context.worldBuilder()
                        .adjustSettings(
                                settings -> settings.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE))
                        .create()) {
            world.getServer()
                    .runOnServer(
                            server -> {
                                var player = world.getConnection().getServerPlayer();
                                var level = player.level();
                                for (int x = -4; x <= 4; x++) {
                                    for (int z = -2; z <= 6; z++) {
                                        level.setBlock(
                                                new BlockPos(x, 99, z),
                                                BlockInit.LUNECAST.get().defaultBlockState(),
                                                2);
                                    }
                                }
                                for (int y = 99; y <= 104; y++) {
                                    for (int side = -2; side <= 4; side++) {
                                        level.setBlock(
                                                new BlockPos(side, y, 0),
                                                BlockInit.LUNECAST.get().defaultBlockState(),
                                                2);
                                        level.setBlock(
                                                new BlockPos(40, y, side),
                                                BlockInit.LUNECAST.get().defaultBlockState(),
                                                2);
                                    }
                                }
                                for (int x : new int[] {-2, 2}) {
                                    for (int y = 100; y <= 104; y++) {
                                        level.setBlock(
                                                new BlockPos(x, y, 0), Blocks.CONCRETE.red().defaultBlockState(), 2);
                                    }
                                }
                                // This wall is loaded by the real camera, but over 60 blocks from the virtual camera.
                                for (int y = 90; y <= 115; y++) {
                                    for (int z = -12; z <= 18; z++) {
                                        level.setBlock(
                                                new BlockPos(-18, y, z), Blocks.CONCRETE.cyan().defaultBlockState(), 2);
                                    }
                                }
                                var entry =
                                        PortalPlacer.placePortal(
                                                level,
                                                PortalEnd.PRIMARY,
                                                "blue",
                                                gun,
                                                new Vec3(.5, 101.625, 1),
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
                                                new Vec3(40, 101.625, 3.5),
                                                Direction.WEST,
                                                Direction.UP,
                                                false,
                                                null,
                                                null);
                                if (entry == null || exit == null) {
                                    throw new AssertionError("Visibility test portals failed to place");
                                }
                                player.connection.teleport(.5, 100, 4, 180, 0);
                            });
            world.getConnection().waitForClientboundPackets();
            context.waitFor(
                    client ->
                            client.player != null
                                    && client.player.getY() > 99
                                    && ClientPortalManager.getInstance().has(gun, PortalEnd.PRIMARY)
                                    && ClientPortalManager.getInstance().has(gun, PortalEnd.SECONDARY));
            world.getConnection().waitForChunksRender();
            context.waitTicks(40);
            for (float yaw : new float[] {180, 140, 220}) {
                context.getInput().lookAt(yaw, 0);
                context.waitTicks(6);
                assertPixels(context, "portal-visibility-" + (int) yaw, false);
            }
            for (float[] angles :
                    new float[][] {
                        {125, 0},
                        {130, 0},
                        {230, 0},
                        {235, 0},
                        {180, -45},
                        {180, 45},
                        {140, -30},
                        {140, 30},
                        {220, -30},
                        {220, 30}
                    }) {
                context.getInput().lookAt(angles[0], angles[1]);
                context.waitTicks(3);
                assertVisibleEdge(context, "portal-edge-" + (int) angles[0] + "-" + (int) angles[1]);
            }
            context.getInput().lookAt(180, 0);
            world.getServer()
                    .runOnServer(
                            server -> {
                                var level = world.getConnection().getServerPlayer().level();
                                for (int y = 100; y <= 103; y++) {
                                    level.setBlock(
                                            new BlockPos(0, y, 2), Blocks.CONCRETE.green().defaultBlockState(), 3);
                                }
                            });
            world.getConnection().waitForClientboundPackets();
            world.getConnection().waitForChunksRender();
            context.waitTicks(10);
            try {
                var image = ImageIO.read(context.takeScreenshot("portal-occluded-by-wall").toFile());
                int green = 0;
                for (int y = image.getHeight() / 2 - 8; y <= image.getHeight() / 2 + 8; y++) {
                    for (int x = image.getWidth() / 2 - 8; x <= image.getWidth() / 2 + 8; x++) {
                        int rgb = image.getRGB(x, y), r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
                        if (g > 20 && g > r * 1.15 && g > b * 1.5) {
                            green++;
                        }
                    }
                }
                if (green < 200) {
                    throw new AssertionError("Portal surface bypassed ordinary terrain depth testing");
                }
            } catch (IOException error) {
                throw new AssertionError(error);
            }
            world.getServer()
                    .runOnServer(
                            server -> {
                                var level = world.getConnection().getServerPlayer().level();
                                for (int y = 100; y <= 103; y++) {
                                    level.setBlock(new BlockPos(0, y, 2), Blocks.AIR.defaultBlockState(), 3);
                                }
                                for (int y = 90; y <= 115; y++) {
                                    for (int z = -12; z <= 18; z++) {
                                        level.setBlock(new BlockPos(-18, y, z), Blocks.AIR.defaultBlockState(), 3);
                                    }
                                }
                            });
            context.getInput().lookAt(180, 0);
            world.getConnection().waitForClientboundPackets();
            context.waitTicks(40);
            assertPixels(context, "portal-sky-visible", true);
            int cubeId =
                    world.getServer()
                            .computeOnServer(
                                    server -> {
                                        var level = world.getConnection().getServerPlayer().level();
                                        var cube =
                                                EntityInit.STORAGE_CUBE.get().create(level, EntitySpawnReason.COMMAND);
                                        if (cube == null) {
                                            throw new AssertionError("Cannot create destination cube");
                                        }
                                        cube.snapTo(36.5, 100.5, 3.5);
                                        cube.setNoGravity(true);
                                        level.addFreshEntity(cube);
                                        return cube.getId();
                                    });
            context.waitFor(
                    client -> client.level.getEntity(cubeId) != null && client.level.getEntity(cubeId).tickCount > 2);
            context.runOnClient(
                    client -> {
                        try {
                            var cube = client.level.getEntity(cubeId);
                            boolean entityCulling = FabricLoader.getInstance().isModLoaded("entityculling");
                            var setter = entityCulling ? cube.getClass().getMethod("setCulled", boolean.class) : null;
                            var getter = entityCulling ? cube.getClass().getMethod("isCulled") : null;
                            if (entityCulling) {
                                setter.invoke(cube, true);
                                if (!(boolean) getter.invoke(cube)) {
                                    throw new AssertionError(
                                            "Test must start with the destination cube hidden from the main camera");
                                }
                            }
                            try {
                                var entry = ClientPortalManager.getInstance().get(gun, PortalEnd.PRIMARY);
                                var exit = ClientPortalManager.getInstance().get(gun, PortalEnd.SECONDARY);
                                var position =
                                        new Vec3(client.gameRenderer.mainCamera().position())
                                                .transform(PortalEntity.getPortalToPortalMatrix(entry, exit));
                                var camera = new PortalCamera(client.level, client.player, position, 0, 90, 0, 1);
                                camera.clipAtExit(exit);
                                var fogCamera =
                                        new PortalCamera(
                                                client.level, client.player, new Vec3(exit.position()), 0, 90, 0, 1);
                                var root = client.gameRenderer.gameRenderState().levelRenderState;
                                var scene =
                                        PortalSceneExtractor.extract(
                                                client.level,
                                                camera,
                                                fogCamera,
                                                client.getDeltaTracker(),
                                                root,
                                                1,
                                                false);
                                if (scene.state().entityRenderStates.stream()
                                        .noneMatch(state -> state.entityType == EntityInit.STORAGE_CUBE.get())) {
                                    throw new AssertionError(
                                            "A main-camera-hidden cube must still be extracted for the portal camera: camera="
                                                    + camera.position()
                                                    + " cube="
                                                    + cube.position()
                                                    + " frustum="
                                                    + camera.getCullFrustum().isVisible(cube.getBoundingBox())
                                                    + " compiled="
                                                    + client.levelRenderer.isSectionCompiledAndVisible(
                                                            cube.blockPosition(), 0)
                                                    + " states="
                                                    + scene.state().entityRenderStates.stream()
                                                            .map(state -> state.entityType.toString())
                                                            .toList());
                                }
                                if (entityCulling && !(boolean) getter.invoke(cube)) {
                                    throw new AssertionError(
                                            "Portal extraction must preserve the main camera's culling decision");
                                }
                            } finally {
                                if (entityCulling) {
                                    setter.invoke(cube, false);
                                }
                            }
                        } catch (ReflectiveOperationException exception) {
                            throw new AssertionError("Cannot exercise Entity Culling integration", exception);
                        }
                    });
            UUID nestedGun = UUID.randomUUID();
            world.getServer()
                    .runOnServer(
                            server -> {
                                var level = world.getConnection().getServerPlayer().level();
                                level.getEntity(cubeId).discard();
                                for (int y = 99; y <= 104; y++) {
                                    for (int z = 0; z <= 6; z++) {
                                        level.setBlock(
                                                new BlockPos(34, y, z),
                                                BlockInit.LUNECAST.get().defaultBlockState(),
                                                2);
                                    }
                                }
                                for (int x = 37; x <= 43; x++) {
                                    for (int z = 9; z <= 15; z++) {
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
                                                "cyan",
                                                nestedGun,
                                                new Vec3(35, 101.625, 3.5),
                                                Direction.EAST,
                                                Direction.UP,
                                                false,
                                                null,
                                                null);
                                var exit =
                                        PortalPlacer.placePortal(
                                                level,
                                                PortalEnd.SECONDARY,
                                                "purple",
                                                nestedGun,
                                                new Vec3(40.5, 100, 12.5),
                                                Direction.UP,
                                                Direction.NORTH,
                                                false,
                                                null,
                                                null);
                                if (entry == null || exit == null) {
                                    throw new AssertionError("Nested sky test portals failed to place");
                                }
                            });
            world.getConnection().waitForClientboundPackets();
            context.waitFor(
                    client ->
                            ClientPortalManager.getInstance().has(nestedGun, PortalEnd.PRIMARY)
                                    && ClientPortalManager.getInstance().has(nestedGun, PortalEnd.SECONDARY));
            context.waitTicks(20);
            assertPixels(context, "portal-sky-nested-floor", true);
            world.getServer()
                    .runOnServer(
                            server -> {
                                var level = world.getConnection().getServerPlayer().level();
                                level.getEntitiesOfClass(
                                                PortalEntity.class,
                                                new AABB(30, 95, -2, 45, 110, 17),
                                                portal -> portal.getGunUUID().equals(nestedGun))
                                        .forEach(PortalEntity::discard);
                            });
            // A wall-to-floor view rotates the sky by 90 degrees. Testing only a
            // horizontal exit misses sky projection and lower-hemisphere failures.
            for (var up : new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
                world.getServer()
                        .runOnServer(
                                server -> {
                                    var level = world.getConnection().getServerPlayer().level();
                                    for (int x = 37; x <= 43; x++) {
                                        for (int z = 9; z <= 15; z++) {
                                            level.setBlock(
                                                    new BlockPos(x, 99, z),
                                                    BlockInit.LUNECAST.get().defaultBlockState(),
                                                    2);
                                        }
                                    }
                                    var exit =
                                            PortalPlacer.placePortal(
                                                    level,
                                                    PortalEnd.SECONDARY,
                                                    "orange",
                                                    gun,
                                                    new Vec3(40.5, 100, 12.5),
                                                    Direction.UP,
                                                    up,
                                                    false,
                                                    null,
                                                    null);
                                    if (exit == null) {
                                        throw new AssertionError("Floor sky test portal failed to place: " + up);
                                    }
                                });
                world.getConnection().waitForClientboundPackets();
                context.waitTicks(20);
                assertPixels(context, "portal-sky-floor-" + up.getSerializedName(), true);
            }
            world.getServer()
                    .runOnServer(
                            server -> {
                                var player = world.getConnection().getServerPlayer();
                                var level = player.level();
                                // The real player is below the vanilla sky horizon, while the exit is not.
                                // Destination sky must not inherit the real player's black underside disc.
                                for (int x = -4; x <= 4; x++) {
                                    for (int z = -2; z <= 6; z++) {
                                        level.setBlock(
                                                new BlockPos(x, -1, z),
                                                BlockInit.LUNECAST.get().defaultBlockState(),
                                                2);
                                    }
                                }
                                for (int x = -2; x <= 4; x++) {
                                    for (int y = 0; y <= 4; y++) {
                                        level.setBlock(
                                                new BlockPos(x, y, 0), BlockInit.LUNECAST.get().defaultBlockState(), 2);
                                    }
                                }
                                var entry =
                                        PortalPlacer.placePortal(
                                                level,
                                                PortalEnd.PRIMARY,
                                                "blue",
                                                gun,
                                                new Vec3(.5, 1.625, 1),
                                                Direction.SOUTH,
                                                Direction.UP,
                                                false,
                                                null,
                                                null);
                                if (entry == null) {
                                    throw new AssertionError("Below-horizon sky test portal failed to place");
                                }
                                player.connection.teleport(.5, 0, 4, 180, 0);
                            });
            world.getConnection().waitForClientboundPackets();
            context.waitFor(client -> client.player.getY() < 1);
            world.getConnection().waitForChunksRender();
            context.waitTicks(20);
            assertPixels(context, "portal-sky-below-horizon", true, new Vec3(.5, 1.625, 1));
        }
    }

    private static void assertPixels(ClientGameTestContext context, String name, boolean sky) {
        assertPixels(context, name, sky, new Vec3(.5, 101.625, 1));
    }

    private static void assertVisibleEdge(ClientGameTestContext context, String name) {
        try {
            var image = ImageIO.read(context.takeScreenshot(name).toFile());
            var opening =
                    context.computeOnClient(
                            client -> {
                                var state = client.gameRenderer.gameRenderState().levelRenderState.cameraRenderState;
                                var transform = new Matrix4f(state.projectionMatrix).mul(state.viewRotationMatrix);
                                var rect =
                                        new float[] {
                                            Float.POSITIVE_INFINITY,
                                            Float.POSITIVE_INFINITY,
                                            Float.NEGATIVE_INFINITY,
                                            Float.NEGATIVE_INFINITY
                                        };
                                for (float x : new float[] {0, 1}) {
                                    for (float y : new float[] {100.625F, 102.625F}) {
                                        var p =
                                                transform.transform(
                                                        new Vector4f(
                                                                x - (float) state.pos.x,
                                                                y - (float) state.pos.y,
                                                                1 - (float) state.pos.z,
                                                                1));
                                        rect[0] = Math.min(rect[0], p.x / p.w);
                                        rect[1] = Math.min(rect[1], p.y / p.w);
                                        rect[2] = Math.max(rect[2], p.x / p.w);
                                        rect[3] = Math.max(rect[3], p.y / p.w);
                                    }
                                }
                                return rect;
                            });
            int left = Math.max(0, (int) ((opening[0] + 1) * image.getWidth() / 2));
            int right = Math.min(image.getWidth() - 1, (int) ((opening[2] + 1) * image.getWidth() / 2));
            int top = Math.max(0, (int) ((1 - opening[3]) * image.getHeight() / 2));
            int bottom = Math.min(image.getHeight() - 51, (int) ((1 - opening[1]) * image.getHeight() / 2));
            int portalPixels = 0;
            for (int y = top; y <= bottom; y++) {
                for (int x = left; x <= right; x++) {
                    int rgb = image.getRGB(x, y), r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
                    if (g > 20 && b > 20 && g > r * 1.4 && b > r * 1.4) {
                        portalPixels++;
                    }
                }
            }
            if (portalPixels < 150) {
                throw new AssertionError(
                        "Visible portal disappeared at screen edge: " + name + " pixels=" + portalPixels);
            }
            var markers =
                    context.computeOnClient(
                            client -> {
                                var state = client.gameRenderer.gameRenderState().levelRenderState.cameraRenderState;
                                var transform = new Matrix4f(state.projectionMatrix).mul(state.viewRotationMatrix);
                                var points = new ArrayList<Vector4f>();
                                for (float x : new float[] {-1.5F, 2.5F}) {
                                    for (float y : new float[] {100.5F, 101.5F, 102.5F, 103.5F, 104.5F}) {
                                        points.add(
                                                transform.transform(
                                                        new Vector4f(
                                                                x - (float) state.pos.x,
                                                                y - (float) state.pos.y,
                                                                1 - (float) state.pos.z,
                                                                1)));
                                    }
                                }
                                return points;
                            });
            int visibleMarkers = 0;
            for (var point : markers) {
                if (point.w <= 0) {
                    continue;
                }
                int x = Math.round((point.x / point.w + 1) * image.getWidth() / 2);
                int y = Math.round((1 - point.y / point.w) * image.getHeight() / 2);
                if (x < 4 || y < 4 || x >= image.getWidth() - 4 || y >= image.getHeight() - 54) {
                    continue;
                }
                int redPixels = 0;
                for (int py = y - 2; py <= y + 2; py++) {
                    for (int px = x - 2; px <= x + 2; px++) {
                        int rgb = image.getRGB(px, py), r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
                        if (r > 30 && r > g * 1.4 && r > b * 1.2) {
                            redPixels++;
                        }
                    }
                }
                if (redPixels < 15) {
                    throw new AssertionError("Main-world terrain vanished after portal render: " + name);
                }
                visibleMarkers++;
            }
            if (visibleMarkers == 0) {
                throw new AssertionError("No main-world terrain witness in edge test: " + name);
            }
        } catch (IOException error) {
            throw new AssertionError(error);
        }
    }

    private static void assertPixels(ClientGameTestContext context, String name, boolean sky, Vec3 portalCenter) {
        var center =
                context.computeOnClient(
                        client -> {
                            var state = client.gameRenderer.gameRenderState().levelRenderState.cameraRenderState;
                            return new Matrix4f(state.projectionMatrix)
                                    .mul(state.viewRotationMatrix)
                                    .transform(
                                            new Vector4f(
                                                    (float) (portalCenter.x - state.pos.x),
                                                    (float) (portalCenter.y - state.pos.y),
                                                    (float) (portalCenter.z - state.pos.z),
                                                    1));
                        });
        var screenshot = context.takeScreenshot(name);
        try {
            var image = ImageIO.read(screenshot.toFile());
            int centerX = Math.round((center.x / center.w + 1) * image.getWidth() / 2);
            int centerY = Math.round((1 - center.y / center.w) * image.getHeight() / 2);
            int cyan = 0, samples = 0;
            for (int y = centerY - 10; y <= centerY + 10; y++) {
                for (int x = centerX - 8; x <= centerX + 8; x++) {
                    if (x < 0 || y < 0 || x >= image.getWidth() || y >= image.getHeight()) {
                        continue;
                    }
                    int color = image.getRGB(x, y),
                            red = color >> 16 & 255,
                            green = color >> 8 & 255,
                            blue = color & 255;
                    if (sky
                            ? red > 40 && blue > 90 && blue > red * 1.1 && green > red * .9
                            : green > 20 && blue > 20 && green > red * 1.4 && blue > red * 1.4) {
                        cyan++;
                    }
                    samples++;
                }
            }
            if (samples == 0 || cyan < samples / 2) {
                throw new AssertionError(
                        (sky ? "Sky is missing" : "Destination terrain was culled")
                                + " in "
                                + name
                                + ": visible="
                                + cyan
                                + "/"
                                + samples);
            }
        } catch (IOException exception) {
            throw new AssertionError("Cannot inspect visibility screenshot", exception);
        }
    }
}
