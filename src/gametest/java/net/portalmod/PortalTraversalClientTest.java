package net.portalmod;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.portalmod.common.sorted.portal.ClientPortalManager;
import net.portalmod.common.sorted.portal.PortalEnd;
import net.portalmod.common.sorted.portal.PortalPlacer;
import net.portalmod.core.init.BlockInit;
import net.portalmod.core.math.Vec3;

import java.io.IOException;
import java.util.UUID;

import javax.imageio.ImageIO;

/** Samples sub-tick crossing positions; a successful teleport alone misses one-frame wall flashes. */
public final class PortalTraversalClientTest implements FabricClientGameTest {
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
                                for (int y = 90; y <= 115; y++) {
                                    for (int z = -12; z <= 18; z++) {
                                        level.setBlock(
                                                new BlockPos(30, y, z), Blocks.CONCRETE.cyan().defaultBlockState(), 2);
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
                                    throw new AssertionError("Traversal portals failed to place");
                                }
                                player.getAbilities().flying = true;
                                player.onUpdateAbilities();
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
            context.waitTicks(30);
            int index = 0;
            for (float[] look : new float[][] {{180, 0}, {135, 0}, {225, 0}, {180, -30}, {180, 30}}) {
                for (double distance : new double[] {.08, .025, .005, .0015, .0008, 0, -.0008, -.005, -.025, -.08}) {
                    world.getServer()
                            .runOnServer(
                                    server ->
                                            world.getConnection()
                                                    .getServerPlayer()
                                                    .connection
                                                    .teleport(.5, 100, 1 + distance, look[0], look[1]));
                    world.getConnection().waitForClientboundPackets();
                    context.runOnClient(
                            client -> {
                                client.player.snapTo(.5, 100, 1 + distance, look[0], look[1]);
                                client.player.setOldPosAndRot();
                                client.player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                            });
                    context.waitTicks(2);
                    var screenshot = context.takeScreenshot("portal-crossing-" + index++);
                    try {
                        var image = ImageIO.read(screenshot.toFile());
                        int visible = 0;
                        for (int y = image.getHeight() / 2 - 12; y <= image.getHeight() / 2 + 12; y++) {
                            for (int x = image.getWidth() / 2 - 12; x <= image.getWidth() / 2 + 12; x++) {
                                int color = image.getRGB(x, y),
                                        r = color >> 16 & 255,
                                        g = color >> 8 & 255,
                                        b = color & 255;
                                if (g > 20 && b > 20 && g > r * 1.4 && b > r * 1.4) {
                                    visible++;
                                }
                            }
                        }
                        if (visible < 500) {
                            throw new AssertionError(
                                    "Portal backing wall flashed at distance "
                                            + distance
                                            + ", destination pixels="
                                            + visible
                                            + "/625, "
                                            + context.computeOnClient(
                                                    client ->
                                                            "camera="
                                                                    + client.gameRenderer.mainCamera().position()
                                                                    + " player="
                                                                    + client.player.position()
                                                                    + " overlay="
                                                                    + client.gameRenderer.gameRenderState()
                                                                            .levelRenderState
                                                                            .playerRenderState
                                                                            .blockOverlay));
                        }
                    } catch (IOException error) {
                        throw new AssertionError(error);
                    }
                }
            }
        }
    }
}
