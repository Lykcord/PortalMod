package net.portalmod.fabric.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.portalmod.fabric.portal.PortalTracking;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Treats entities reachable through an open portal pair as "in range" so they stay synced
 * to players watching them through a portal.
 */
@Mixin(targets = "net.minecraft.server.level.ChunkMap$TrackedEntity")
public abstract class EntityTrackerMixin {
    @Shadow
    @Final
    private Entity entity;

    @ModifyVariable(method = "updatePlayer", at = @At("STORE"), ordinal = 0)
    private boolean portalmod$trackThroughPortals(boolean shouldTrack, ServerPlayer player) {
        if (shouldTrack) {
            return true;
        }

        return PortalTracking.shouldTrackThroughPortal(entity, player);
    }
}
