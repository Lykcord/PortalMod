package net.portalmod.mixins.accessors;

import net.minecraft.client.renderer.state.GameRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GameRenderState.class)
public interface GameRenderStateAccessor {
    @Mutable
    @Accessor("levelRenderState")
    void pmLevelState(LevelRenderState state);
}
