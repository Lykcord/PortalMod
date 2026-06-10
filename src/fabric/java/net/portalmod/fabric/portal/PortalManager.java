package net.portalmod.fabric.portal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.portalmod.fabric.PortalModFabric;
import net.portalmod.fabric.entity.PortalEntity;
import net.portalmod.fabric.network.PortalPairPayload;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Server-side registry of all portal pairs keyed by portal gun UUID. Persisted with the
 * overworld so pairs survive restarts even when their entities sit in unloaded chunks, and
 * synced to all clients so rendering/teleport logic can resolve pairs across dimensions.
 */
public final class PortalManager extends SavedData {
    private static final Codec<Map<UUID, PortalPairRecord>> MAP_CODEC =
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, PortalPairRecord.CODEC);

    public static final Codec<PortalManager> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            MAP_CODEC.optionalFieldOf("pairs", Map.of()).forGetter(manager -> manager.pairs),
            UUIDUtil.CODEC_SET.optionalFieldOf("revoked", Set.of()).forGetter(manager -> manager.revoked)
    ).apply(instance, PortalManager::new));

    public static final SavedDataType<PortalManager> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(PortalModFabric.MOD_ID, "portals"),
            PortalManager::new,
            CODEC,
            null
    );

    private final Map<UUID, PortalPairRecord> pairs = new HashMap<>();
    private final Set<UUID> revoked = new HashSet<>();

    public PortalManager() {
    }

    private PortalManager(Map<UUID, PortalPairRecord> pairs, Set<UUID> revoked) {
        this.pairs.putAll(pairs);
        this.revoked.addAll(revoked);
    }

    public static PortalManager get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public Map<UUID, PortalPairRecord> pairs() {
        return pairs;
    }

    public PortalPairRecord pair(UUID gunId) {
        return pairs.getOrDefault(gunId, PortalPairRecord.EMPTY);
    }

    public Optional<PortalRecord> end(UUID gunId, boolean primary) {
        return pair(gunId).end(primary);
    }

    public boolean hasPair(UUID gunId) {
        return pair(gunId).isComplete();
    }

    public void put(MinecraftServer server, UUID gunId, boolean primary, PortalRecord record) {
        PortalPairRecord updated = pair(gunId).withEnd(primary, Optional.of(record));
        pairs.put(gunId, updated);
        setDirty();
        broadcast(server, gunId, updated);
    }

    public void remove(MinecraftServer server, UUID gunId, boolean primary, UUID entityUuid) {
        PortalPairRecord current = pair(gunId);
        Optional<PortalRecord> end = current.end(primary);
        if (end.isEmpty() || !end.get().entityUuid().equals(entityUuid)) {
            return;
        }

        PortalPairRecord updated = current.withEnd(primary, Optional.empty());
        if (updated.isEmpty()) {
            pairs.remove(gunId);
        } else {
            pairs.put(gunId, updated);
        }
        setDirty();
        broadcast(server, gunId, updated);
    }

    /**
     * Self-heals records for portal entities loaded from worlds saved before the manager
     * existed (or after manual NBT edits).
     */
    public void ensureRegistered(MinecraftServer server, PortalEntity portal) {
        Optional<PortalRecord> end = end(portal.gunId(), portal.primary());
        if (end.isEmpty() || !end.get().entityUuid().equals(portal.getUUID())) {
            put(server, portal.gunId(), portal.primary(), PortalRecord.of(portal));
        }
    }

    /**
     * Marks an unloaded portal entity for removal; the entity discards itself the next time
     * it ticks. Used by /portal close when the target chunk is not loaded.
     */
    public void revoke(MinecraftServer server, UUID gunId, boolean primary) {
        end(gunId, primary).ifPresent(record -> {
            revoked.add(record.entityUuid());
            PortalPairRecord updated = pair(gunId).withEnd(primary, Optional.empty());
            if (updated.isEmpty()) {
                pairs.remove(gunId);
            } else {
                pairs.put(gunId, updated);
            }
            setDirty();
            broadcast(server, gunId, updated);
        });
    }

    public boolean isRevoked(UUID entityUuid) {
        return revoked.contains(entityUuid);
    }

    public void clearRevoked(UUID entityUuid) {
        if (revoked.remove(entityUuid)) {
            setDirty();
        }
    }

    public PortalEntity resolve(MinecraftServer server, PortalRecord record) {
        ServerLevel level = server.getLevel(record.dimensionKey());
        if (level == null) {
            return null;
        }

        Entity entity = level.getEntity(record.entityUuid());
        return entity instanceof PortalEntity portal && !portal.isRemoved() ? portal : null;
    }

    public ServerLevel level(MinecraftServer server, PortalRecord record) {
        return server.getLevel(record.dimensionKey());
    }

    private void broadcast(MinecraftServer server, UUID gunId, PortalPairRecord pair) {
        PortalPairPayload payload = new PortalPairPayload(gunId, pair);
        for (ServerPlayer player : PlayerLookup.all(server)) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    public void syncAllTo(ServerPlayer player) {
        pairs.forEach((gunId, pair) -> ServerPlayNetworking.send(player, new PortalPairPayload(gunId, pair)));
    }
}
