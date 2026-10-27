package net.portalmod;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.TagParser;
import net.minecraft.server.notifications.EmptyNotificationService;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.worldupdate.UpgradeProgress;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.chunk.storage.RegionFile;
import net.minecraft.world.level.chunk.storage.RegionFileVersion;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.portalmod.common.sorted.antline.AntlineTileEntity;
import net.portalmod.common.sorted.autoportal.AutoPortalTileEntity;
import net.portalmod.common.sorted.cube.Cube;
import net.portalmod.common.sorted.portal.PortalEnd;
import net.portalmod.common.sorted.portal.PortalManager;
import net.portalmod.common.sorted.portalgun.PortalGun;
import net.portalmod.common.sorted.sign.ChamberSignEntity;
import net.portalmod.common.sorted.trigger.TriggerTileEntity;
import net.portalmod.common.sorted.trigger.TriggerType;
import net.portalmod.core.init.BlockInit;
import net.portalmod.core.init.GameRuleInit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class LegacyChunkClientTest implements FabricClientGameTest {
    private static final ChunkPos CHUNK = new ChunkPos(32, 32);
    private static final int BASE = 512;

    @Override
    public void runTest(ClientGameTestContext context) {
        var gun = UUID.randomUUID();
        var cube = UUID.randomUUID();
        var sign = UUID.randomUUID();
        var portalId = UUID.randomUUID();
        TestWorldSave save;
        Path root;
        try (var world = context.worldBuilder().create()) {
            save = world.getWorldSave();
            root = world.getServer().computeOnServer(server -> server.getWorldPath(LevelResource.ROOT).normalize());
        }
        var region = DimensionType.getStorageFolder(Level.OVERWORLD, root).resolve("region/r.1.1.mca");
        var upgraded =
                DataFixTypes.CHUNK.updateToCurrentVersion(
                        DataFixers.getDataFixer(), legacyChunk(gun, cube, sign), 2586);
        if (upgraded.getListOrEmpty("entities").size() != 2) {
            throw new AssertionError("Native chunk upgrade lost embedded mod entities");
        }
        try {
            Files.move(root, root.resolveSibling(root.getFileName() + "-modern-template"));
            Files.createDirectories(root);
            NbtIo.writeCompressed(legacyWorldData(), root.resolve("level.dat"));
            Files.createDirectories(root.resolve("data"));
            NbtIo.writeCompressed(legacyPortals(gun, portalId), root.resolve("data/portalmod_portals.dat"));
            for (var folder : new String[] {"region", "DIM-1/region", "DIM1/region"}) {
                var oldRegion = root.resolve(folder).resolve("r.1.1.mca");
                Files.createDirectories(oldRegion.getParent());
                try (var file = openRegion(oldRegion);
                        var output = file.getChunkDataOutputStream(CHUNK)) {
                    NbtIo.write(legacyChunk(gun, cube, sign), output);
                }
            }
            var storage = LevelStorageSource.createDefault(root.getParent());
            try (var access = storage.validateAndCreateAccess(root.getFileName().toString())) {
                DataFixers.getFileFixer()
                        .fix(
                                access,
                                access.getUnfixedDataTag(false),
                                new UpgradeProgress(new EmptyNotificationService()));
            }
        } catch (Exception error) {
            throw new AssertionError("Cannot prepare and upgrade legacy world fixture", error);
        }
        for (int pass = 0; pass < 2; pass++) {
            try (var world = save.open()) {
                world.getServer()
                        .runOnServer(
                                server -> {
                                    if (!server.getWorldData().getLevelName().equals("PortalMod Legacy 1.16.5")) {
                                        throw new AssertionError("Legacy world name was not preserved");
                                    }
                                    for (var dimension : List.of(Level.OVERWORLD, Level.NETHER, Level.END)) {
                                        var importedLevel = server.getLevel(dimension);
                                        if (importedLevel == null
                                                || !importedLevel
                                                        .getBlockState(at(1, 99, 1))
                                                        .is(BlockInit.LUNECAST.get())) {
                                            throw new AssertionError(
                                                    "Legacy dimension chunk was not imported: " + dimension);
                                        }
                                    }
                                    var level = world.getConnection().getServerPlayer().level();
                                    if (!level.getGameRules().get(GameRuleInit.PORTAL_SLOWSHOT)
                                            || !level.getGameRules().get(GameRuleInit.USE_PORTALABLE_BLACKLIST)
                                            || level.getGameRules().get(GameRuleInit.ALLOW_PORTAL_OVERWRITE)) {
                                        throw new AssertionError("Legacy mod game rules were not preserved");
                                    }
                                    var portal = PortalManager.getInstance().get(gun, PortalEnd.PRIMARY);
                                    if (portal == null
                                            || !portal.getUUID().equals(portalId)
                                            || !portal.getColor().equals("cyan")
                                            || portal.getDirection() != Direction.NORTH
                                            || portal.getUpVector() != Direction.UP
                                            || portal.position().distanceTo(new Vec3(BASE + 2.5, 101.5, BASE + 13))
                                                    > .001) {
                                        throw new AssertionError(
                                                "Legacy portal file was not imported with its saved settings");
                                    }
                                    level.getChunk(CHUNK.x(), CHUNK.z());
                                    if (!level.getBlockState(at(1, 99, 1)).is(BlockInit.LUNECAST.get())) {
                                        throw new AssertionError("Legacy mod block palette was not preserved");
                                    }
                                    var chest = (ChestBlockEntity) level.getBlockEntity(at(2, 100, 3));
                                    if (chest == null
                                            || PortalGun.getUUID(chest.getItem(0)).filter(gun::equals).isEmpty()) {
                                        throw new AssertionError("Legacy chest lost its portal gun or gun identity");
                                    }
                                    var trigger = (TriggerTileEntity) level.getBlockEntity(at(3, 100, 3));
                                    if (trigger == null
                                            || !new AABB(-1, 0, -1, 2, 3, 2).equals(trigger.getField())
                                            || trigger.getTriggerType() != TriggerType.MOB) {
                                        throw new AssertionError("Legacy trigger bounds/type were not preserved");
                                    }
                                    var emitter = (AutoPortalTileEntity) level.getBlockEntity(at(6, 100, 3));
                                    if (emitter == null
                                            || !gun.equals(emitter.gunUUID)
                                            || emitter.end != PortalEnd.SECONDARY
                                            || emitter.primaryColor != 1
                                            || emitter.secondaryColor != 10) {
                                        throw new AssertionError("Legacy automatic portal link was not preserved");
                                    }
                                    var wire = (AntlineTileEntity) level.getBlockEntity(at(4, 100, 5));
                                    if (wire == null
                                            || !wire.getSideMap().hasSide(Direction.DOWN)
                                            || !wire.getSideMap().hasSide(Direction.NORTH)) {
                                        throw new AssertionError("Legacy antline faces were not preserved");
                                    }
                                    world.getConnection()
                                            .getServerPlayer()
                                            .connection
                                            .teleport(BASE + 8.5, 100, BASE + 13, 180, 10);
                                });
                world.getServer()
                        .waitFor(server -> world.getConnection().getServerPlayer().level().getEntity(cube) != null);
                world.getServer()
                        .waitFor(server -> world.getConnection().getServerPlayer().level().getEntity(sign) != null);
                world.getServer()
                        .runOnServer(
                                server -> {
                                    var entity = world.getConnection().getServerPlayer().level().getEntity(cube);
                                    if (!(entity instanceof Cube)
                                            || !entity.hasCustomName()
                                            || !entity.getCustomName().getString().equals("Legacy Cube")) {
                                        throw new AssertionError(
                                                "Legacy cube identity or custom name was not preserved: " + entity);
                                    }
                                    var imported =
                                            (ChamberSignEntity)
                                                    world.getConnection().getServerPlayer().level().getEntity(sign);
                                    if (!imported.getPos().equals(at(12, 102, 3))
                                            || imported.getDirection() != Direction.SOUTH
                                            || !imported.getVerticallyAligned()
                                            || !imported.survives()) {
                                        throw new AssertionError(
                                                "Legacy chamber sign lost its mounting position: " + imported.getPos());
                                    }
                                    if (imported.getLeftDigit() != 2
                                            || imported.getRightDigit() != 6
                                            || imported.getProgress() != 4
                                            || !imported.getIcon2()
                                            || imported.getIcon1()
                                            || !imported.getIcon8()) {
                                        throw new AssertionError("Legacy chamber sign lost its display settings");
                                    }
                                });
                context.waitFor(client -> client.level.getBlockEntity(at(3, 100, 3)) != null);
                context.waitTicks(10);
                context.takeScreenshot("portalmod-legacy-chunk-" + pass);
            }
        }
        try (var file = openRegion(region);
                var input = file.getChunkDataInputStream(CHUNK)) {
            var saved = NbtIo.read(input);
            if (saved.getIntOr("DataVersion", 0) != SharedConstants.getCurrentVersion().dataVersion().version()) {
                throw new AssertionError("Imported chunk was not saved in the current format");
            }
            if (!Files.exists(root.resolve("data/portalmod/portals.dat"))) {
                throw new AssertionError("Imported portal file was not saved at the native path");
            }
        } catch (IOException error) {
            throw new AssertionError("Cannot inspect upgraded chunk", error);
        }
    }

    private static RegionFile openRegion(Path path) throws IOException {
        return new RegionFile(
                new RegionStorageInfo("legacy-test", Level.OVERWORLD, "chunk"),
                path,
                path.getParent(),
                RegionFileVersion.VERSION_DEFLATE,
                true);
    }

    private static BlockPos at(int x, int y, int z) {
        return new BlockPos(BASE + x, y, BASE + z);
    }

    private static CompoundTag legacyWorldData() throws CommandSyntaxException {
        return TagParser.parseCompoundFully(
                """
                {Data:{DataVersion:2586,version:19133,LevelName:"PortalMod Legacy 1.16.5",
                  Version:{Name:"1.16.5",Id:2586,Snapshot:0b},GameType:1,hardcore:0b,
                  allowCommands:1b,Difficulty:2b,DifficultyLocked:0b,initialized:1b,
                  Time:6000L,DayTime:6000L,SpawnX:520,SpawnY:100,SpawnZ:525,
                  GameRules:{portalSlowShot:"true",usePortalableBlacklist:"true",allowPortalOverwrite:"false"},
                  DataPacks:{Enabled:["vanilla","mod:forge","mod:portalmod"],Disabled:[]},
                  WorldGenSettings:{seed:42L,generate_features:0b,bonus_chest:0b,dimensions:{
                    "minecraft:overworld":{type:"minecraft:overworld",generator:{type:"minecraft:flat",settings:{
                      biome:"minecraft:plains",layers:[{block:"minecraft:bedrock",height:1}],structures:{structures:{}}}}},
                    "minecraft:the_nether":{type:"minecraft:the_nether",generator:{type:"minecraft:noise",seed:42L,
                      settings:"minecraft:nether",biome_source:{type:"minecraft:multi_noise",seed:42L,preset:"minecraft:nether"}}},
                    "minecraft:the_end":{type:"minecraft:the_end",generator:{type:"minecraft:noise",seed:42L,
                      settings:"minecraft:end",biome_source:{type:"minecraft:the_end",seed:42L}}}
                  }}}}
                """);
    }

    private static CompoundTag legacyPortals(UUID gun, UUID portalId) {
        var portal = new CompoundTag();
        portal.putString("id", "portalmod:portal");
        portal.putString("level", "minecraft:overworld");
        portal.putIntArray("UUID", UUIDUtil.uuidToIntArray(portalId));
        portal.putIntArray("gunUUID", UUIDUtil.uuidToIntArray(gun));
        portal.putString("end", "primary");
        portal.putString("up", "UP");
        portal.putString("facing", "NORTH");
        portal.putString("hue", "cyan");
        portal.store("Pos", Vec3.CODEC, new Vec3(BASE + 2.5, 101.5, BASE + 13));
        portal.store("Motion", Vec3.CODEC, Vec3.ZERO);
        portal.store("Rotation", Vec2.CODEC, Vec2.ZERO);
        var pair = new CompoundTag();
        pair.put("primary", portal);
        var data = new CompoundTag();
        data.put(gun.toString(), pair);
        var root = new CompoundTag();
        root.putInt("DataVersion", 2586);
        root.put("data", data);
        return root;
    }

    private static CompoundTag legacyChunk(UUID gun, UUID cube, UUID sign) {
        var root = new CompoundTag();
        root.putInt("DataVersion", 2586);
        var level = new CompoundTag();
        root.put("Level", level);
        level.putInt("xPos", CHUNK.x());
        level.putInt("zPos", CHUNK.z());
        level.putString("Status", "full");
        level.putLong("LastUpdate", 0);
        level.putLong("InhabitedTime", 0);
        level.putBoolean("isLightOn", false);
        int[] biomes = new int[1024];
        Arrays.fill(biomes, 1);
        level.putIntArray("Biomes", biomes);
        level.put("Heightmaps", new CompoundTag());
        var structures = new CompoundTag();
        structures.put("Starts", new CompoundTag());
        structures.put("References", new CompoundTag());
        level.put("Structures", structures);
        var palette = new ListTag();
        palette.add(state("minecraft:air"));
        palette.add(state("portalmod:lunecast"));
        palette.add(state("minecraft:chest", "facing", "north", "type", "single", "waterlogged", "false"));
        palette.add(state("portalmod:trigger", "axis", "x", "type", "mob", "state", "inactive"));
        for (String corner : new String[] {"down_left", "down_right", "up_left", "up_right"}) {
            palette.add(
                    state(
                            "portalmod:autoportal",
                            "facing",
                            "south",
                            "corner",
                            corner,
                            "direction",
                            "north",
                            "powered",
                            "false"));
        }
        palette.add(state("portalmod:antline"));
        palette.add(state("portalmod:radio", "facing", "north", "powered", "false", "state", "off"));
        long[] blocks = new long[256];
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                block(blocks, x, 99, z, 1);
            }
        }
        block(blocks, 2, 100, 3, 2);
        block(blocks, 3, 100, 3, 3);
        block(blocks, 6, 100, 3, 4);
        block(blocks, 7, 100, 3, 5);
        block(blocks, 6, 101, 3, 6);
        block(blocks, 7, 101, 3, 7);
        for (int x = 6; x <= 7; x++) {
            for (int y = 100; y <= 101; y++) {
                block(blocks, x, y, 2, 1);
            }
        }
        block(blocks, 4, 100, 5, 8);
        block(blocks, 4, 100, 4, 1);
        block(blocks, 9, 100, 3, 9);
        for (int x = 12; x <= 13; x++) {
            for (int y = 101; y <= 103; y++) {
                block(blocks, x, y, 2, 1);
            }
        }
        for (int x = 1; x <= 3; x++) {
            for (int y = 100; y <= 103; y++) {
                block(blocks, x, y, 13, 1);
            }
        }
        var section = new CompoundTag();
        section.putByte("Y", (byte) 6);
        section.put("Palette", palette);
        section.putLongArray("BlockStates", blocks);
        var sections = new ListTag();
        sections.add(section);
        level.put("Sections", sections);
        var tiles = new ListTag();
        var chest = tile("minecraft:chest", 2, 100, 3);
        var stack = new CompoundTag();
        stack.putString("id", "portalmod:portalgun");
        stack.putByte("Count", (byte) 1);
        stack.putByte("Slot", (byte) 0);
        var gunData = new CompoundTag();
        gunData.putIntArray("gunUUID", UUIDUtil.uuidToIntArray(gun));
        gunData.putString("LeftColor", "blue");
        gunData.putString("RightColor", "orange");
        stack.put("tag", gunData);
        var items = new ListTag();
        items.add(stack);
        chest.put("Items", items);
        tiles.add(chest);
        var trigger = tile("portalmod:trigger", 3, 100, 3);
        trigger.put("start", coordinates(-1, 0, -1));
        trigger.put("end", coordinates(1, 2, 1));
        tiles.add(trigger);
        var emitter = tile("portalmod:autoportal", 6, 100, 3);
        emitter.putIntArray("gunUUID", UUIDUtil.uuidToIntArray(gun));
        emitter.putString("end", "secondary");
        emitter.putString("primaryColor", "blue");
        emitter.putString("secondaryColor", "orange");
        tiles.add(emitter);
        var wire = tile("portalmod:antline", 4, 100, 5);
        var sides = new CompoundTag();
        for (var direction : Direction.values()) {
            sides.putByte(
                    direction.getName(), (byte) (direction == Direction.DOWN || direction == Direction.NORTH ? 0 : 15));
        }
        wire.put("AntlineSides", sides);
        tiles.add(wire);
        tiles.add(tile("portalmod:radio", 9, 100, 3));
        level.put("TileEntities", tiles);
        var entity = new CompoundTag();
        entity.putString("id", "portalmod:storage_cube");
        entity.putIntArray("UUID", UUIDUtil.uuidToIntArray(cube));
        entity.store("Pos", Vec3.CODEC, new Vec3(BASE + 8.5, 100, BASE + 8.5));
        entity.store("Motion", Vec3.CODEC, Vec3.ZERO);
        entity.store("Rotation", Vec2.CODEC, Vec2.ZERO);
        entity.putString("CustomName", "{\"text\":\"Legacy Cube\"}");
        entity.putFloat("Health", 20);
        var entities = new ListTag();
        entities.add(entity);
        var chamberSign = new CompoundTag();
        chamberSign.putString("id", "portalmod:chamber_sign");
        chamberSign.putIntArray("UUID", UUIDUtil.uuidToIntArray(sign));
        chamberSign.store("Pos", Vec3.CODEC, new Vec3(BASE + 13, 102.5, BASE + 3.0625));
        chamberSign.store("Motion", Vec3.CODEC, Vec3.ZERO);
        chamberSign.store("Rotation", Vec2.CODEC, Vec2.ZERO);
        chamberSign.putInt("TileX", BASE + 12);
        chamberSign.putInt("TileY", 102);
        chamberSign.putInt("TileZ", BASE + 3);
        chamberSign.putByte("Facing", (byte) Direction.SOUTH.get2DDataValue());
        chamberSign.putInt("LeftDigit", 2);
        chamberSign.putInt("RightDigit", 6);
        chamberSign.putInt("Progress", 4);
        chamberSign.putBoolean("Icon2", true);
        chamberSign.putBoolean("Icon8", true);
        chamberSign.putBoolean("Aligned", true);
        entities.add(chamberSign);
        level.put("Entities", entities);
        return root;
    }

    private static CompoundTag state(String name, String... properties) {
        var state = new CompoundTag();
        state.putString("Name", name);
        if (properties.length > 0) {
            var values = new CompoundTag();
            for (int i = 0; i < properties.length; i += 2) {
                values.putString(properties[i], properties[i + 1]);
            }
            state.put("Properties", values);
        }
        return state;
    }

    private static void block(long[] blocks, int x, int y, int z, int palette) {
        int index = (y & 15) * 256 + z * 16 + x;
        int shift = (index & 15) * 4;
        blocks[index >> 4] = blocks[index >> 4] & ~(15L << shift) | (long) palette << shift;
    }

    private static CompoundTag coordinates(int x, int y, int z) {
        var value = new CompoundTag();
        value.putInt("x", x);
        value.putInt("y", y);
        value.putInt("z", z);
        return value;
    }

    private static CompoundTag tile(String id, int x, int y, int z) {
        var value = coordinates(BASE + x, y, BASE + z);
        value.putString("id", id);
        return value;
    }
}
