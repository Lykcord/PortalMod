package net.portalmod.core.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import net.portalmod.PortalMod;

public final class DamageTypeInit {
    public static final ResourceKey<DamageType> GOO = key("goo");
    public static final ResourceKey<DamageType> FIZZLE = key("fizzle");
    public static final ResourceKey<DamageType> CUBE = key("cube");
    public static final ResourceKey<DamageType> TURRET = key("turret");
    public static final ResourceKey<DamageType> FAITHPLATE_WALL = key("faithplate_wall");

    private DamageTypeInit() {}

    private static ResourceKey<DamageType> key(String name) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath(PortalMod.MODID, name));
    }
}
