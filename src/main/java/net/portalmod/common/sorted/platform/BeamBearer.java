package net.portalmod.common.sorted.platform;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import org.jspecify.annotations.Nullable;

public interface BeamBearer {
    @Nullable Direction getBeamDirection(BlockState state);
}
