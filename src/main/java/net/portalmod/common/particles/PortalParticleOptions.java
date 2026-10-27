package net.portalmod.common.particles;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.portalmod.core.init.ParticleInit;

public record PortalParticleOptions(float r, float g, float b, float a, Direction direction)
        implements ParticleOptions {
    public static final MapCodec<PortalParticleOptions> CODEC =
            RecordCodecBuilder.mapCodec(
                    instance ->
                            instance.group(
                                            Codec.floatRange(0, 1)
                                                    .optionalFieldOf("r", 0F)
                                                    .forGetter(PortalParticleOptions::r),
                                            Codec.floatRange(0, 1)
                                                    .optionalFieldOf("g", 0F)
                                                    .forGetter(PortalParticleOptions::g),
                                            Codec.floatRange(0, 1)
                                                    .optionalFieldOf("b", 1F)
                                                    .forGetter(PortalParticleOptions::b),
                                            Codec.floatRange(.01F, 4)
                                                    .optionalFieldOf("a", 1F)
                                                    .forGetter(PortalParticleOptions::a),
                                            Direction.CODEC
                                                    .optionalFieldOf("direction", Direction.NORTH)
                                                    .forGetter(PortalParticleOptions::direction))
                                    .apply(instance, PortalParticleOptions::new));

    @Override
    public ParticleType<PortalParticleOptions> getType() {
        return ParticleInit.PORTAL_PARTICLE.get();
    }
}
