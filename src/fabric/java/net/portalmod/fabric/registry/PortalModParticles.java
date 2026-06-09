package net.portalmod.fabric.registry;

import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.portalmod.fabric.PortalModFabric;

import java.util.LinkedHashMap;
import java.util.Map;

public final class PortalModParticles {
    private static final Map<String, SimpleParticleType> PARTICLES = new LinkedHashMap<>();

    public static final SimpleParticleType PORTAL_PARTICLE = register("portal_particle");
    public static final SimpleParticleType PORTAL_PHOTON = register("portal_photon");
    public static final SimpleParticleType FIZZLE_GLOW = register("fizzle_glow");
    public static final SimpleParticleType FIZZLE_FLAKE_FALLING = register("fizzle_flake_falling");
    public static final SimpleParticleType FIZZLE_FLAKE_LANDING = register("fizzle_flake_landing");
    public static final SimpleParticleType PORTALGUN_SPARK = register("portalgun_spark");
    public static final SimpleParticleType TURRET_SPARK = register("turret_spark");
    public static final SimpleParticleType SMALL_FLAME = register("small_flame");

    private PortalModParticles() {
    }

    public static void register() {
        PortalModFabric.LOGGER.info("Registered {} PortalMod particle types.", PARTICLES.size());
    }

    private static SimpleParticleType register(String name) {
        Identifier id = Identifier.fromNamespaceAndPath(PortalModFabric.MOD_ID, name);
        SimpleParticleType particle = FabricParticleTypes.simple(false);
        PARTICLES.put(name, particle);
        return Registry.register(BuiltInRegistries.PARTICLE_TYPE, id, particle);
    }
}
