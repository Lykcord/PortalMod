package net.portalmod.fabric.portal;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * Extends vanilla entity tracking so entities visible through an open portal pair are
 * synced to players standing on the other side, mirroring the Forge EntityTrackerMixin.
 */
public final class PortalTracking {
    private static final double ENTITY_PORTAL_RANGE = 64.0D;
    private static final double PLAYER_PORTAL_RANGE = 64.0D;

    private PortalTracking() {
    }

    public static boolean shouldTrackThroughPortal(Entity entity, ServerPlayer player) {
        if (entity.level() != player.level() || !(entity.level() instanceof ServerLevel level)) {
            return false;
        }

        PortalManager manager = PortalManager.get(level.getServer());
        if (manager.pairs().isEmpty()) {
            return false;
        }

        Vec3 entityPos = entity.position();
        Vec3 playerPos = player.position();
        String dimension = level.dimension().identifier().toString();

        for (PortalPairRecord pair : manager.pairs().values()) {
            if (!pair.isComplete()) {
                continue;
            }

            Optional<PortalRecord> primary = pair.primary();
            Optional<PortalRecord> secondary = pair.secondary();
            PortalRecord a = primary.orElseThrow();
            PortalRecord b = secondary.orElseThrow();
            if (!a.dimension().equals(dimension) || !b.dimension().equals(dimension)) {
                continue;
            }

            if (linksPositions(a, b, entityPos, playerPos) || linksPositions(b, a, entityPos, playerPos)) {
                return true;
            }
        }

        return false;
    }

    private static boolean linksPositions(PortalRecord nearEntity, PortalRecord nearPlayer, Vec3 entityPos, Vec3 playerPos) {
        return nearEntity.position().distanceToSqr(entityPos) <= ENTITY_PORTAL_RANGE * ENTITY_PORTAL_RANGE
                && nearPlayer.position().distanceToSqr(playerPos) <= PLAYER_PORTAL_RANGE * PLAYER_PORTAL_RANGE;
    }
}
