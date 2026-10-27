package net.portalmod;

import com.google.gson.GsonBuilder;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.TagValueInput;
import net.portalmod.common.blocks.MultiBlock;
import net.portalmod.common.sorted.antline.AntlineBlock;
import net.portalmod.common.sorted.antline.AntlineTileEntity;
import net.portalmod.common.sorted.autoportal.AutoPortalBlock;
import net.portalmod.common.sorted.button.SuperButtonBlock;
import net.portalmod.common.sorted.cubedropper.CubeDropperTileEntity;
import net.portalmod.common.sorted.faithplate.FaithPlateTileEntity;
import net.portalmod.common.sorted.fizzler.FizzlerEmitterBlock;
import net.portalmod.common.sorted.gel.AbstractGelBlock;
import net.portalmod.common.sorted.portal.PortalEnd;
import net.portalmod.common.sorted.portal.PortalPlacer;
import net.portalmod.common.sorted.portalgun.PortalGun;
import net.portalmod.common.sorted.trigger.TriggerTileEntity;
import net.portalmod.core.init.BlockInit;
import net.portalmod.core.init.EntityInit;
import net.portalmod.core.init.ItemInit;
import net.portalmod.core.math.Vec3;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.UUID;

/** Optional persistent creative review world, exported after the regression suite has passed. */
public final class PortalReviewWorldCreator implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        String output = System.getProperty("portalmod.reviewWorldOutput");
        if (output == null) {
            return;
        }
        Path destination = Path.of(output);
        if (Files.exists(destination)) {
            throw new AssertionError("Review world already exists; preserving it: " + destination);
        }
        Path save;
        UUID playerId;
        try (var world =
                context.worldBuilder()
                        .adjustSettings(
                                settings -> {
                                    settings.setName("PortalMod 26.3 Review");
                                    settings.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
                                    settings.setAllowCommands(true);
                                    settings.setDifficulty(Difficulty.PEACEFUL);
                                })
                        .create()) {
            save = world.getWorldSave().getSaveDirectory();
            playerId =
                    world.getServer()
                            .computeOnServer(
                                    server -> {
                                        ServerPlayer player = world.getConnection().getServerPlayer();
                                        createChamber(player.level(), player);
                                        if (!server.getWorldData().isAllowCommands()) {
                                            throw new AssertionError("Review world must allow cheats");
                                        }
                                        return player.getUUID();
                                    });
            world.getConnection().waitForClientboundPackets();
            context.waitFor(client -> client.player != null && client.player.getY() > 99);
            world.getConnection().waitForChunksRender();
            context.waitTicks(20);
            context.takeScreenshot("portalmod-review-world");
        }
        try {
            Files.createDirectories(destination);
            try (var files = Files.walk(save)) {
                for (Path source : files.toList()) {
                    Path relative = save.relativize(source);
                    if (relative.toString().equals("session.lock")) {
                        continue;
                    }
                    Path target = destination.resolve(relative);
                    if (Files.isDirectory(source)) {
                        Files.createDirectories(target);
                    } else {
                        Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES);
                    }
                }
            }
            var metadata = new LinkedHashMap<String, String>();
            metadata.put("world", destination.getFileName().toString());
            metadata.put("playerUuid", playerId.toString());
            metadata.put("mode", "creative");
            metadata.put("cheats", "true");
            metadata.put("spawn", "0.5 100 8");
            Files.writeString(
                    destination.getParent().getParent().resolve("review.json"),
                    new GsonBuilder().setPrettyPrinting().create().toJson(metadata));
        } catch (IOException exception) {
            throw new AssertionError("Cannot export creative review world", exception);
        }
    }

    private static void createChamber(ServerLevel level, ServerPlayer player) {
        for (int x = -28; x <= 28; x++) {
            for (int z = -20; z <= 28; z++) {
                level.setBlock(new BlockPos(x, 99, z), BlockInit.LUNECAST.get().defaultBlockState(), 2);
            }
        }
        for (int x = -22; x <= 22; x++) {
            for (int y = 100; y <= 105; y++) {
                level.setBlock(new BlockPos(x, y, 0), BlockInit.LUNECAST.get().defaultBlockState(), 2);
                level.setBlock(new BlockPos(x, y, 20), BlockInit.LUNECAST.get().defaultBlockState(), 2);
            }
        }
        for (int z = 10; z <= 15; z++) {
            for (int y = 100; y <= 105; y++) {
                level.setBlock(new BlockPos(21, y, z), BlockInit.LUNECAST.get().defaultBlockState(), 2);
            }
        }
        UUID recursive = UUID.randomUUID(), floor = UUID.randomUUID(), side = UUID.randomUUID();
        portal(level, recursive, PortalEnd.PRIMARY, "blue", new Vec3(.5, 101.5, 1), Direction.SOUTH, Direction.UP);
        portal(level, recursive, PortalEnd.SECONDARY, "orange", new Vec3(.5, 101.5, 20), Direction.NORTH, Direction.UP);
        portal(level, floor, PortalEnd.PRIMARY, "cyan", new Vec3(16.5, 101.5, 1), Direction.SOUTH, Direction.UP);
        portal(level, floor, PortalEnd.SECONDARY, "purple", new Vec3(16.5, 100, 12.5), Direction.UP, Direction.NORTH);
        portal(level, side, PortalEnd.PRIMARY, "lime", new Vec3(-16.5, 101.5, 1), Direction.SOUTH, Direction.UP);
        portal(level, side, PortalEnd.SECONDARY, "pink", new Vec3(21, 101.5, 12.5), Direction.WEST, Direction.UP);

        // Ready-to-use cube/button/wire/indicator/door circuit.
        var button = (SuperButtonBlock) BlockInit.SUPER_BUTTON.get();
        var buttonPos = new BlockPos(-10, 100, 10);
        place(level, buttonPos, button.defaultBlockState());
        place(level, new BlockPos(-6, 100, 12), BlockInit.CHAMBER_DOOR.get().defaultBlockState());
        level.setBlock(new BlockPos(-6, 100, 11), BlockInit.ANTLINE_INDICATOR.get().defaultBlockState(), 2);
        var wire = (AntlineBlock) BlockInit.ANTLINE.get();
        for (int x : new int[] {-8, -7}) {
            var pos = new BlockPos(x, 100, 11);
            level.setBlock(pos, wire.defaultBlockState(), 2);
            var entity = (AntlineTileEntity) level.getBlockEntity(pos);
            entity.getSideMap().put(Direction.DOWN, AntlineTileEntity.Side.dot(Direction.DOWN));
            wire.sideUpdate(level, entity.getSideMap().get(Direction.DOWN), pos, true, false, null);
        }
        var cube = EntityInit.STORAGE_CUBE.get().create(level, EntitySpawnReason.COMMAND);
        if (cube != null) {
            var trigger = button.getTrigger(button.defaultBlockState(), buttonPos);
            cube.snapTo(trigger.getCenter().x, trigger.minY + .01, trigger.getCenter().z);
            level.addFreshEntity(cube);
        }
        place(level, new BlockPos(7, 100, 7), BlockInit.STANDING_BUTTON.get().defaultBlockState());
        place(level, new BlockPos(12, 100, 7), BlockInit.PUSH_DOOR.get().defaultBlockState());
        level.setBlock(new BlockPos(5, 100, 7), BlockInit.RADIO.get().defaultBlockState(), 2);
        level.setBlock(new BlockPos(5, 100, 9), BlockInit.FOREST_CAKE.get().defaultBlockState(), 2);
        place(level, new BlockPos(-2, 100, -14), BlockInit.CHAMBER_LIGHTS.get().defaultBlockState());
        place(level, new BlockPos(2, 100, -14), BlockInit.CHAMBER_LIGHTS.get().defaultBlockState());
        for (int x : new int[] {-4, 4}) {
            place(
                    level,
                    new BlockPos(x, 100, -9),
                    BlockInit.FIZZLER_EMITTER
                            .get()
                            .defaultBlockState()
                            .setValue(FizzlerEmitterBlock.FACING, x < 0 ? Direction.EAST : Direction.WEST));
        }
        for (int x = 12; x <= 14; x++) {
            for (int z = -8; z <= -3; z++) {
                level.setBlock(
                        new BlockPos(x, 100, z),
                        BlockInit.REPULSION_GEL.get().defaultBlockState().setValue(AbstractGelBlock.DOWN, true),
                        2);
            }
        }
        for (int x = 17; x <= 19; x++) {
            for (int z = -8; z <= -1; z++) {
                level.setBlock(
                        new BlockPos(x, 100, z),
                        BlockInit.PROPULSION_GEL.get().defaultBlockState().setValue(AbstractGelBlock.DOWN, true),
                        2);
            }
        }
        for (int x = -22; x <= -16; x++) {
            for (int z = -9; z <= -3; z++) {
                boolean edge = x == -22 || x == -16 || z == -9 || z == -3;
                level.setBlock(new BlockPos(x, 98, z), BlockInit.BLACKPLATE.get().defaultBlockState(), 2);
                level.setBlock(
                        new BlockPos(x, 99, z),
                        edge ? BlockInit.BLACKPLATE.get().defaultBlockState() : BlockInit.GOO.get().defaultBlockState(),
                        2);
                if (edge) {
                    level.setBlock(new BlockPos(x, 100, z), BlockInit.BLACKPLATE.get().defaultBlockState(), 2);
                }
            }
        }
        var platePos = new BlockPos(9, 100, 12);
        place(level, platePos, BlockInit.FAITHPLATE.get().defaultBlockState());
        if (level.getBlockEntity(platePos) instanceof FaithPlateTileEntity plate) {
            var data = new CompoundTag();
            data.putBoolean("enabled", true);
            var target = new CompoundTag();
            target.putDouble("x", 5);
            target.putDouble("y", 0);
            target.putDouble("z", 0);
            target.putByte("side", (byte) Direction.UP.get3DDataValue());
            target.putFloat("height", 4);
            data.put("target", target);
            plate.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), data));
            plate.setChanged();
        }
        place(level, new BlockPos(-12, 104, 5), BlockInit.CUBE_DROPPER.get().defaultBlockState());
        if (level.getBlockEntity(new BlockPos(-12, 104, 5)) instanceof CubeDropperTileEntity dropper) {
            dropper.onEggClick(new ItemStack(ItemInit.STORAGE_CUBE.get()), player);
        }
        var turret = EntityInit.TURRET.get().create(level, EntitySpawnReason.COMMAND);
        if (turret != null) {
            turret.snapTo(-19, 100, 17, 180, 0);
            level.addFreshEntity(turret);
        }
        place(level, new BlockPos(-12, 100, -14), BlockInit.TRIGGER.get().defaultBlockState());
        if (level.getBlockEntity(new BlockPos(-12, 100, -14)) instanceof TriggerTileEntity trigger) {
            trigger.setField(new BlockPos(-2, 0, -2), new BlockPos(2, 2, 2));
        }
        place(
                level,
                new BlockPos(12, 100, 20),
                BlockInit.AUTOPORTAL.get().defaultBlockState().setValue(AutoPortalBlock.FACING, Direction.NORTH));

        var items = new ArrayList<>(ItemInit.ITEMS.getRegistry().values());
        for (int start = 0, index = 0; start < items.size(); start += 27, index++) {
            BlockPos pos = new BlockPos(-24 + index * 3, 100, 25);
            level.setBlock(pos, Blocks.BARREL.defaultBlockState(), 2);
            if (level.getBlockEntity(pos) instanceof BarrelBlockEntity barrel) {
                for (int slot = 0; slot < 27 && start + slot < items.size(); slot++) {
                    barrel.setItem(slot, new ItemStack(items.get(start + slot).get()));
                }
                barrel.setChanged();
            }
        }
        player.getInventory().setItem(0, gun(recursive, "blue", "orange"));
        player.getInventory().setItem(1, new ItemStack(ItemInit.WRENCH.get()));
        player.getInventory().setItem(2, new ItemStack(ItemInit.STORAGE_CUBE.get()));
        player.getInventory().setItem(3, new ItemStack(ItemInit.COMPANION_CUBE.get()));
        player.getInventory().setItem(4, new ItemStack(ItemInit.TURRET.get()));
        player.getInventory().setItem(5, new ItemStack(ItemInit.ANTLINE.get(), 64));
        player.getInventory().setItem(6, new ItemStack(ItemInit.SUPER_BUTTON.get()));
        player.getInventory().setItem(7, new ItemStack(ItemInit.REPULSION_GEL.get()));
        player.getInventory().setItem(8, new ItemStack(ItemInit.LONGFALL_BOOTS.get()));
        player.getInventory().setItem(9, gun(floor, "cyan", "purple"));
        player.getInventory().setItem(10, gun(side, "lime", "pink"));
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(ItemInit.LONGFALL_BOOTS.get()));
        player.removeAllEffects();
        level.getServer().getWorldData().setAllowCommands(true);
        level.setRespawnData(LevelData.RespawnData.of(Level.OVERWORLD, new BlockPos(0, 100, 8), 180, 0));
        player.connection.teleport(.5, 100, 8, 180, 0);
    }

    private static void portal(
            ServerLevel level, UUID gun, PortalEnd end, String hue, Vec3 pos, Direction face, Direction up) {
        if (PortalPlacer.placePortal(level, end, hue, gun, pos, face, up, false, null, null) == null) {
            throw new AssertionError("Cannot create review portal " + end + " at " + pos);
        }
    }

    private static ItemStack gun(UUID uuid, String left, String right) {
        var stack = PortalGun.modifyColors(new ItemStack(ItemInit.PORTALGUN.get()), left, right, "none");
        CustomData.update(DataComponents.CUSTOM_DATA, stack, data -> data.store("gunUUID", UUIDUtil.CODEC, uuid));
        return stack;
    }

    private static void place(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, 2);
        if (state.getBlock() instanceof MultiBlock multi) {
            multi.getOtherParts(state, pos).forEach((part, value) -> level.setBlock(part, value, 2));
        }
    }
}
