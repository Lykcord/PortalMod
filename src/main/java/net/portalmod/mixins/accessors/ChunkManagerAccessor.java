package net.portalmod.mixins.accessors;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ChunkMap.class)
public interface ChunkManagerAccessor {
    @Invoker("getPlayerViewDistance")
    int portalmod$getPlayerViewDistance(ServerPlayer player);
}
