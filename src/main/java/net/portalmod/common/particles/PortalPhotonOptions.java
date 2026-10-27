package net.portalmod.common.particles;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.portalmod.core.init.ParticleInit;

public record PortalPhotonOptions(
        int portalId, float r, float g, float b, float speed, float decay, boolean smooth, boolean useGravity)
        implements ParticleOptions {
    public static final MapCodec<PortalPhotonOptions> CODEC =
            RecordCodecBuilder.mapCodec(
                    instance ->
                            instance.group(
                                            Codec.INT
                                                    .optionalFieldOf("portal_id", -1)
                                                    .forGetter(PortalPhotonOptions::portalId),
                                            Codec.floatRange(0, 1)
                                                    .optionalFieldOf("r", 1F)
                                                    .forGetter(PortalPhotonOptions::r),
                                            Codec.floatRange(0, 1)
                                                    .optionalFieldOf("g", 1F)
                                                    .forGetter(PortalPhotonOptions::g),
                                            Codec.floatRange(0, 1)
                                                    .optionalFieldOf("b", 1F)
                                                    .forGetter(PortalPhotonOptions::b),
                                            Codec.floatRange(.01F, 1024)
                                                    .optionalFieldOf("speed", 4F)
                                                    .forGetter(PortalPhotonOptions::speed),
                                            Codec.floatRange(.01F, 1024)
                                                    .optionalFieldOf("decay", 15F)
                                                    .forGetter(PortalPhotonOptions::decay),
                                            Codec.BOOL
                                                    .optionalFieldOf("smooth", false)
                                                    .forGetter(PortalPhotonOptions::smooth),
                                            Codec.BOOL
                                                    .optionalFieldOf("gravity", false)
                                                    .forGetter(PortalPhotonOptions::useGravity))
                                    .apply(instance, PortalPhotonOptions::new));

    @Override
    public ParticleType<PortalPhotonOptions> getType() {
        return ParticleInit.PORTAL_PHOTON.get();
    }
}
