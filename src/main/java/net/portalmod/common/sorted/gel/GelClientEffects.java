package net.portalmod.common.sorted.gel;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.RenderShape;

@Environment(EnvType.CLIENT)
public final class GelClientEffects {
    private GelClientEffects() {}

    public static void spawnSprintParticle(Entity entity) {
        if (!(entity.level() instanceof ClientLevel level)) {
            return;
        }
        var pos = BlockPos.containing(entity.getX(), entity.getY() + .8, entity.getZ());
        var state = level.getBlockState(pos);
        if (state.getRenderShape() == RenderShape.INVISIBLE) {
            return;
        }
        var velocity = entity.getDeltaMovement();
        float width = entity.getBbWidth();
        var random = level.getRandom();
        level.addParticle(
                new BlockParticleOption(ParticleTypes.BLOCK, state),
                entity.getX() + (random.nextDouble() - .5) * width,
                entity.getY() + .1,
                entity.getZ() + (random.nextDouble() - .5) * width,
                velocity.x * -4,
                1.5,
                velocity.z * -4);
    }
}
