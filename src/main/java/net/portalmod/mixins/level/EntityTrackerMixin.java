package net.portalmod.mixins.level;

import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.portalmod.common.sorted.portal.PortalEnd;
import net.portalmod.common.sorted.portal.PortalEntity;
import net.portalmod.common.sorted.portal.PortalManager;
import net.portalmod.mixins.accessors.ChunkManagerAccessor;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(targets = "net.minecraft.server.level.ChunkMap$TrackedEntity")
public abstract class EntityTrackerMixin {
    @Shadow @Final private Entity entity;

    @Shadow
    protected abstract int getEffectiveRange();

    @ModifyVariable(method = "updatePlayer", at = @At("STORE"), ordinal = 0)
    private boolean portalmod$trackThroughPortals(boolean vanilla, @Local(argsOnly = true) ServerPlayer player) {
        if (vanilla || !entity.broadcastToPlayer(player) || entity.level() != player.level()) {
            return vanilla;
        }
        var chunks = ((ServerChunkCache) entity.level().getChunkSource()).chunkMap;
        int viewDistance = ((ChunkManagerAccessor) chunks).portalmod$getPlayerViewDistance(player);
        int range = Math.min(getEffectiveRange(), viewDistance * 16);
        for (var pair : PortalManager.getInstance().getPortalMap().values()) {
            for (PortalEnd end : new PortalEnd[] {PortalEnd.PRIMARY, PortalEnd.SECONDARY}) {
                PortalEntity portal = pair.get(end);
                if (portal == null || portal.isRemoved() || !portal.isOpen() || portal.level() != player.level()) {
                    continue;
                }
                var portalChunk = portal.chunkPosition();
                if (!chunks.isChunkTracked(player, portalChunk.x(), portalChunk.z())) {
                    continue;
                }
                if (!portalmod$inRange(player.position().subtract(portal.position()), viewDistance * 16)) {
                    continue;
                }
                var destination = portal.getOtherPortal().orElse(null);
                if (destination == null || destination.level() != entity.level()) {
                    continue;
                }
                Vec3 transformed = portal.teleportPoint(new net.portalmod.core.math.Vec3(player.position())).to3d();
                if (portalmod$inRange(transformed.subtract(entity.position()), range)
                        && entity.chunkPosition()
                                        .getChessboardDistance(ChunkPos.containing(BlockPos.containing(transformed)))
                                <= viewDistance) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean portalmod$inRange(Vec3 delta, int range) {
        return Math.abs(delta.x) <= range && Math.abs(delta.z) <= range;
    }
}
