package net.portalmod;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.portalmod.common.sorted.portal.ClientPortalManager;
import net.portalmod.common.sorted.portal.PortalEnd;
import net.portalmod.common.sorted.portal.PortalPlacer;
import net.portalmod.common.sorted.portal.PortalRenderer;
import net.portalmod.core.config.PortalModConfigManager;
import net.portalmod.core.init.BlockInit;
import net.portalmod.core.math.Vec3;

import java.util.UUID;

public final class PortalCloudsClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        var previousClouds = context.computeOnClient(client -> client.options.cloudStatus().get());
        int previousDepth = PortalModConfigManager.RECURSION.get();
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
                                    for (int z = -2; z <= 18; z++) {
                                        level.setBlock(
                                                new BlockPos(x, 99, z),
                                                BlockInit.LUNECAST.get().defaultBlockState(),
                                                2);
                                    }
                                }
                                for (int x = -2; x <= 2; x++) {
                                    for (int y = 100; y <= 103; y++) {
                                        level.setBlock(
                                                new BlockPos(x, y, 0), BlockInit.LUNECAST.get().defaultBlockState(), 2);
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
                                    throw new AssertionError("Cannot place cloud-rendering portals");
                                }
                                player.connection.teleport(.5, 100, 8, 180, -15);
                            });
            world.getConnection().waitForClientboundPackets();
            context.waitFor(
                    client ->
                            client.player.getY() > 99
                                    && ClientPortalManager.getInstance().has(gun, PortalEnd.SECONDARY));
            context.runOnClient(
                    client -> {
                        PortalModConfigManager.RECURSION.set(9);
                        client.options.cloudStatus().set(CloudStatus.FANCY);
                    });
            world.getConnection().waitForChunksRender();
            context.waitFor(client -> PortalRenderer.getInstance().renderedPortals >= 4);
            context.waitTicks(60);
            context.takeScreenshot("portalmod-recursive-clouds");
        } finally {
            context.runOnClient(
                    client -> {
                        client.options.cloudStatus().set(previousClouds);
                        PortalModConfigManager.RECURSION.set(previousDepth);
                    });
        }
    }
}
