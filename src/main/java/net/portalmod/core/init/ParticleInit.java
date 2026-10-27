package net.portalmod.core.init;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.portalmod.PortalMod;
import net.portalmod.common.particles.*;
import net.portalmod.common.sorted.portal.PortalParticle;
import net.portalmod.common.sorted.portal.PortalPhotonParticle;
import net.portalmod.core.util.Registry;
import net.portalmod.core.util.Registry.Entry;

public class ParticleInit {
    public static final Registry<ParticleType<?>> PARTICLE_TYPES =
            new Registry<>(
                    (id, value) ->
                            net.minecraft.core.Registry.register(
                                    BuiltInRegistries.PARTICLE_TYPE,
                                    Identifier.fromNamespaceAndPath(PortalMod.MODID, id),
                                    value));
    public static final Entry<ParticleType<PortalParticleOptions>> PORTAL_PARTICLE =
            PARTICLE_TYPES.register(
                    "portal_particle",
                    () ->
                            FabricParticleTypes.complex(
                                    PortalParticleOptions.CODEC,
                                    ByteBufCodecs.fromCodecWithRegistries(PortalParticleOptions.CODEC.codec())));
    public static final Entry<ParticleType<PortalPhotonOptions>> PORTAL_PHOTON =
            PARTICLE_TYPES.register(
                    "portal_photon",
                    () ->
                            FabricParticleTypes.complex(
                                    PortalPhotonOptions.CODEC,
                                    ByteBufCodecs.fromCodecWithRegistries(PortalPhotonOptions.CODEC.codec())));
    public static final Entry<SimpleParticleType> FIZZLE_GLOW =
            PARTICLE_TYPES.register("fizzle_glow", () -> FabricParticleTypes.simple(false));
    public static final Entry<SimpleParticleType> FIZZLE_FLAKE_FALLING =
            PARTICLE_TYPES.register("fizzle_flake_falling", () -> FabricParticleTypes.simple(false));
    public static final Entry<SimpleParticleType> FIZZLE_FLAKE_LANDING =
            PARTICLE_TYPES.register("fizzle_flake_landing", () -> FabricParticleTypes.simple(false));
    public static final Entry<SimpleParticleType> PORTALGUN_SPARK =
            PARTICLE_TYPES.register("portalgun_spark", () -> FabricParticleTypes.simple(false));
    public static final Entry<SimpleParticleType> TURRET_SPARK =
            PARTICLE_TYPES.register("turret_spark", () -> FabricParticleTypes.simple(false));

    // Backport of the small flame for candles, this should get replaced with the vanilla one after porting to a newer
    // version
    public static final Entry<SimpleParticleType> SMALL_FLAME =
            PARTICLE_TYPES.register("small_flame", () -> FabricParticleTypes.simple(false));

    private ParticleInit() {}

    @Environment(EnvType.CLIENT)
    public static void registerParticleFactory() {
        ParticleProviderRegistry particleEngine = ParticleProviderRegistry.getInstance();
        particleEngine.register(ParticleInit.PORTAL_PARTICLE.get(), PortalParticle.Factory::new);
        particleEngine.register(ParticleInit.PORTAL_PHOTON.get(), PortalPhotonParticle.Factory::new);
        particleEngine.register(ParticleInit.FIZZLE_GLOW.get(), FizzleGlowParticle.Factory::new);
        particleEngine.register(ParticleInit.FIZZLE_FLAKE_FALLING.get(), FizzleFlakeParticle.FallingFactory::new);
        particleEngine.register(ParticleInit.FIZZLE_FLAKE_LANDING.get(), FizzleFlakeParticle.LandingFactory::new);
        particleEngine.register(ParticleInit.PORTALGUN_SPARK.get(), PortalGunSparkParticle.Factory::new);
        particleEngine.register(ParticleInit.TURRET_SPARK.get(), TurretSparkParticle.Factory::new);
        particleEngine.register(ParticleInit.SMALL_FLAME.get(), SmallFlameFactory::new);
    }
}
