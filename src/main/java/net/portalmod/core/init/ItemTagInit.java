package net.portalmod.core.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.portalmod.PortalMod;

public class ItemTagInit {
    private ItemTagInit() {}

    public static void init() {}

    public static final TagKey<Item> LUNECAST = itemTag("lunecast");
    public static final TagKey<Item> BLACKPLATE = itemTag("blackplate");
    public static final TagKey<Item> CUBES = itemTag("cubes");
    public static final TagKey<Item> INDICATORS = itemTag("indicators");
    public static final TagKey<Item> TESTING_ELEMENTS = itemTag("testing_elements");
    public static final TagKey<Item> IRON_FRAMES = itemTag("iron_frames");
    public static final TagKey<Item> CHAMBER_DECORATION = itemTag("chamber_decoration");
    public static final TagKey<Item> PRESSURE_PLATES = itemTag("pressure_plates");
    public static final TagKey<Item> BUTTONS =
            TagKey.create(Registries.ITEM, Identifier.withDefaultNamespace("buttons"));

    public static TagKey<Item> itemTag(String name) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(PortalMod.MODID, name));
    }
}
