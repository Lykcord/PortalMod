package net.portalmod.fabric.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.portalmod.fabric.PortalModFabric;
import net.portalmod.fabric.component.PortalGunState;
import net.portalmod.fabric.item.PortalGunItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class PortalModItems {
    private static final List<Item> CREATIVE_TAB_ITEMS = new ArrayList<>();

    public static final Item PORTALGUN = registerItem("portalgun", properties -> new PortalGunItem(properties),
            new Item.Properties()
                    .stacksTo(1)
                    .fireResistant()
                    .rarity(Rarity.RARE)
                    .component(PortalModDataComponents.PORTAL_GUN_STATE, PortalGunState.DEFAULT));
    public static final Item WRENCH = registerItem("wrench", new Item.Properties().stacksTo(1));
    public static final Item BULLETS = registerItem("bullets", new Item.Properties());
    public static final Item CONTAINER = registerItem("container", new Item.Properties().stacksTo(16));
    public static final Item ANTLINE = registerItem("antline", new Item.Properties());
    public static final Item ANTLINE_DECODER = registerItem("antline_decoder", new Item.Properties());
    public static final Item ANTLINE_ENCODER = registerItem("antline_encoder", new Item.Properties());
    public static final Item ANTLINE_INDICATOR = registerItem("antline_indicator", new Item.Properties());
    public static final Item ANTLINE_TIMER = registerItem("antline_timer", new Item.Properties());
    public static final Item AUTOPORTAL = registerItem("autoportal", new Item.Properties());
    public static final Item CHAMBER_DOOR = registerItem("chamber_door", new Item.Properties());
    public static final Item CHAMBER_LIGHTS = registerItem("chamber_lights", new Item.Properties());
    public static final Item CHAMBER_SIGN = registerItem("chamber_sign", new Item.Properties());
    public static final Item COMPANION_CUBE = registerItem("companion_cube", new Item.Properties());
    public static final Item CUBE_DROPPER = registerItem("cube_dropper", new Item.Properties());
    public static final Item FAITHPLATE = registerItem("faithplate", new Item.Properties());
    public static final Item FIZZLER_EMITTER = registerItem("fizzler_emitter", new Item.Properties());
    public static final Item FIZZLER_FIELD = registerItem("fizzler_field", new Item.Properties());
    public static final Item FOREST_CAKE = registerItem("forest_cake", new Item.Properties());
    public static final Item GABE = registerItem("gabe", new Item.Properties());
    public static final Item GOO_BUCKET = registerItem("goo_bucket", new Item.Properties().stacksTo(1));
    public static final Item LONGFALL_BOOTS = registerItem("longfall_boots", new Item.Properties().stacksTo(1));
    public static final Item MUSIC_DISC_RAIN = registerItem("music_disc_rain", new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
    public static final Item PROPULSION_GEL = registerItem("propulsion_gel", new Item.Properties());
    public static final Item PUSH_DOOR = registerItem("push_door", new Item.Properties());
    public static final Item RADIO = registerItem("radio", new Item.Properties());
    public static final Item REPULSION_GEL = registerItem("repulsion_gel", new Item.Properties());
    public static final Item STANDING_BUTTON = registerItem("standing_button", new Item.Properties());
    public static final Item STORAGE_CUBE = registerItem("storage_cube", new Item.Properties());
    public static final Item SUPER_BUTTON = registerItem("super_button", new Item.Properties());
    public static final Item TRIGGER = registerItem("trigger", new Item.Properties());
    public static final Item TURRET = registerItem("turret", new Item.Properties());
    public static final Item VINTAGE_CUBE = registerItem("vintage_cube", new Item.Properties());
    public static final Item TEST_BLOCK = registerItem("test_block", new Item.Properties());

    public static final CreativeModeTab CREATIVE_TAB = Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB,
            id("portalmod"),
            CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                    .title(Component.translatable("itemGroup.portalmod"))
                    .icon(() -> new ItemStack(PortalModBlocks.LUNECAST))
                    .displayItems((parameters, output) -> CREATIVE_TAB_ITEMS.forEach(output::accept))
                    .build()
    );

    private PortalModItems() {
    }

    public static void register() {
        for (Map.Entry<String, Block> entry : PortalModBlocks.decorativeBlocks()) {
            registerBlockItem(entry.getKey(), entry.getValue());
        }

        PortalModFabric.LOGGER.info("Registered {} PortalMod Fabric items.", CREATIVE_TAB_ITEMS.size());
    }

    private static Item registerBlockItem(String name, Block block) {
        Item item = new BlockItem(block, itemProperties(name).useBlockDescriptionPrefix());
        CREATIVE_TAB_ITEMS.add(item);
        return Registry.register(BuiltInRegistries.ITEM, id(name), item);
    }

    private static Item registerItem(String name, Item.Properties properties) {
        return registerItem(name, Item::new, properties);
    }

    private static Item registerItem(String name, java.util.function.Function<Item.Properties, Item> factory, Item.Properties properties) {
        Item item = factory.apply(properties.setId(itemKey(name)));
        CREATIVE_TAB_ITEMS.add(item);
        return Registry.register(BuiltInRegistries.ITEM, id(name), item);
    }

    private static Item.Properties itemProperties(String name) {
        return new Item.Properties().setId(itemKey(name));
    }

    private static ResourceKey<Item> itemKey(String name) {
        return ResourceKey.create(Registries.ITEM, id(name));
    }

    private static Identifier id(String name) {
        return Identifier.fromNamespaceAndPath(PortalModFabric.MOD_ID, name);
    }
}
