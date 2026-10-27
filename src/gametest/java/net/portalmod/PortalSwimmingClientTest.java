package net.portalmod;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;

/** Ordinary swimming must keep working when the portal movement hooks are installed. */
public final class PortalSwimmingClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        try (var world = context.worldBuilder().create()) {
            world.getServer()
                    .runOnServer(
                            server -> {
                                var player = world.getConnection().getServerPlayer();
                                var level = player.level();
                                for (int x = -3; x <= 3; x++) {
                                    for (int z = -3; z <= 10; z++) {
                                        level.setBlock(new BlockPos(x, 98, z), Blocks.STONE.defaultBlockState(), 2);
                                        for (int y = 99; y <= 104; y++) {
                                            level.setBlock(new BlockPos(x, y, z), Blocks.WATER.defaultBlockState(), 2);
                                        }
                                    }
                                }
                                player.getFoodData().setFoodLevel(20);
                                player.connection.teleport(.5, 100, .5, 0, 0);
                            });
            world.getConnection().waitForClientboundPackets();
            context.waitFor(
                    client -> client.player != null && client.player.getY() > 99 && client.player.isUnderWater(), 200);
            context.getInput().holdKey(options -> options.keyUp);
            context.getInput().holdKey(options -> options.keySprint);
            try {
                context.waitFor(client -> client.player.isSwimming(), 100);
                context.waitFor(client -> client.player.getZ() > 2, 100);
            } finally {
                context.getInput().releaseKey(options -> options.keyUp);
                context.getInput().releaseKey(options -> options.keySprint);
            }
            world.getConnection().waitForServerboundPackets();
            world.getServer().waitFor(server -> world.getConnection().getServerPlayer().getZ() > 1.5, 100);
        }
    }
}
