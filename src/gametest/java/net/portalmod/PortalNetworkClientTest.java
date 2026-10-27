package net.portalmod;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.portalmod.common.entities.TestElementEntity;
import net.portalmod.common.items.WrenchItem;
import net.portalmod.common.sorted.cube.Cube;
import net.portalmod.common.sorted.cube.storage.StorageCubeRenderer;
import net.portalmod.common.sorted.portal.ClientPortalManager;
import net.portalmod.common.sorted.portal.ITeleportable;
import net.portalmod.common.sorted.portal.PortalEnd;
import net.portalmod.common.sorted.portal.PortalManager;
import net.portalmod.common.sorted.portalgun.CPortalGunInteractionPacket;
import net.portalmod.common.sorted.portalgun.PortalGun;
import net.portalmod.common.sorted.portalgun.PortalGunInteraction;
import net.portalmod.common.sorted.trigger.TriggerSelectionClient;
import net.portalmod.common.sorted.trigger.TriggerSelectionServer;
import net.portalmod.common.sorted.trigger.TriggerTileEntity;
import net.portalmod.core.init.BlockInit;
import net.portalmod.core.init.EntityInit;
import net.portalmod.core.init.ItemInit;
import net.portalmod.core.packet.ClientNetworking;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.Properties;

@Environment(EnvType.CLIENT)
public class PortalNetworkClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        var properties = new Properties();
        properties.setProperty("server-ip", "127.0.0.1");
        properties.setProperty("gamemode", "creative");
        try (var socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            properties.setProperty("server-port", Integer.toString(socket.getLocalPort()));
        } catch (IOException error) {
            throw new AssertionError("Cannot allocate a test server port", error);
        }
        try (var server = context.worldBuilder().createServer(properties);
                var connection = server.connect()) {
            var gun =
                    server.computeOnServer(
                            instance -> {
                                var player = connection.getServerPlayer();
                                var level = player.level();
                                for (int x = -4; x <= 4; x++) {
                                    for (int z = -2; z <= 10; z++) {
                                        level.setBlock(
                                                new BlockPos(x, 99, z),
                                                BlockInit.LUNECAST.get().defaultBlockState(),
                                                2);
                                    }
                                }
                                for (int x = -2; x <= 2; x++) {
                                    for (int y = 100; y <= 104; y++) {
                                        level.setBlock(
                                                new BlockPos(x, y, 0), BlockInit.LUNECAST.get().defaultBlockState(), 2);
                                        level.setBlock(
                                                new BlockPos(x, y, 8), BlockInit.LUNECAST.get().defaultBlockState(), 2);
                                    }
                                }
                                var stack = new ItemStack(ItemInit.PORTALGUN.get());
                                PortalGun.addUUID(stack);
                                player.setItemSlot(EquipmentSlot.MAINHAND, stack);
                                player.connection.teleport(.5, 100, 4, 180, 0);
                                return PortalGun.getUUID(stack).orElseThrow();
                            });
            connection.waitForClientboundPackets();
            context.waitFor(
                    client -> PortalGun.getUUID(client.player.getMainHandItem()).filter(gun::equals).isPresent());
            context.waitFor(
                    client ->
                            client.player.getY() > 99
                                    && client.level
                                            .getBlockState(new BlockPos(0, 101, 0))
                                            .is(BlockInit.LUNECAST.get()));
            context.runOnClient(
                    client -> {
                        if (!(client.getConnection().getConnection().getRemoteAddress() instanceof InetSocketAddress)) {
                            throw new AssertionError(
                                    "Network test must use TCP rather than an integrated-server connection");
                        }
                        client.mouseHandler.grabMouse();
                    });
            // Aim the aperture's bottom at walkway height so the player's feet fit through it.
            context.getInput()
                    .lookAt(
                            180,
                            context.computeOnClient(
                                    client -> (float) Math.toDegrees(Math.atan2(client.player.getEyeY() - 101, 3))));
            context.waitTicks(10);
            context.takeScreenshot("portalmod-network-before-shot");
            context.getInput().holdMouseFor(InputConstants.MOUSE_BUTTON_LEFT, 2);
            context.waitFor(client -> ClientPortalManager.getInstance().has(gun, PortalEnd.PRIMARY));
            context.waitTicks(10);
            context.getInput()
                    .lookAt(
                            0,
                            context.computeOnClient(
                                    client -> (float) Math.toDegrees(Math.atan2(client.player.getEyeY() - 101, 4))));
            context.waitTicks(2);
            context.getInput().holdMouseFor(InputConstants.MOUSE_BUTTON_RIGHT, 2);
            context.waitFor(client -> ClientPortalManager.getInstance().has(gun, PortalEnd.SECONDARY));
            server.runOnServer(
                    instance -> {
                        if (PortalManager.getInstance().get(gun, PortalEnd.SECONDARY).getDirection()
                                != Direction.NORTH) {
                            throw new AssertionError("Secondary shot did not hit the opposite chamber wall");
                        }
                    });
            context.getInput().lookAt(180, 0);
            context.waitTicks(10);
            context.takeScreenshot("portalmod-network-portals");
            context.getInput().holdKey(options -> options.keyUp);
            try {
                context.waitFor(client -> ((ITeleportable) client.player).hasLastUsedPortal(), 100);
            } catch (AssertionError failure) {
                context.takeScreenshot("portalmod-network-traversal-failure");
                throw new AssertionError(
                        "Player did not enter portal from "
                                + context.computeOnClient(client -> client.player.position()),
                        failure);
            } finally {
                context.getInput().releaseKey(options -> options.keyUp);
            }
            context.waitTicks(5);
            connection.waitForServerboundPackets();
            var position = context.computeOnClient(client -> client.player.position());
            server.runOnServer(
                    instance -> {
                        if (connection.getServerPlayer().position().distanceTo(position) > .5) {
                            throw new AssertionError("TCP server rejected portal traversal");
                        }
                    });
            context.runOnClient(
                    client ->
                            ClientNetworking.sendToServer(
                                    new CPortalGunInteractionPacket.Builder(PortalGunInteraction.FIZZLE).build()));
            connection.waitForServerboundPackets();
            server.waitFor(
                    instance ->
                            !PortalManager.getInstance().has(gun, PortalEnd.PRIMARY)
                                    && !PortalManager.getInstance().has(gun, PortalEnd.SECONDARY));
            context.waitFor(
                    client ->
                            !ClientPortalManager.getInstance().hasFullOrPartial(gun, PortalEnd.PRIMARY)
                                    && !ClientPortalManager.getInstance().hasFullOrPartial(gun, PortalEnd.SECONDARY));
            var cubeId =
                    server.computeOnServer(
                            instance -> {
                                var player = connection.getServerPlayer();
                                player.connection.teleport(.5, 100, 4, 180, 0);
                                var cube =
                                        EntityInit.STORAGE_CUBE.get().create(player.level(), EntitySpawnReason.COMMAND);
                                cube.snapTo(.5, 100, 3);
                                player.level().addFreshEntity(cube);
                                return cube.getId();
                            });
            context.waitFor(
                    client ->
                            client.level.getEntity(cubeId) != null
                                    && client.player.position().distanceTo(new Vec3(.5, 100, 4)) < .1);
            context.getInput().lookAt(180, 0);
            context.waitTicks(5);
            context.runOnClient(
                    client -> {
                        if (!((TestElementEntity) client.level.getEntity(cubeId)).pickUp(client.player)) {
                            throw new AssertionError("Client pickup rejected");
                        }
                    });
            connection.waitForServerboundPackets();
            server.runOnServer(
                    instance -> {
                        var cube = connection.getServerPlayer().level().getEntity(cubeId);
                        if (cube.getVehicle() != connection.getServerPlayer()) {
                            throw new AssertionError(
                                    "Server pickup lost: vehicle=" + cube.getVehicle() + ", pos=" + cube.position());
                        }
                    });
            context.getInput().lookAt(90, 0);
            context.waitTicks(12);
            context.runOnClient(
                    client -> {
                        var cube = (Cube) client.level.getEntity(cubeId);
                        float body = client.player.yBodyRot, oldBody = client.player.yBodyRotO;
                        try {
                            client.player.yBodyRot = client.player.yBodyRotO = 0;
                            cube.yBodyRot = cube.yBodyRotO = cube.yHeadRot = cube.yHeadRotO = 90;
                            var renderer =
                                    (StorageCubeRenderer)
                                            client.levelRenderer.entityRenderDispatcher().getRenderer(cube);
                            var state = renderer.createRenderState();
                            renderer.extractRenderState(cube, state, 1);
                            if (Math.abs(Mth.wrapDegrees(state.bodyRot - 90)) > .01) {
                                throw new AssertionError(
                                        "Held cube render rotation follows the holder's torso: " + state.bodyRot);
                            }
                        } finally {
                            client.player.yBodyRot = body;
                            client.player.yBodyRotO = oldBody;
                        }
                    });
            var heldPosition = context.computeOnClient(client -> client.level.getEntity(cubeId).position());
            connection.waitForServerboundPackets();
            server.runOnServer(
                    instance -> {
                        var cube = connection.getServerPlayer().level().getEntity(cubeId);
                        if (cube.position().distanceTo(heldPosition) > .2) {
                            throw new AssertionError(
                                    "Server hold position differs: " + cube.position() + " vs " + heldPosition);
                        }
                    });
            context.runOnClient(
                    client -> {
                        TestElementEntity.dropHeldEntities(client.player, false, true, client.player.getMainHandItem());
                        ClientNetworking.sendToServer(
                                new CPortalGunInteractionPacket.Builder(PortalGunInteraction.RELEASE_ENTITY).build());
                    });
            connection.waitForServerboundPackets();
            context.waitTicks(8);
            context.runOnClient(
                    client -> {
                        var cube = client.level.getEntity(cubeId);
                        if (Math.abs(cube.getX() - heldPosition.x) > .2
                                || Math.abs(cube.getZ() - heldPosition.z) > .2) {
                            throw new AssertionError(
                                    "Released cube snapped back: " + cube.position() + " vs " + heldPosition);
                        }
                    });
            server.runOnServer(
                    instance -> {
                        var player = connection.getServerPlayer();
                        var cube = (TestElementEntity) player.level().getEntity(cubeId);
                        if (!cube.pickUp(player)) {
                            throw new AssertionError("Server cannot pick up a released cube again");
                        }
                        cube.stopRiding();
                        if (!Double.isFinite(cube.getDeltaMovement().lengthSqr())) {
                            throw new AssertionError(
                                    "Release before the first movement update produced invalid momentum");
                        }
                    });
            var triggerPos = new BlockPos(2, 100, 3);
            server.runOnServer(
                    instance -> {
                        var player = connection.getServerPlayer();
                        player.level().setBlockAndUpdate(triggerPos, BlockInit.TRIGGER.get().defaultBlockState());
                        player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ItemInit.WRENCH.get()));
                        player.connection.teleport(2.5, 100, 5, 180, 20);
                    });
            connection.waitForClientboundPackets();
            context.waitFor(
                    client ->
                            client.player.getMainHandItem().is(ItemInit.WRENCH.get())
                                    && client.level.getBlockEntity(triggerPos) instanceof TriggerTileEntity);
            context.runOnClient(
                    client ->
                            client.gameMode.useItemOn(
                                    client.player,
                                    InteractionHand.MAIN_HAND,
                                    new BlockHitResult(
                                            Vec3.atCenterOf(triggerPos), Direction.SOUTH, triggerPos, false)));
            context.waitFor(client -> TriggerSelectionClient.isSelecting());
            context.runOnClient(
                    client -> {
                        TriggerSelectionClient.updateSelectedPos(triggerPos.offset(-1, 0, -1));
                        var wrench = (WrenchItem) client.player.getMainHandItem().getItem();
                        wrench.use(client.level, client.player, InteractionHand.MAIN_HAND);
                        if (!TriggerSelectionClient.isSelectingEnd()) {
                            throw new AssertionError("Wrench did not confirm the first trigger corner");
                        }
                        TriggerSelectionClient.updateSelectedPos(triggerPos.offset(1, 2, 1));
                        wrench.use(client.level, client.player, InteractionHand.MAIN_HAND);
                    });
            connection.waitForServerboundPackets();
            var field = new AABB(-1, 0, -1, 2, 3, 2);
            server.runOnServer(
                    instance -> {
                        var trigger =
                                (TriggerTileEntity) connection.getServerPlayer().level().getBlockEntity(triggerPos);
                        if (!field.equals(trigger.getField())
                                || trigger.isBeingConfigured()
                                || TriggerSelectionServer.TRIGGER_PER_PLAYER.containsKey(
                                        connection.getServerPlayer())) {
                            throw new AssertionError("Confirmed trigger field/configuration did not reach the server");
                        }
                    });
            context.waitFor(
                    client -> field.equals(((TriggerTileEntity) client.level.getBlockEntity(triggerPos)).getField()));
            context.takeScreenshot("portalmod-network-trigger-field");
            context.runOnClient(
                    client ->
                            client.gameMode.useItemOn(
                                    client.player,
                                    InteractionHand.MAIN_HAND,
                                    new BlockHitResult(
                                            Vec3.atCenterOf(triggerPos), Direction.SOUTH, triggerPos, false)));
            context.waitFor(client -> TriggerSelectionClient.isSelecting());
            server.runOnServer(
                    instance -> connection.getServerPlayer().setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY));
            context.waitFor(client -> !TriggerSelectionClient.isSelecting());
            connection.waitForServerboundPackets();
            server.runOnServer(
                    instance -> {
                        if (TriggerSelectionServer.TRIGGER_PER_PLAYER.containsKey(connection.getServerPlayer())) {
                            throw new AssertionError("Putting away the wrench did not release the trigger");
                        }
                        connection
                                .getServerPlayer()
                                .setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ItemInit.WRENCH.get()));
                    });
            context.waitFor(client -> client.player.getMainHandItem().is(ItemInit.WRENCH.get()));
            context.runOnClient(
                    client ->
                            client.gameMode.useItemOn(
                                    client.player,
                                    InteractionHand.MAIN_HAND,
                                    new BlockHitResult(
                                            Vec3.atCenterOf(triggerPos), Direction.SOUTH, triggerPos, false)));
            context.waitFor(client -> TriggerSelectionClient.isSelecting());
        }
        context.runOnClient(
                client -> {
                    if (TriggerSelectionClient.isSelecting()) {
                        throw new AssertionError("Disconnected client retained its trigger selection");
                    }
                });
    }
}
