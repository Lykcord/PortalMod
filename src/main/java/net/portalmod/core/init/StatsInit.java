package net.portalmod.core.init;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.stats.StatFormatter;
import net.minecraft.stats.Stats;
import net.portalmod.PortalMod;

public class StatsInit {
    private StatsInit() {}

    public static void init() {}

    public static final Identifier PORTALS_SHOT = makeCustomStat("portals_shot", StatFormatter.DEFAULT);

    private static Identifier makeCustomStat(String name, StatFormatter formatter) {
        Identifier resourcelocation = Identifier.fromNamespaceAndPath(PortalMod.MODID, name);
        Registry.register(BuiltInRegistries.CUSTOM_STAT, resourcelocation, resourcelocation);
        Stats.CUSTOM.get(resourcelocation, formatter);
        return resourcelocation;
    }
}
