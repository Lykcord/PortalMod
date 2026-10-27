package net.portalmod.capture;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.InactivityFpsLimit;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.portalmod.common.entities.TestElementEntity;
import net.portalmod.common.sorted.portal.ClientPortalManager;
import net.portalmod.common.sorted.portal.ITeleportable;
import net.portalmod.common.sorted.portal.PortalEnd;
import net.portalmod.common.sorted.portal.PortalPlacer;
import net.portalmod.common.sorted.portalgun.PortalGun;
import net.portalmod.core.config.PortalModConfigManager;
import net.portalmod.core.init.BlockInit;
import net.portalmod.core.init.EntityInit;
import net.portalmod.core.init.ItemInit;
import net.portalmod.core.math.Vec3;

import java.nio.file.Path;
import java.util.UUID;

public final class PortalWalkingClientTest implements FabricClientGameTest {
    private record Walk(
            String name,
            boolean bob,
            boolean angled,
            boolean held,
            boolean turning,
            boolean newRoom,
            boolean oscillating) {
        Walk(String name, boolean bob, boolean angled, boolean held, boolean turning) {
            this(name, bob, angled, held, turning, false, false);
        }

        Walk(String name, boolean bob, boolean angled, boolean held, boolean turning, boolean newRoom) {
            this(name, bob, angled, held, turning, newRoom, false);
        }
    }

    @Override
    public void runTest(ClientGameTestContext context) {
        String output = System.getProperty("portalmod.walkCaptureOutput");
        if (output == null) {
            return;
        }
        Path directory = Path.of(output, "walk-" + System.currentTimeMillis());
        int previousDepth = PortalModConfigManager.RECURSION.get();
        PortalModConfigManager.RECURSION.set(Integer.getInteger("portalmod.walkCaptureDepth", 3));
        UUID gun = UUID.randomUUID();
        try (var world =
                context.worldBuilder()
                        .adjustSettings(
                                settings -> settings.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE))
                        .create()) {
            world.getServer()
                    .runOnServer(
                            server -> {
                                var level = world.getConnection().getServerPlayer().level();
                                for (int x = -8; x <= 8; x++) {
                                    for (int z = -3; z <= 20; z++) {
                                        var floor =
                                                (x + z & 1) == 0
                                                        ? BlockInit.LUNECAST.get()
                                                        : BlockInit.BLACKPLATE.get();
                                        level.setBlock(new BlockPos(x, 99, z), floor.defaultBlockState(), 2);
                                    }
                                }
                                for (int x = -8; x <= 8; x++) {
                                    for (int y = 100; y <= 105; y++) {
                                        level.setBlock(
                                                new BlockPos(x, y, 0), BlockInit.LUNECAST.get().defaultBlockState(), 2);
                                        level.setBlock(
                                                new BlockPos(x, y, 16),
                                                BlockInit.LUNECAST.get().defaultBlockState(),
                                                2);
                                    }
                                }
                                for (int z = 1; z <= 15; z++) {
                                    for (int y = 100; y <= 103; y++) {
                                        level.setBlock(
                                                new BlockPos(-8, y, z), Blocks.CONCRETE.red().defaultBlockState(), 2);
                                        level.setBlock(
                                                new BlockPos(8, y, z), Blocks.CONCRETE.green().defaultBlockState(), 2);
                                    }
                                }
                                for (int x = 14; x <= 38; x++) {
                                    for (int z = -8; z <= 14; z++) {
                                        level.setBlock(
                                                new BlockPos(x, 99, z),
                                                ((x + z & 1) == 0
                                                                ? BlockInit.LUNECAST.get()
                                                                : BlockInit.BLACKPLATE.get())
                                                        .defaultBlockState(),
                                                2);
                                    }
                                }
                                for (int y = 100; y <= 105; y++) {
                                    for (int z = -8; z <= 14; z++) {
                                        level.setBlock(
                                                new BlockPos(14, y, z), Blocks.CONCRETE.blue().defaultBlockState(), 2);
                                        level.setBlock(
                                                new BlockPos(30, y, z),
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
                                    throw new AssertionError("Cannot place walking capture portals");
                                }
                            });
            world.getConnection().waitForClientboundPackets();
            context.waitFor(
                    client ->
                            ClientPortalManager.getInstance().has(gun, PortalEnd.PRIMARY)
                                    && ClientPortalManager.getInstance().has(gun, PortalEnd.SECONDARY));
            world.getConnection().waitForChunksRender();
            for (var walk :
                    new Walk[] {
                        new Walk("straight-no-bobbing", false, false, false, false),
                        new Walk("angled-no-bobbing", false, true, false, false),
                        new Walk("straight-bobbing", true, false, false, false),
                        new Walk("angled-bobbing", true, true, false, false),
                        new Walk("straight-new-room", true, false, false, false, true),
                        new Walk("carried-cube-crossing", true, false, true, false),
                        new Walk("carried-cube-turning", false, false, true, true),
                        new Walk("portal-boundary-side-step", true, true, false, false, false, true)
                    }) {
                int cubeId =
                        world.getServer()
                                .computeOnServer(
                                        server -> {
                                            var player = world.getConnection().getServerPlayer();
                                            var exit =
                                                    PortalPlacer.placePortal(
                                                            player.level(),
                                                            PortalEnd.SECONDARY,
                                                            "orange",
                                                            gun,
                                                            walk.newRoom
                                                                    ? new Vec3(30, 101, 3.5)
                                                                    : new Vec3(.5, 101, 16),
                                                            walk.newRoom ? Direction.WEST : Direction.NORTH,
                                                            Direction.UP,
                                                            false,
                                                            null,
                                                            null);
                                            if (exit == null) {
                                                throw new AssertionError("Cannot set capture destination");
                                            }
                                            double x = walk.oscillating ? .5 : walk.angled ? 2.5 : .5;
                                            double z =
                                                    walk.oscillating ? 1.025 : walk.turning ? 8 : walk.angled ? 3 : 4;
                                            player.connection.teleport(x, 100, z, walk.angled ? 135 : 180, 0);
                                            player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                                            if (!walk.held) {
                                                player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                                                if (!walk.oscillating) {
                                                    return -1;
                                                }
                                                var marker =
                                                        EntityInit.STORAGE_CUBE
                                                                .get()
                                                                .create(player.level(), EntitySpawnReason.COMMAND);
                                                marker.snapTo(.5, 100, 2);
                                                player.level().addFreshEntity(marker);
                                                return marker.getId();
                                            }
                                            var stack = new ItemStack(ItemInit.PORTALGUN.get());
                                            PortalGun.addUUID(stack);
                                            player.setItemSlot(EquipmentSlot.MAINHAND, stack);
                                            var cube =
                                                    EntityInit.STORAGE_CUBE
                                                            .get()
                                                            .create(player.level(), EntitySpawnReason.COMMAND);
                                            cube.snapTo(x, 100, z - 1.5);
                                            player.level().addFreshEntity(cube);
                                            return cube.getId();
                                        });
                world.getConnection().waitForClientboundPackets();
                context.waitFor(
                        client ->
                                client.player.getY() > 99
                                        && Math.abs(
                                                        client.player.getZ()
                                                                - (walk.oscillating
                                                                        ? 1.025
                                                                        : walk.turning ? 8 : walk.angled ? 3 : 4))
                                                < .01);
                context.runOnClient(
                        client -> {
                            client.options.framerateLimit().set(120);
                            client.options.fov().set(Integer.getInteger("portalmod.walkCaptureFov", 70));
                            client.options.enableVsync().set(false);
                            client.options.inactivityFpsLimit().set(InactivityFpsLimit.MINIMIZED);
                            client.options.bobView().set(walk.bob);
                            client.options.pauseOnLostFocus = false;
                            ((ITeleportable) client.player).removeLastUsedPortal();
                        });
                context.getInput().lookAt(walk.angled ? 135 : 180, 0);
                context.waitTicks(20);
                if (walk.held) {
                    context.waitFor(
                            client ->
                                    client.level.getEntity(cubeId) != null
                                            && client.player.getMainHandItem().is(ItemInit.PORTALGUN.get()));
                    context.runOnClient(
                            client -> {
                                if (!((TestElementEntity) client.level.getEntity(cubeId)).pickUp(client.player)) {
                                    throw new AssertionError("Cannot pick up capture cube");
                                }
                            });
                    world.getConnection().waitForServerboundPackets();
                    context.waitTicks(8);
                }
                var recorder =
                        context.computeOnClient(
                                client -> {
                                    var capture = WalkFrameRecorder.start(directory.resolve(walk.name), walk.turning);
                                    capture.writeFixture(gun);
                                    if (cubeId >= 0) {
                                        capture.watchEntity(cubeId);
                                    }
                                    return capture;
                                });
                try {
                    context.waitTicks(4);
                    if (walk.oscillating) {
                        for (int crossing = 0; crossing < 10; crossing++) {
                            int previous =
                                    context.computeOnClient(
                                            client -> ((ITeleportable) client.player).getLastUsedPortal());
                            boolean right = (crossing & 1) == 0;
                            if (right) {
                                context.getInput().holdKey(options -> options.keyRight);
                            } else {
                                context.getInput().holdKey(options -> options.keyLeft);
                            }
                            try {
                                context.waitFor(
                                        client -> ((ITeleportable) client.player).getLastUsedPortal() != previous, 30);
                            } finally {
                                if (right) {
                                    context.getInput().releaseKey(options -> options.keyRight);
                                } else {
                                    context.getInput().releaseKey(options -> options.keyLeft);
                                }
                            }
                        }
                    } else {
                        if (!walk.turning) {
                            context.getInput().holdKey(options -> options.keyUp);
                        }
                        context.waitTicks(35);
                        context.getInput().releaseKey(options -> options.keyUp);
                    }
                    context.waitTicks(5);
                    context.runOnClient(
                            client -> {
                                if (!walk.turning && !((ITeleportable) client.player).hasLastUsedPortal()) {
                                    throw new AssertionError("Walking capture never crossed the portal");
                                }
                                if (walk.held && client.level.getEntity(cubeId).getVehicle() != client.player) {
                                    throw new AssertionError("Capture cube unexpectedly detached");
                                }
                            });
                } finally {
                    context.getInput().releaseKey(options -> options.keyUp);
                    context.runOnClient(client -> WalkFrameRecorder.stop());
                }
                context.waitFor(client -> recorder.finished(), 100);
                recorder.writeMetadata();
                if (cubeId >= 0) {
                    world.getServer()
                            .runOnServer(
                                    server ->
                                            world.getConnection()
                                                    .getServerPlayer()
                                                    .level()
                                                    .getEntity(cubeId)
                                                    .discard());
                    world.getConnection().waitForClientboundPackets();
                }
            }
        }
        context.restoreDefaultGameOptions();
        PortalModConfigManager.RECURSION.set(previousDepth);
    }
}
