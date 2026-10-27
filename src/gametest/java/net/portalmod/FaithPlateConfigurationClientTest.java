package net.portalmod;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.portalmod.common.items.WrenchItem;
import net.portalmod.common.sorted.faithplate.FaithPlateBlock;
import net.portalmod.common.sorted.faithplate.FaithPlateConfigScreen;
import net.portalmod.common.sorted.faithplate.FaithPlateTER;
import net.portalmod.common.sorted.faithplate.FaithPlateTileEntity;
import net.portalmod.core.init.BlockInit;
import net.portalmod.core.init.ItemInit;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Properties;

public class FaithPlateConfigurationClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        var properties = new Properties();
        properties.setProperty("server-ip", "127.0.0.1");
        properties.setProperty("gamemode", "creative");
        try (var socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            properties.setProperty("server-port", Integer.toString(socket.getLocalPort()));
        } catch (IOException error) {
            throw new AssertionError("Cannot allocate a configuration test server port", error);
        }
        var plate = new BlockPos(0, 100, 3);
        var hit = new BlockHitResult(Vec3.atCenterOf(plate), Direction.UP, plate, false);
        try (var server = context.worldBuilder().createServer(properties);
                var connection = server.connect()) {
            server.runOnServer(
                    instance -> {
                        var player = connection.getServerPlayer();
                        var level = player.level();
                        for (int x = -4; x <= 4; x++) {
                            for (int z = 0; z <= 8; z++) {
                                level.setBlockAndUpdate(
                                        new BlockPos(x, 99, z), BlockInit.LUNECAST.get().defaultBlockState());
                            }
                        }
                        var state = BlockInit.FAITHPLATE.get().defaultBlockState();
                        level.setBlockAndUpdate(
                                plate.north(), state.setValue(FaithPlateBlock.HALF, DoubleBlockHalf.LOWER));
                        level.setBlockAndUpdate(plate, state);
                        player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ItemInit.WRENCH.get()));
                        player.connection.teleport(.5, 100, 6.5, 180, 20);
                    });
            connection.waitForClientboundPackets();
            context.waitFor(
                    client ->
                            client.player.getMainHandItem().is(ItemInit.WRENCH.get())
                                    && client.level.getBlockEntity(plate) instanceof FaithPlateTileEntity);
            context.runOnClient(client -> client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, hit));
            context.waitForScreen(FaithPlateConfigScreen.class);
            context.clickScreenButton("container.faithplate.select");
            context.waitFor(client -> plate.equals(FaithPlateTER.selected) && client.gui.screen() == null);
            context.getInput().lookAt(0, 90);
            context.waitTicks(2);
            context.runOnClient(
                    client ->
                            ((WrenchItem) client.player.getMainHandItem().getItem())
                                    .use(client.level, client.player, InteractionHand.MAIN_HAND));
            connection.waitForServerboundPackets();
            server.runOnServer(
                    instance -> {
                        var entity = (FaithPlateTileEntity) connection.getServerPlayer().level().getBlockEntity(plate);
                        if (!new Vec3(0, -1, 3).equals(entity.getTargetPos())
                                || entity.getTargetFace() != Direction.UP
                                || !entity.isEnabled()
                                || entity.getHeight() != 1
                                || entity.isBeingConfigured()) {
                            throw new AssertionError(
                                    "Wrench target selection was not saved on the server: " + entity.getTargetPos());
                        }
                    });
            context.runOnClient(client -> client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, hit));
            context.waitForScreen(FaithPlateConfigScreen.class);
            context.runOnClient(
                    client -> {
                        var input =
                                (EditBox)
                                        client.gui.screen().children().stream()
                                                .filter(EditBox.class::isInstance)
                                                .findFirst()
                                                .orElseThrow();
                        input.setValue("");
                        input.insertText("3.5");
                    });
            context.takeScreenshot("portalmod-network-faithplate-configuration");
            context.clickScreenButton("gui.done");
            connection.waitForServerboundPackets();
            server.runOnServer(
                    instance -> {
                        var entity = (FaithPlateTileEntity) connection.getServerPlayer().level().getBlockEntity(plate);
                        if (entity.getHeight() != 3.5f
                                || entity.isBeingConfigured()
                                || FaithPlateTileEntity.isPlayerConfiguring(connection.getServerPlayer())) {
                            throw new AssertionError(
                                    "Faith-plate height or configuration release did not reach the server");
                        }
                    });
            context.runOnClient(client -> client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, hit));
            context.waitForScreen(FaithPlateConfigScreen.class);
            context.clickScreenButton("container.faithplate.select");
            context.waitFor(client -> plate.equals(FaithPlateTER.selected));
        }
        context.runOnClient(
                client -> {
                    if (FaithPlateTER.selected != null) {
                        throw new AssertionError("Disconnected client retained its faith-plate target selection");
                    }
                });
    }
}
