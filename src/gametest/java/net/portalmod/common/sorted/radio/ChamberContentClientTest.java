package net.portalmod.common.sorted.radio;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.portalmod.common.sorted.sign.ChamberSignEntity;
import net.portalmod.core.init.BlockInit;
import net.portalmod.core.init.ItemInit;
import net.portalmod.core.init.PacketInit;
import net.portalmod.core.packet.PacketContext;

public class ChamberContentClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> client.options.pauseOnLostFocus = false);
        var radioPos = new BlockPos(2, 100, 3);
        try (var world =
                context.worldBuilder()
                        .adjustSettings(
                                settings -> settings.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE))
                        .create()) {
            var signId =
                    world.getServer()
                            .computeOnServer(
                                    server -> {
                                        var player = world.getConnection().getServerPlayer();
                                        var level = player.level();
                                        for (int x = -3; x <= 5; x++) {
                                            for (int z = -2; z <= 6; z++) {
                                                level.setBlock(
                                                        new BlockPos(x, 99, z),
                                                        BlockInit.LUNECAST.get().defaultBlockState(),
                                                        2);
                                            }
                                        }
                                        for (int x = -1; x <= 3; x++) {
                                            for (int y = 100; y <= 104; y++) {
                                                level.setBlock(
                                                        new BlockPos(x, y, 0),
                                                        BlockInit.LUNECAST.get().defaultBlockState(),
                                                        2);
                                            }
                                        }
                                        var radioStack = new ItemStack(ItemInit.RADIO.get());
                                        var radioHit =
                                                new BlockHitResult(
                                                        new Vec3(2.5, 100, 3.5), Direction.UP, radioPos.below(), false);
                                        if (!((BlockItem) radioStack.getItem())
                                                .place(
                                                        new BlockPlaceContext(
                                                                player,
                                                                InteractionHand.MAIN_HAND,
                                                                radioStack,
                                                                radioHit))
                                                .consumesAction()) {
                                            throw new AssertionError("Radio item failed to place");
                                        }
                                        var sign =
                                                new ChamberSignEntity(
                                                        level, new BlockPos(0, 101, 1), Direction.SOUTH, true);
                                        sign.setLeftDigit(2);
                                        sign.setRightDigit(6);
                                        sign.setProgress(4);
                                        if (!sign.survives()) {
                                            throw new AssertionError("Client sign fixture cannot attach to its wall");
                                        }
                                        level.addFreshEntity(sign);
                                        player.connection.teleport(.5, 100, 5, 180, 0);
                                        return sign.getId();
                                    });
            context.waitFor(
                    client ->
                            client.level.getBlockEntity(radioPos) instanceof RadioBlockTileEntity
                                    && client.level.getEntity(signId) instanceof ChamberSignEntity);
            context.waitTicks(10);
            context.runOnClient(
                    client -> {
                        var sign = (ChamberSignEntity) client.level.getEntity(signId);
                        if (sign.getDirection() != Direction.SOUTH
                                || sign.getLeftDigit() != 2
                                || sign.getRightDigit() != 6
                                || !sign.getVerticallyAligned()
                                || sign.getProgress() != 4) {
                            throw new AssertionError("Chamber sign appearance did not synchronize");
                        }
                    });
            context.takeScreenshot("portalmod-chamber-sign");
            world.getServer()
                    .runOnServer(
                            server ->
                                    ((RadioBlockTileEntity)
                                                    world.getConnection()
                                                            .getServerPlayer()
                                                            .level()
                                                            .getBlockEntity(radioPos))
                                            .switchManual());
            context.waitFor(
                    client ->
                            currentSound(client, radioPos) != null
                                    && client.getSoundManager().isActive(currentSound(client, radioPos)));
            var firstSound = context.computeOnClient(client -> currentSound(client, radioPos));
            world.getServer()
                    .runOnServer(
                            server ->
                                    ((RadioBlockTileEntity)
                                                    world.getConnection()
                                                            .getServerPlayer()
                                                            .level()
                                                            .getBlockEntity(radioPos))
                                            .switchManual());
            try {
                context.waitFor(client -> !client.getSoundManager().isActive(firstSound));
            } catch (AssertionError failure) {
                throw new AssertionError(
                        "Radio stop failed; server="
                                + world.getServer()
                                        .computeOnServer(
                                                server ->
                                                        world.getConnection()
                                                                .getServerPlayer()
                                                                .level()
                                                                .getBlockState(radioPos))
                                + ", sound instance unchanged="
                                + context.computeOnClient(client -> currentSound(client, radioPos) == firstSound),
                        failure);
            }
            world.getServer()
                    .runOnServer(
                            server ->
                                    ((RadioBlockTileEntity)
                                                    world.getConnection()
                                                            .getServerPlayer()
                                                            .level()
                                                            .getBlockEntity(radioPos))
                                            .switchManual());
            context.waitFor(
                    client ->
                            currentSound(client, radioPos) != firstSound
                                    && client.getSoundManager().isActive(currentSound(client, radioPos)));
            var secondSound = context.computeOnClient(client -> currentSound(client, radioPos));
            world.getServer()
                    .runOnServer(
                            server ->
                                    world.getConnection()
                                            .getServerPlayer()
                                            .level()
                                            .setBlock(radioPos, Blocks.CHEST.defaultBlockState(), 3));
            context.waitFor(
                    client ->
                            client.level.getBlockState(radioPos).is(Blocks.CHEST)
                                    && !client.getSoundManager().isActive(secondSound));
            world.getServer()
                    .runOnServer(
                            server ->
                                    PacketInit.sendToPlayer(
                                            world.getConnection().getServerPlayer(),
                                            new SRadioUpdatePacket(radioPos, RadioState.ON)));
            world.getConnection().waitForClientboundPackets();
            context.waitTicks(2);
            context.runOnClient(
                    client ->
                            new SRadioUpdatePacket(radioPos, RadioState.ON)
                                    .handle(() -> new PacketContext(null, Runnable::run)));
        }
        context.runOnClient(
                client ->
                        new SRadioUpdatePacket(new BlockPos(2, 100, 3), RadioState.OFF)
                                .handle(() -> new PacketContext(null, Runnable::run)));
        context.waitTick();
    }

    private static RadioSound currentSound(Minecraft client, BlockPos pos) {
        return RadioBlockClient.RADIOS.get(client.level.getBlockEntity(pos));
    }
}
