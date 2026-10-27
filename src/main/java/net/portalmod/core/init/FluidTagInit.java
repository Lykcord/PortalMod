package net.portalmod.core.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.portalmod.PortalMod;

public class FluidTagInit {
    private FluidTagInit() {}

    public static void init() {}

    public static final TagKey<Fluid> GOO =
            TagKey.create(Registries.FLUID, Identifier.fromNamespaceAndPath(PortalMod.MODID, "goo"));
}
