package net.portalmod;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.portalmod.common.sorted.antline.AntlineTileEntity;
import net.portalmod.core.init.BlockInit;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Properties;

public final class AntlineFacesClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        var properties = new Properties();
        properties.setProperty("server-ip", "127.0.0.1");
        properties.setProperty("gamemode", "creative");
        try (var socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            properties.setProperty("server-port", Integer.toString(socket.getLocalPort()));
        } catch (IOException error) {
            throw new AssertionError("Cannot allocate the antline test port", error);
        }
        try (var server = context.worldBuilder().createServer(properties);
                var connection = server.connect()) {
            server.runOnServer(
                    instance -> {
                        var level = connection.getServerPlayer().level();
                        for (int x = -5; x <= 5; x++) {
                            for (int z = -3; z <= 6; z++) {
                                level.setBlock(new BlockPos(x, 99, z), Blocks.STONE.defaultBlockState(), 2);
                            }
                        }
                        connection.getServerPlayer().connection.teleport(.5, 100, 3.5, 180, 0);
                    });
            connection.waitForClientboundPackets();
            context.waitFor(client -> client.player.getY() > 99);

            for (int testCase = 0; testCase < 4; testCase++) {
                var pos = new BlockPos(-3 + testCase * 2, 100, 0);
                boolean removeFloor = (testCase & 1) != 0;
                boolean removeSupport = testCase >= 2;
                Direction removedFace = removeFloor ? Direction.DOWN : Direction.NORTH;
                Direction remainingFace = removeFloor ? Direction.NORTH : Direction.DOWN;
                var target = removeSupport ? pos.relative(removedFace) : pos;
                var aim = removeFloor ? new Vec3(pos.getX() + .5, 100.06, .5) : new Vec3(pos.getX() + .5, 100.5, .06);

                server.runOnServer(
                        instance -> {
                            var player = connection.getServerPlayer();
                            var level = player.level();
                            level.setBlockAndUpdate(pos.north(), Blocks.STONE.defaultBlockState());
                            level.setBlockAndUpdate(pos, BlockInit.ANTLINE.get().defaultBlockState());
                            var wire = (AntlineTileEntity) level.getBlockEntity(pos);
                            wire.getSideMap().put(Direction.DOWN, AntlineTileEntity.Side.dot(Direction.DOWN));
                            wire.getSideMap().put(Direction.NORTH, AntlineTileEntity.Side.dot(Direction.NORTH));
                            wire.setChanged();
                            var state = level.getBlockState(pos);
                            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
                            var direction = aim.subtract(player.getEyePosition());
                            float yaw = (float) (Math.atan2(direction.z, direction.x) * Mth.RAD_TO_DEG) - 90;
                            float pitch =
                                    (float) (-Math.atan2(direction.y, direction.horizontalDistance()) * Mth.RAD_TO_DEG);
                            player.connection.teleport(player.getX(), player.getY(), player.getZ(), yaw, pitch);
                            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                        });
                connection.waitForClientboundPackets();
                context.waitFor(
                        client ->
                                client.level.getBlockEntity(pos) instanceof AntlineTileEntity wire
                                        && wire.getSideMap().hasSide(Direction.DOWN)
                                        && wire.getSideMap().hasSide(Direction.NORTH));
                context.waitTicks(3);
                var hitFace = removeFloor ? Direction.UP : Direction.SOUTH;
                context.runOnClient(client -> client.gameMode.startDestroyBlock(target, hitFace));
                connection.waitForServerboundPackets();
                server.waitFor(
                        instance ->
                                connection.getServerPlayer().level().getBlockEntity(pos)
                                                instanceof AntlineTileEntity wire
                                        && wire.getSideMap().hasSide(remainingFace)
                                        && !wire.getSideMap().hasSide(removedFace),
                        40);
                connection.waitForClientboundPackets();
                context.waitTicks(5);
                context.runOnClient(
                        client -> {
                            if (!(client.level.getBlockEntity(pos) instanceof AntlineTileEntity wire)
                                    || !wire.getSideMap().hasSide(remainingFace)
                                    || wire.getSideMap().hasSide(removedFace)) {
                                throw new AssertionError(
                                        "Client lost the remaining "
                                                + remainingFace
                                                + " antline after removing "
                                                + removedFace
                                                + (removeSupport ? " support" : " face")
                                                + " at "
                                                + pos);
                            }
                        });
            }
        }
    }
}
