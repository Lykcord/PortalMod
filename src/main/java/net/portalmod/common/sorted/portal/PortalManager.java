package net.portalmod.common.sorted.portal;

import com.mojang.datafixers.util.Pair;

import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.TagValueInput;
import net.portalmod.PortalMod;
import net.portalmod.core.init.PacketInit;
import net.portalmod.core.math.Vec3;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class PortalManager extends SavedData {
    private static PortalManager instance;
    public static final String PATH = PortalMod.MODID + "_portals";
    public static final SavedDataType<PortalManager> TYPE =
            new SavedDataType<>(
                    Identifier.fromNamespaceAndPath(PortalMod.MODID, "portals"),
                    PortalManager::new,
                    CompoundTag.CODEC.xmap(PortalManager::new, PortalManager::savePortals),
                    null);
    private final CompoundTag unresolved;
    private final Map<UUID, PortalPair> portalMap = new HashMap<>();
    private final Map<Pair<UUID, PortalEnd>, PortalPlacementInfo> scheduledPlacements = new HashMap<>();
    private final HashMap<ResourceKey<Level>, HashMap<ChunkPos, List<PortalEntity>>> portalsPerChunk = new HashMap<>();
    private final Deque<PortalEntity> pendingRemovals = new ArrayDeque<>();
    public boolean unloadingChunk = false;

    private PortalManager() {
        this(new CompoundTag());
    }

    private PortalManager(CompoundTag saved) {
        this.unresolved = saved.copy();
    }

    public static PortalManager getInstance() {
        if (instance == null) {
            instance = new PortalManager();
        }
        return instance;
    }

    public void tick() {
        for (PortalEntity portal : this.pendingRemovals) {
            portal.discard();
        }
        this.pendingRemovals.clear();

        List<Pair<UUID, PortalEnd>> completedPlacements = new ArrayList<>();
        this.scheduledPlacements.forEach(
                (key, info) -> {
                    if (--info.tickCountdown <= 0) {
                        PortalEntity portal =
                                PortalPlacer.placePortal(
                                        info.level,
                                        info.end,
                                        info.hue,
                                        info.gunUUID,
                                        info.position,
                                        info.face,
                                        info.upDirection,
                                        info.override,
                                        info.overwriteForeignPortals,
                                        info.lookingDirections,
                                        info.player);
                        info.onPlace.accept(portal);

                        completedPlacements.add(key);
                    }
                });
        completedPlacements.forEach(this.scheduledPlacements::remove);
    }

    public void scheduleRemoval(PortalEntity portal) {
        this.pendingRemovals.add(portal);
    }

    public void schedulePlacement(
            Level level,
            PortalEnd end,
            String hue,
            UUID gunUUID,
            Vec3 position,
            Direction face,
            Direction upDirection,
            boolean override,
            @Nullable Direction[] lookingDirections,
            @Nullable ServerPlayer player,
            long tickCountdown,
            Consumer<PortalEntity> onPlace) {
        schedulePlacement(
                level,
                end,
                hue,
                gunUUID,
                position,
                face,
                upDirection,
                override,
                false,
                lookingDirections,
                player,
                tickCountdown,
                onPlace);
    }

    public void schedulePlacement(
            Level level,
            PortalEnd end,
            String hue,
            UUID gunUUID,
            Vec3 position,
            Direction face,
            Direction upDirection,
            boolean override,
            boolean overwriteForeignPortals,
            @Nullable Direction[] lookingDirections,
            @Nullable ServerPlayer player,
            long tickCountdown,
            Consumer<PortalEntity> onPlace) {
        Pair<UUID, PortalEnd> key = new Pair<>(gunUUID, end);
        this.scheduledPlacements.put(
                key,
                new PortalPlacementInfo(
                        level,
                        end,
                        hue,
                        gunUUID,
                        position,
                        face,
                        upDirection,
                        override,
                        overwriteForeignPortals,
                        lookingDirections,
                        player,
                        tickCountdown,
                        onPlace));
    }

    public void clearScheduledPlacements(UUID gunUUID) {
        List<Pair<UUID, PortalEnd>> removedScheduledPlacements =
                this.scheduledPlacements.keySet().stream()
                        .filter(key -> key.getFirst().equals(gunUUID))
                        .collect(Collectors.toList());
        removedScheduledPlacements.forEach(this.scheduledPlacements::remove);
    }

    public static void start(MinecraftServer server) {
        var storage = server.getDataStorage();
        instance = storage.get(TYPE);
        if (instance == null) {
            var dataDirectory = server.getWorldPath(LevelResource.DATA);
            var currentFile = TYPE.id().withSuffix(".dat").resolveAgainst(dataDirectory);
            if (Files.exists(currentFile)) {
                throw new IllegalStateException("Cannot read saved portals: " + currentFile);
            }
            var legacyFile = dataDirectory.resolve(PATH + ".dat");
            if (Files.exists(legacyFile)) {
                try {
                    CompoundTag root =
                            storage.readTagFromDisk(
                                    legacyFile, null, SharedConstants.getCurrentVersion().dataVersion().version());
                    instance =
                            new PortalManager(
                                    root.getCompound("data")
                                            .orElseThrow(
                                                    () ->
                                                            new IllegalStateException(
                                                                    "Missing portal data in " + legacyFile)));
                } catch (IOException error) {
                    throw new IllegalStateException("Cannot read legacy portals: " + legacyFile, error);
                }
            } else {
                instance = new PortalManager();
            }
            storage.set(TYPE, instance);
        }
        instance.loadPortals(server);
    }

    public static void stop() {
        instance = null;
    }

    private void loadPortals(MinecraftServer server) {
        CompoundTag saved = unresolved.copy();
        for (String key : saved.keySet()) {
            UUID uuid;
            try {
                uuid = UUID.fromString(key);
            } catch (IllegalArgumentException error) {
                PortalMod.LOGGER.warn("Ignoring invalid saved portal UUID {}", key);
                continue;
            }
            CompoundTag pairTag = saved.getCompoundOrEmpty(key);
            PortalPair pair = new PortalPair();
            for (PortalEnd end : new PortalEnd[] {PortalEnd.PRIMARY, PortalEnd.SECONDARY}) {
                String name = end.getSerializedName();
                if (!pairTag.contains(name)) {
                    continue;
                }
                CompoundTag entityTag = pairTag.getCompoundOrEmpty(name);
                try {
                    Identifier dimension = Identifier.parse(entityTag.getStringOr("level", ""));
                    ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
                    if (level == null) {
                        PortalMod.LOGGER.warn("Keeping portal {} in unavailable dimension {}", uuid, dimension);
                        continue;
                    }
                    PortalEntity portal = new PortalEntity(level);
                    portal.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), entityTag));
                    pair.set(end, portal);
                    portalsPerChunk
                            .computeIfAbsent(level.dimension(), ignored -> new HashMap<>())
                            .computeIfAbsent(portal.chunkPosition(), ignored -> new ArrayList<>())
                            .add(portal);
                    if (level.hasChunkAt(portal.blockPosition())) {
                        level.addFreshEntity(portal);
                    }
                    unresolved.getCompoundOrEmpty(key).remove(name);
                } catch (RuntimeException error) {
                    PortalMod.LOGGER.error("Keeping unreadable saved portal {} {}", uuid, name, error);
                }
            }
            if (!pair.isEmpty()) {
                portalMap.put(uuid, pair);
            }
            if (unresolved.getCompoundOrEmpty(key).isEmpty()) {
                unresolved.remove(key);
            }
        }
    }

    private CompoundTag savePortals() {
        CompoundTag saved = unresolved.copy();
        portalMap.forEach(
                (uuid, pair) -> {
                    CompoundTag pairTag = saved.getCompoundOrEmpty(uuid.toString());
                    for (PortalEnd end : new PortalEnd[] {PortalEnd.PRIMARY, PortalEnd.SECONDARY}) {
                        if (!pair.has(end)) {
                            continue;
                        }
                        PortalEntity portal = pair.get(end);
                        CompoundTag entityTag = new CompoundTag();
                        portal.saveGlobal(entityTag);
                        entityTag.putString("level", portal.level().dimension().identifier().toString());
                        pairTag.put(end.getSerializedName(), entityTag);
                    }
                    saved.put(uuid.toString(), pairTag);
                });
        return saved;
    }

    private static void getStorageAndSetDirty() {
        getInstance().setDirty();
    }

    public HashMap<ResourceKey<Level>, HashMap<ChunkPos, List<PortalEntity>>> getPortalsPerChunk() {
        return portalsPerChunk;
    }

    public void put(UUID gunUUID, PortalEnd end, PortalEntity portal) {
        PortalPair pair = portalMap.getOrDefault(gunUUID, new PortalPair());

        if (!pair.areInSameDimension(PortalEnd.PRIMARY, portal)) {
            PortalEntity primary = pair.get(PortalEnd.PRIMARY);
            primary.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
        }

        if (!pair.areInSameDimension(PortalEnd.SECONDARY, portal)) {
            PortalEntity secondary = pair.get(PortalEnd.SECONDARY);
            secondary.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
        }

        pair.computeIfPresent(end, PortalEntity::onReplaced);
        pair.set(end, portal);
        portalMap.put(gunUUID, pair);

        ChunkPos chunkPos = new ChunkPos(Mth.floor(portal.getX()) >> 4, Mth.floor(portal.getZ()) >> 4);
        HashMap<ChunkPos, List<PortalEntity>> chunks =
                portalsPerChunk.getOrDefault(portal.level().dimension(), new HashMap<>());
        List<PortalEntity> portals = chunks.getOrDefault(chunkPos, new ArrayList<>());
        portals.add(portal);
        chunks.put(chunkPos, portals);
        portalsPerChunk.put(portal.level().dimension(), chunks);

        PacketInit.sendToAll(new SPortalPairPacket(gunUUID, new PartialPortalPair(pair)));

        getStorageAndSetDirty();
    }

    public void remove(UUID gunUUID, PortalEntity portal) {
        portalMap.computeIfPresent(
                gunUUID,
                (uuid, pair) -> {
                    pair.remove(portal);

                    ResourceKey<Level> dimension = portal.level().dimension();
                    ChunkPos chunkPos = new ChunkPos(Mth.floor(portal.getX()) >> 4, Mth.floor(portal.getZ()) >> 4);
                    if (portalsPerChunk.containsKey(dimension)) {
                        if (portalsPerChunk.get(dimension).containsKey(chunkPos)) {
                            List<PortalEntity> portals = portalsPerChunk.get(dimension).get(chunkPos);
                            portals.remove(portal);
                            if (portals.isEmpty()) {
                                portalsPerChunk.get(dimension).remove(chunkPos);
                            }
                        }
                    }

                    getStorageAndSetDirty();
                    return !pair.isEmpty() ? pair : null;
                });

        PacketInit.sendToAll(
                new SPortalPairPacket(
                        gunUUID, new PartialPortalPair(portalMap.getOrDefault(gunUUID, new PortalPair()))));
    }

    public boolean has(UUID gunUUID, PortalEnd end) {
        return portalMap.containsKey(gunUUID) && portalMap.get(gunUUID).has(end);
    }

    @Nullable
    public PortalEntity get(UUID gunUUID, PortalEnd end) {
        return portalMap.containsKey(gunUUID) ? portalMap.get(gunUUID).get(end) : null;
    }

    @Nullable
    public PortalPair getPair(UUID gunUUID) {
        return portalMap.getOrDefault(gunUUID, null);
    }

    public Map<UUID, PortalPair> getPortalMap() {
        return portalMap;
    }

    private static class PortalPlacementInfo {
        public final Level level;
        public final PortalEnd end;
        public final String hue;
        public final UUID gunUUID;
        public final Vec3 position;
        public final Direction face;
        public final Direction upDirection;
        public final boolean override;
        public final boolean overwriteForeignPortals;
        public final @Nullable Direction[] lookingDirections;
        public final @Nullable ServerPlayer player;
        public long tickCountdown;
        public Consumer<PortalEntity> onPlace;

        public PortalPlacementInfo(
                Level level,
                PortalEnd end,
                String hue,
                UUID gunUUID,
                Vec3 position,
                Direction face,
                Direction upDirection,
                boolean override,
                boolean overwriteForeignPortals,
                @Nullable Direction[] lookingDirections,
                @Nullable ServerPlayer player,
                long tickCountdown,
                Consumer<PortalEntity> onPlace) {
            this.level = level;
            this.end = end;
            this.hue = hue;
            this.gunUUID = gunUUID;
            this.position = position;
            this.face = face;
            this.upDirection = upDirection;
            this.override = override;
            this.overwriteForeignPortals = overwriteForeignPortals;
            this.lookingDirections = lookingDirections;
            this.player = player;
            this.tickCountdown = tickCountdown;
            this.onPlace = onPlace;
        }
    }
}
