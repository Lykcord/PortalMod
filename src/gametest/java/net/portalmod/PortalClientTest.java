package net.portalmod;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.QuadParticleGroup;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.portalmod.client.render.PortalCamera;
import net.portalmod.common.sorted.antline.AntlineBlock;
import net.portalmod.common.sorted.antline.AntlineTileEntity;
import net.portalmod.common.sorted.autoportal.AutoPortalBlock;
import net.portalmod.common.sorted.autoportal.AutoPortalTileEntity;
import net.portalmod.common.sorted.button.QuadBlockCorner;
import net.portalmod.common.sorted.faithplate.FaithPlateBlock;
import net.portalmod.common.sorted.faithplate.FaithPlateConfigScreen;
import net.portalmod.common.sorted.fizzler.FizzlerEmitterBlock;
import net.portalmod.common.sorted.portal.ClientPortalManager;
import net.portalmod.common.sorted.portal.ITeleportable;
import net.portalmod.common.sorted.portal.PortalEnd;
import net.portalmod.common.sorted.portal.PortalEntity;
import net.portalmod.common.sorted.portal.PortalManager;
import net.portalmod.common.sorted.portal.PortalPlacer;
import net.portalmod.common.sorted.portal.PortalRenderer;
import net.portalmod.common.sorted.portalgun.skins.SkinSelectorScreen;
import net.portalmod.common.sorted.trigger.TriggerBlock;
import net.portalmod.common.sorted.trigger.TriggerTileEntity;
import net.portalmod.common.sorted.trigger.TriggerType;
import net.portalmod.core.init.BlockInit;
import net.portalmod.core.init.EntityInit;
import net.portalmod.core.init.ItemInit;
import net.portalmod.core.math.Vec3;
import net.portalmod.mixins.accessors.LevelRendererAccessor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public class PortalClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        UUID gun = UUID.randomUUID();
        UUID emitterGun = UUID.randomUUID();
        BlockPos plate = new BlockPos(2, 100, 4);
        TestWorldSave save;
        Path dataDirectory;
        try (var world =
                context.worldBuilder()
                        .adjustSettings(
                                settings -> settings.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE))
                        .create()) {
            save = world.getWorldSave();
            dataDirectory = world.getServer().computeOnServer(server -> server.getWorldPath(LevelResource.DATA));
            world.getServer()
                    .runOnServer(
                            server -> {
                                var player = world.getConnection().getServerPlayer();
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
                                var primary =
                                        PortalPlacer.placePortal(
                                                level,
                                                PortalEnd.PRIMARY,
                                                "blue",
                                                gun,
                                                new Vec3(.5, 101.5, 1),
                                                Direction.SOUTH,
                                                Direction.UP,
                                                false,
                                                null,
                                                null);
                                var secondary =
                                        PortalPlacer.placePortal(
                                                level,
                                                PortalEnd.SECONDARY,
                                                "orange",
                                                gun,
                                                new Vec3(.5, 101.5, 8),
                                                Direction.NORTH,
                                                Direction.UP,
                                                false,
                                                null,
                                                null);
                                if (primary == null || secondary == null) {
                                    throw new AssertionError("Client test portals failed to place");
                                }
                                var cube = EntityInit.STORAGE_CUBE.get().create(level, EntitySpawnReason.COMMAND);
                                if (cube == null) {
                                    throw new AssertionError("Cube failed to create");
                                }
                                cube.snapTo(.5, 100, 6);
                                level.addFreshEntity(cube);
                                var state = BlockInit.FAITHPLATE.get().defaultBlockState();
                                level.setBlock(
                                        plate.north(), state.setValue(FaithPlateBlock.HALF, DoubleBlockHalf.LOWER), 2);
                                level.setBlock(plate, state, 2);
                                var emitterBlock = (AutoPortalBlock) BlockInit.AUTOPORTAL.get();
                                var emitterState =
                                        emitterBlock
                                                .defaultBlockState()
                                                .setValue(AutoPortalBlock.FACING, Direction.SOUTH)
                                                .setValue(AutoPortalBlock.CORNER, QuadBlockCorner.DOWN_LEFT);
                                var emitterPos = new BlockPos(-4, 100, 1);
                                var emitterParts = new HashMap<>(emitterBlock.getOtherParts(emitterState, emitterPos));
                                emitterParts.put(emitterPos, emitterState);
                                emitterParts.forEach(
                                        (pos, part) -> {
                                            level.setBlock(
                                                    pos.north(), BlockInit.LUNECAST.get().defaultBlockState(), 2);
                                            level.setBlock(pos, part, 2);
                                        });
                                ((AutoPortalTileEntity) level.getBlockEntity(emitterPos))
                                        .link(emitterGun, PortalEnd.PRIMARY, 1, 10);
                                var triggerPos = new BlockPos(-3, 100, 4);
                                level.setBlock(
                                        triggerPos,
                                        BlockInit.TRIGGER
                                                .get()
                                                .defaultBlockState()
                                                .setValue(TriggerBlock.TYPE, TriggerType.MOB),
                                        2);
                                ((TriggerTileEntity) level.getBlockEntity(triggerPos))
                                        .setField(new BlockPos(-1, 0, -1), new BlockPos(1, 2, 1));
                                var wireStack = new ItemStack(ItemInit.ANTLINE.get());
                                var wireItem = (BlockItem) wireStack.getItem();
                                for (var hit :
                                        List.of(
                                                new BlockHitResult(
                                                        new net.minecraft.world.phys.Vec3(1.5, 100, 1.5),
                                                        Direction.UP,
                                                        new BlockPos(1, 99, 1),
                                                        false),
                                                new BlockHitResult(
                                                        new net.minecraft.world.phys.Vec3(1.5, 100.5, 1),
                                                        Direction.SOUTH,
                                                        new BlockPos(1, 100, 0),
                                                        false))) {
                                    if (!wireItem.place(
                                                    new BlockPlaceContext(
                                                            player, InteractionHand.MAIN_HAND, wireStack, hit))
                                            .consumesAction()) {
                                        throw new AssertionError(
                                                "Antline item cannot place floor and wall wires in the same block");
                                    }
                                }
                                player.connection.teleport(.5, 100, 4, 180, 0);
                            });
            world.getConnection().waitForClientboundPackets();
            context.waitFor(
                    client ->
                            ClientPortalManager.getInstance().has(gun, PortalEnd.PRIMARY)
                                    && ClientPortalManager.getInstance().has(gun, PortalEnd.SECONDARY));
            context.waitTicks(40);
            context.runOnClient(client -> verifyChamberConfiguration(client, emitterGun, PortalEnd.PRIMARY));
            world.getServer()
                    .runOnServer(
                            server ->
                                    ((AutoPortalTileEntity)
                                                    world.getConnection()
                                                            .getServerPlayer()
                                                            .level()
                                                            .getBlockEntity(new BlockPos(-4, 100, 1)))
                                            .swapEnd());
            context.waitFor(
                    client ->
                            client.level.getBlockEntity(new BlockPos(-4, 100, 1))
                                            instanceof AutoPortalTileEntity emitter
                                    && emitter.end == PortalEnd.SECONDARY);
            context.runOnClient(PortalClientTest::verifyAntlineFaces);
            world.getServer()
                    .runOnServer(
                            server -> {
                                var player = world.getConnection().getServerPlayer();
                                var pos = new BlockPos(1, 100, 1);
                                var state = player.level().getBlockState(pos);
                                ((AntlineBlock) state.getBlock())
                                        .breakDot(state, player.level(), pos, player, Direction.NORTH);
                            });
            context.waitFor(
                    client ->
                            client.level.getBlockEntity(new BlockPos(1, 100, 1)) instanceof AntlineTileEntity wire
                                    && !wire.getSideMap().hasSide(Direction.NORTH));
            world.getServer()
                    .runOnServer(
                            server -> {
                                var player = world.getConnection().getServerPlayer();
                                var stack = new ItemStack(ItemInit.ANTLINE.get());
                                var hit =
                                        new BlockHitResult(
                                                new net.minecraft.world.phys.Vec3(1.5, 100.5, 1),
                                                Direction.SOUTH,
                                                new BlockPos(1, 100, 0),
                                                false);
                                if (!((BlockItem) stack.getItem())
                                        .place(new BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack, hit))
                                        .consumesAction()) {
                                    throw new AssertionError("Wall antline cannot be placed again after removing it");
                                }
                            });
            context.waitFor(
                    client ->
                            client.level.getBlockEntity(new BlockPos(1, 100, 1)) instanceof AntlineTileEntity wire
                                    && wire.getSideMap().hasSide(Direction.NORTH));
            context.runOnClient(PortalClientTest::verifyParticleSnapshots);
            context.runOnClient(
                    client -> {
                        var entry = ClientPortalManager.getInstance().get(gun, PortalEnd.PRIMARY);
                        var exit = ClientPortalManager.getInstance().get(gun, PortalEnd.SECONDARY);
                        var position =
                                new Vec3(client.gameRenderer.mainCamera().position())
                                        .transform(PortalEntity.getPortalToPortalMatrix(entry, exit));
                        var camera = new PortalCamera(client.level, client.player, position, 0, 180, 0, 1);
                        camera.clipAtExit(exit);
                        if (!camera.getCullFrustum().isVisible(new AABB(0, 100, 2, 1, 102, 3))) {
                            throw new AssertionError("Portal CPU frustum rejects terrain in front of the exit");
                        }
                    });
            context.takeScreenshot("portalmod-recursive-portals");
            for (float yaw : new float[] {135, 225, 180}) {
                context.getInput().lookAt(yaw, 0);
                context.waitTicks(3);
                context.runOnClient(
                        client -> {
                            var state = ((LevelRendererAccessor) client.levelRenderer).pmRenderState();
                            if (!PortalRenderer.getInstance().hasViews(state)) {
                                throw new AssertionError("Portal surface vanished near the screen edge at yaw " + yaw);
                            }
                        });
                if (yaw != 180) {
                    context.takeScreenshot("portalmod-portal-edge-" + (int) yaw);
                }
            }
            for (float pitch : new float[] {-60, 0, 60}) {
                context.getInput().lookAt(180, pitch);
                context.waitTicks(10);
            }
            context.getInput().lookAt(180, 0);
            context.waitFor(client -> client.level.getBlockEntity(plate) != null);
            context.setScreen(() -> new FaithPlateConfigScreen(plate));
            context.waitTick();
            context.runOnClient(
                    client -> {
                        if (!(client.gui.screen() instanceof FaithPlateConfigScreen)) {
                            throw new AssertionError("Faith plate screen did not open");
                        }
                    });
            context.takeScreenshot("portalmod-faithplate-config");
            context.setScreen(() -> SkinSelectorScreen.getInstance(null));
            context.waitTicks(5);
            context.takeScreenshot("portalmod-skin-selector");
            context.setScreen(() -> null);
            float walkBeforePortal =
                    context.computeOnClient(client -> client.player.avatarState().getInterpolatedWalkDistance(1));
            context.getInput().holdKey(options -> options.keyUp);
            try {
                context.waitFor(client -> ((ITeleportable) client.player).hasLastUsedPortal(), 100);
            } finally {
                context.getInput().releaseKey(options -> options.keyUp);
            }
            context.waitTicks(5);
            float walkAfterPortal =
                    context.computeOnClient(client -> client.player.avatarState().getInterpolatedWalkDistance(1));
            if (walkAfterPortal - walkBeforePortal > 4) {
                throw new AssertionError("Portal displacement was counted as footsteps, causing a view-bob shake");
            }
            var clientPosition = context.computeOnClient(client -> client.player.position());
            world.getServer()
                    .runOnServer(
                            server -> {
                                if (world.getConnection().getServerPlayer().position().distanceTo(clientPosition)
                                        > .5) {
                                    throw new AssertionError("Server did not accept player portal traversal");
                                }
                            });
            context.takeScreenshot("portalmod-after-player-traversal");
            world.getServer()
                    .runOnServer(
                            server -> {
                                var player = world.getConnection().getServerPlayer();
                                var level = player.level();
                                for (int x : new int[] {-2, 2}) {
                                    var state =
                                            BlockInit.FIZZLER_EMITTER
                                                    .get()
                                                    .defaultBlockState()
                                                    .setValue(
                                                            FizzlerEmitterBlock.FACING,
                                                            x < 0 ? Direction.EAST : Direction.WEST);
                                    level.setBlock(new BlockPos(x, 100, 3), state, 2);
                                    level.setBlock(
                                            new BlockPos(x, 101, 3),
                                            state.setValue(FizzlerEmitterBlock.HALF, DoubleBlockHalf.UPPER),
                                            2);
                                }
                                player.connection.teleport(.5, 100, 6, 180, 0);
                            });
            context.waitFor(
                    client -> client.level.getBlockState(new BlockPos(0, 100, 3)).is(BlockInit.FIZZLER_FIELD.get()));
            context.waitTicks(20);
            context.takeScreenshot("portalmod-connected-fizzler");
        }
        try (var restored = save.open()) {
            restored.getServer()
                    .runOnServer(
                            server -> {
                                var manager = PortalManager.getInstance();
                                if (!manager.has(gun, PortalEnd.PRIMARY) || !manager.has(gun, PortalEnd.SECONDARY)) {
                                    throw new AssertionError("Saved portal pair missing after world restart");
                                }
                            });
            context.waitFor(
                    client ->
                            ClientPortalManager.getInstance().has(gun, PortalEnd.PRIMARY)
                                    && ClientPortalManager.getInstance().has(gun, PortalEnd.SECONDARY));
            context.waitTicks(10);
            context.runOnClient(PortalClientTest::verifyAntlineFaces);
            context.runOnClient(client -> verifyChamberConfiguration(client, emitterGun, PortalEnd.SECONDARY));
            context.takeScreenshot("portalmod-restored-portals");
        }
        var current = dataDirectory.resolve("portalmod/portals.dat");
        try {
            Files.move(current, dataDirectory.resolve("portalmod_portals.dat"));
        } catch (IOException error) {
            throw new AssertionError("Cannot prepare legacy portal-file fixture", error);
        }
        try (var imported = save.open()) {
            imported.getServer()
                    .runOnServer(
                            server -> {
                                var manager = PortalManager.getInstance();
                                if (!manager.has(gun, PortalEnd.PRIMARY) || !manager.has(gun, PortalEnd.SECONDARY)) {
                                    throw new AssertionError("Legacy portal file was not imported");
                                }
                            });
            context.waitFor(
                    client ->
                            ClientPortalManager.getInstance().has(gun, PortalEnd.PRIMARY)
                                    && ClientPortalManager.getInstance().has(gun, PortalEnd.SECONDARY));
            context.waitTicks(10);
            context.takeScreenshot("portalmod-imported-legacy-file");
        }
        if (!Files.exists(current)) {
            throw new AssertionError("Imported portals were not saved to the native data file");
        }
    }

    private static void verifyAntlineFaces(Minecraft client) {
        var pos = new BlockPos(1, 100, 1);
        if (!(client.level.getBlockEntity(pos) instanceof AntlineTileEntity wire)
                || !wire.getSideMap().hasSide(Direction.DOWN)
                || !wire.getSideMap().hasSide(Direction.NORTH)) {
            throw new AssertionError("Floor and wall wire faces did not synchronize or survive world reload");
        }
        var block = (AntlineBlock) wire.getBlockState().getBlock();
        for (var face : new Direction[] {Direction.DOWN, Direction.NORTH}) {
            if (block.getSideShape(client.level, pos, face, true).isEmpty()) {
                throw new AssertionError("Synchronized wire face has no selection geometry: " + face);
            }
        }
    }

    private static void verifyChamberConfiguration(Minecraft client, UUID gun, PortalEnd end) {
        if (!(client.level.getBlockEntity(new BlockPos(-4, 100, 1)) instanceof AutoPortalTileEntity emitter)
                || !gun.equals(emitter.gunUUID)
                || emitter.end != end
                || emitter.primaryColor != 1
                || emitter.secondaryColor != 10) {
            throw new AssertionError("Automatic emitter link did not synchronize or survive world reload");
        }
        if (!(client.level.getBlockEntity(new BlockPos(-3, 100, 4)) instanceof TriggerTileEntity trigger)
                || !new AABB(-1, 0, -1, 2, 3, 2).equals(trigger.getField())
                || trigger.getTriggerType() != TriggerType.MOB) {
            throw new AssertionError("Mob trigger configuration did not synchronize or survive world reload");
        }
    }

    private static void verifyParticleSnapshots(Minecraft client) {
        var camera = client.gameRenderer.mainCamera();
        var pos = camera.position().add(client.player.getViewVector(1).scale(2));
        var group = new QuadParticleGroup(client.particleEngine, ParticleRenderType.SINGLE_QUADS);
        group.add(
                new TerrainParticle(
                        client.level, pos.x, pos.y, pos.z, 0, 0, 0, BlockInit.LUNECAST.get().defaultBlockState()));
        var frustum = new Frustum(camera.getCullFrustum()).offset(-3);
        var root = (QuadParticleRenderState) group.extractRenderState(frustum, camera, 1);
        if (root.isEmpty()) {
            throw new AssertionError("Particle fixture must be visible");
        }
        var portalCamera = new PortalCamera(camera, 1);
        var first = group.extractRenderState(frustum, portalCamera, 1);
        var second = group.extractRenderState(frustum, portalCamera, 1);
        if (root == first || root == second || first == second) {
            throw new AssertionError("Portal particle snapshots share the main view's mutable geometry");
        }
        first.clear();
        second.clear();
        if (root.isEmpty()) {
            throw new AssertionError("Clearing a portal view cleared the main particle view");
        }
        root.clear();
        if (group.extractRenderState(frustum, camera, 1) != root) {
            throw new AssertionError("Main particle buffer was not restored after portal extraction");
        }
    }
}
