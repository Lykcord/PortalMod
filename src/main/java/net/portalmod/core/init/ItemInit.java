package net.portalmod.core.init;

import net.minecraft.core.BlockPos;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DispenserBlock;
import net.portalmod.PortalMod;
import net.portalmod.common.items.BulletsItem;
import net.portalmod.common.items.ModSpawnEggItem;
import net.portalmod.common.items.PanelBlockItem;
import net.portalmod.common.items.WrenchItem;
import net.portalmod.common.sorted.antline.AntlineBlockItem;
import net.portalmod.common.sorted.gel.container.EmptyGelContainer;
import net.portalmod.common.sorted.gel.container.GelContainer;
import net.portalmod.common.sorted.goo.GooBucketItem;
import net.portalmod.common.sorted.longfallboots.LongFallBoots;
import net.portalmod.common.sorted.portalgun.PortalGun;
import net.portalmod.common.sorted.sign.ChamberSignItem;
import net.portalmod.core.util.Registry;
import net.portalmod.core.util.Registry.Entry;

import java.util.function.Function;

public class ItemInit {
    public static final Registry<Item> ITEMS =
            new Registry<>(
                    (id, value) ->
                            net.minecraft.core.Registry.register(
                                    BuiltInRegistries.ITEM,
                                    Identifier.fromNamespaceAndPath(PortalMod.MODID, id),
                                    value));

    private ItemInit() {}

    // Fundamental Items
    public static final Entry<Item> PORTALGUN =
            register("portalgun", key -> new PortalGun(properties(key).stacksTo(1).fireResistant()));

    public static final Entry<Item> LONGFALL_BOOTS =
            register(
                    "longfall_boots",
                    key -> new LongFallBoots(ArmorMaterialInit.LONGFALL_BOOTS, properties(key).fireResistant()));

    public static final Entry<Item> WRENCH = register("wrench", key -> new WrenchItem(properties(key).stacksTo(1)));

    // Cubes
    public static final Entry<Item> COMPANION_CUBE =
            registerSpawnEgg("companion_cube", EntityInit.COMPANION_CUBE, "cube");
    public static final Entry<Item> STORAGE_CUBE = registerSpawnEgg("storage_cube", EntityInit.STORAGE_CUBE, "cube");
    public static final Entry<Item> VINTAGE_CUBE = registerSpawnEgg("vintage_cube", EntityInit.VINTAGE_CUBE, "cube");
    public static final Entry<Item> GABE =
            register("gabe", key -> new ModSpawnEggItem(EntityInit.GABE, properties(key).stacksTo(1), "cube"));

    public static final Entry<Item> CUBE_DROPPER = registerBlockItem("cube_dropper", BlockInit.CUBE_DROPPER);

    // Test Elements
    public static final Entry<Item> SUPER_BUTTON = registerBlockItem("super_button", BlockInit.SUPER_BUTTON);
    public static final Entry<Item> STANDING_BUTTON = registerBlockItem("standing_button", BlockInit.STANDING_BUTTON);

    public static final Entry<Item> ANTLINE =
            register(
                    "antline",
                    key -> new AntlineBlockItem(BlockInit.ANTLINE.get(), properties(key).useBlockDescriptionPrefix()));

    public static final Entry<Item> ANTLINE_INDICATOR =
            registerBlockItem("antline_indicator", BlockInit.ANTLINE_INDICATOR);
    public static final Entry<Item> ANTLINE_TIMER = registerBlockItem("antline_timer", BlockInit.ANTLINE_TIMER);
    public static final Entry<Item> ANTLINE_DECODER = registerBlockItem("antline_decoder", BlockInit.ANTLINE_DECODER);
    public static final Entry<Item> ANTLINE_ENCODER = registerBlockItem("antline_encoder", BlockInit.ANTLINE_ENCODER);

    public static final Entry<Item> TRIGGER =
            register(
                    "trigger",
                    key -> new BlockItem(BlockInit.TRIGGER.get(), properties(key).useBlockDescriptionPrefix()));

    public static final Entry<Item> AUTOPORTAL =
            register(
                    "autoportal",
                    key -> new BlockItem(BlockInit.AUTOPORTAL.get(), properties(key).useBlockDescriptionPrefix()));

    public static final Entry<Item> CHAMBER_DOOR = registerBlockItem("chamber_door", BlockInit.CHAMBER_DOOR);
    public static final Entry<Item> PUSH_DOOR = registerBlockItem("push_door", BlockInit.PUSH_DOOR);
    public static final Entry<Item> FIZZLER_EMITTER = registerBlockItem("fizzler_emitter", BlockInit.FIZZLER_EMITTER);
    public static final Entry<Item> FAITHPLATE = registerBlockItem("faithplate", BlockInit.FAITHPLATE);

    public static final Entry<Item> CHAMBER_SIGN =
            register("chamber_sign", key -> new ChamberSignItem(properties(key)));

    public static final Entry<Item> CHAMBER_LIGHTS =
            register(
                    "chamber_lights",
                    key -> new BlockItem(BlockInit.CHAMBER_LIGHTS.get(), properties(key).useBlockDescriptionPrefix()));

    public static final Entry<Item> GOO_BUCKET =
            register("goo_bucket", key -> new GooBucketItem(FluidInit.GOO_FLUID, properties(key).stacksTo(1)));

    public static final Entry<Item> CONTAINER =
            register("container", key -> new EmptyGelContainer(properties(key).stacksTo(16)));

    public static final Entry<Item> REPULSION_GEL =
            register(
                    "repulsion_gel",
                    key ->
                            new GelContainer(
                                    BlockInit.REPULSION_GEL.get(),
                                    properties(key).useBlockDescriptionPrefix().stacksTo(1),
                                    0x3776F1));

    public static final Entry<Item> PROPULSION_GEL =
            register(
                    "propulsion_gel",
                    key ->
                            new GelContainer(
                                    BlockInit.PROPULSION_GEL.get(),
                                    properties(key).useBlockDescriptionPrefix().stacksTo(1),
                                    0xE3834A));

    public static final Entry<Item> TURRET = registerSpawnEgg("turret", EntityInit.TURRET, "turret");

    public static final Entry<Item> BULLETS = register("bullets", key -> new BulletsItem(properties(key)));

    public static final Entry<Item> RADIO = registerBlockItem("radio", BlockInit.RADIO);

    // Discs
    public static final Entry<Item> DISC_RAIN =
            register(
                    "music_disc_rain",
                    key ->
                            new Item(
                                    properties(key)
                                            .stacksTo(1)
                                            .rarity(Rarity.RARE)
                                            .jukeboxPlayable(
                                                    ResourceKey.create(
                                                            Registries.JUKEBOX_SONG,
                                                            Identifier.fromNamespaceAndPath(
                                                                    PortalMod.MODID, "rain")))));

    public static final Entry<Item> FOREST_CAKE =
            register(
                    "forest_cake",
                    key ->
                            new BlockItem(
                                    BlockInit.FOREST_CAKE.get(),
                                    properties(key).useBlockDescriptionPrefix().stacksTo(1)));

    public static final Entry<Item> PLATFORM_BEAM =
            register(
                    "platform_beam",
                    key -> new BlockItem(BlockInit.PLATFORM_BEAM.get(), properties(key).useBlockDescriptionPrefix()));
    public static final Entry<Item> RUSTY_PLATFORM_BEAM =
            register(
                    "rusty_platform_beam",
                    key ->
                            new BlockItem(
                                    BlockInit.RUSTY_PLATFORM_BEAM.get(), properties(key).useBlockDescriptionPrefix()));

    // Blocks
    public static final Entry<Item> LUNECAST = registerPanelBlockItem("lunecast", BlockInit.LUNECAST);
    public static final Entry<Item> LUNECAST_STAIRS = registerBlockItem("lunecast_stairs", BlockInit.LUNECAST_STAIRS);
    public static final Entry<Item> LUNECAST_SLAB = registerBlockItem("lunecast_slab", BlockInit.LUNECAST_SLAB);
    public static final Entry<Item> LUNECAST_PLATFORM =
            registerBlockItem("lunecast_platform", BlockInit.LUNECAST_PLATFORM);

    public static final Entry<Item> BLACKPLATE = registerPanelBlockItem("blackplate", BlockInit.BLACKPLATE);
    public static final Entry<Item> BLACKPLATE_STAIRS =
            registerBlockItem("blackplate_stairs", BlockInit.BLACKPLATE_STAIRS);
    public static final Entry<Item> BLACKPLATE_SLAB = registerBlockItem("blackplate_slab", BlockInit.BLACKPLATE_SLAB);
    public static final Entry<Item> BLACKPLATE_PLATFORM =
            registerBlockItem("blackplate_platform", BlockInit.BLACKPLATE_PLATFORM);

    public static final Entry<Item> ARBORED_LUNECAST =
            registerPanelBlockItem("arbored_lunecast", BlockInit.ARBORED_LUNECAST);
    public static final Entry<Item> ARBORED_LUNECAST_STAIRS =
            registerBlockItem("arbored_lunecast_stairs", BlockInit.ARBORED_LUNECAST_STAIRS);
    public static final Entry<Item> ARBORED_LUNECAST_SLAB =
            registerBlockItem("arbored_lunecast_slab", BlockInit.ARBORED_LUNECAST_SLAB);
    public static final Entry<Item> ARBORED_LUNECAST_PLATFORM =
            registerBlockItem("arbored_lunecast_platform", BlockInit.ARBORED_LUNECAST_PLATFORM);

    public static final Entry<Item> ARBORED_BLACKPLATE =
            registerPanelBlockItem("arbored_blackplate", BlockInit.ARBORED_BLACKPLATE);
    public static final Entry<Item> ARBORED_BLACKPLATE_STAIRS =
            registerBlockItem("arbored_blackplate_stairs", BlockInit.ARBORED_BLACKPLATE_STAIRS);
    public static final Entry<Item> ARBORED_BLACKPLATE_SLAB =
            registerBlockItem("arbored_blackplate_slab", BlockInit.ARBORED_BLACKPLATE_SLAB);
    public static final Entry<Item> ARBORED_BLACKPLATE_PLATFORM =
            registerBlockItem("arbored_blackplate_platform", BlockInit.ARBORED_BLACKPLATE_PLATFORM);

    public static final Entry<Item> ERODED_LUNECAST =
            registerPanelBlockItem("eroded_lunecast", BlockInit.ERODED_LUNECAST);
    public static final Entry<Item> ERODED_LUNECAST_STAIRS =
            registerBlockItem("eroded_lunecast_stairs", BlockInit.ERODED_LUNECAST_STAIRS);
    public static final Entry<Item> ERODED_LUNECAST_SLAB =
            registerBlockItem("eroded_lunecast_slab", BlockInit.ERODED_LUNECAST_SLAB);
    public static final Entry<Item> ERODED_LUNECAST_PLATFORM =
            registerBlockItem("eroded_lunecast_platform", BlockInit.ERODED_LUNECAST_PLATFORM);

    public static final Entry<Item> ERODED_BLACKPLATE =
            registerPanelBlockItem("eroded_blackplate", BlockInit.ERODED_BLACKPLATE);
    public static final Entry<Item> ERODED_BLACKPLATE_STAIRS =
            registerBlockItem("eroded_blackplate_stairs", BlockInit.ERODED_BLACKPLATE_STAIRS);
    public static final Entry<Item> ERODED_BLACKPLATE_SLAB =
            registerBlockItem("eroded_blackplate_slab", BlockInit.ERODED_BLACKPLATE_SLAB);
    public static final Entry<Item> ERODED_BLACKPLATE_PLATFORM =
            registerBlockItem("eroded_blackplate_platform", BlockInit.ERODED_BLACKPLATE_PLATFORM);

    public static final Entry<Item> FRACTURED_LUNECAST =
            registerPanelBlockItem("fractured_lunecast", BlockInit.FRACTURED_LUNECAST);
    public static final Entry<Item> FRACTURED_LUNECAST_STAIRS =
            registerBlockItem("fractured_lunecast_stairs", BlockInit.FRACTURED_LUNECAST_STAIRS);
    public static final Entry<Item> FRACTURED_LUNECAST_SLAB =
            registerBlockItem("fractured_lunecast_slab", BlockInit.FRACTURED_LUNECAST_SLAB);
    public static final Entry<Item> FRACTURED_LUNECAST_PLATFORM =
            registerBlockItem("fractured_lunecast_platform", BlockInit.FRACTURED_LUNECAST_PLATFORM);

    public static final Entry<Item> FRACTURED_BLACKPLATE =
            registerPanelBlockItem("fractured_blackplate", BlockInit.FRACTURED_BLACKPLATE);
    public static final Entry<Item> FRACTURED_BLACKPLATE_STAIRS =
            registerBlockItem("fractured_blackplate_stairs", BlockInit.FRACTURED_BLACKPLATE_STAIRS);
    public static final Entry<Item> FRACTURED_BLACKPLATE_SLAB =
            registerBlockItem("fractured_blackplate_slab", BlockInit.FRACTURED_BLACKPLATE_SLAB);
    public static final Entry<Item> FRACTURED_BLACKPLATE_PLATFORM =
            registerBlockItem("fractured_blackplate_platform", BlockInit.FRACTURED_BLACKPLATE_PLATFORM);

    public static final Entry<Item> VINTAGE_LUNECAST =
            registerPanelBlockItem("vintage_lunecast", BlockInit.VINTAGE_LUNECAST);
    public static final Entry<Item> VINTAGE_LUNECAST_STAIRS =
            registerBlockItem("vintage_lunecast_stairs", BlockInit.VINTAGE_LUNECAST_STAIRS);
    public static final Entry<Item> VINTAGE_LUNECAST_SLAB =
            registerBlockItem("vintage_lunecast_slab", BlockInit.VINTAGE_LUNECAST_SLAB);
    public static final Entry<Item> VINTAGE_LUNECAST_PLATFORM =
            registerBlockItem("vintage_lunecast_platform", BlockInit.VINTAGE_LUNECAST_PLATFORM);

    public static final Entry<Item> VINTAGE_BLACKPLATE =
            registerPanelBlockItem("vintage_blackplate", BlockInit.VINTAGE_BLACKPLATE);
    public static final Entry<Item> VINTAGE_BLACKPLATE_STAIRS =
            registerBlockItem("vintage_blackplate_stairs", BlockInit.VINTAGE_BLACKPLATE_STAIRS);
    public static final Entry<Item> VINTAGE_BLACKPLATE_SLAB =
            registerBlockItem("vintage_blackplate_slab", BlockInit.VINTAGE_BLACKPLATE_SLAB);
    public static final Entry<Item> VINTAGE_BLACKPLATE_PLATFORM =
            registerBlockItem("vintage_blackplate_platform", BlockInit.VINTAGE_BLACKPLATE_PLATFORM);

    // Decoration blocks
    public static final Entry<Item> WIRE_MESH_BLOCK = registerBlockItem("wire_mesh_block", BlockInit.WIRE_MESH_BLOCK);
    public static final Entry<Item> WIRE_MESH = registerBlockItem("wire_mesh", BlockInit.WIRE_MESH);
    public static final Entry<Item> IRON_FRAME = registerBlockItem("iron_frame", BlockInit.IRON_FRAME);
    public static final Entry<Item> BARRED_IRON_FRAME =
            registerBlockItem("barred_iron_frame", BlockInit.BARRED_IRON_FRAME);
    public static final Entry<Item> MESHED_IRON_FRAME =
            registerBlockItem("meshed_iron_frame", BlockInit.MESHED_IRON_FRAME);
    public static final Entry<Item> RUSTY_IRON_FRAME =
            registerBlockItem("rusty_iron_frame", BlockInit.RUSTY_IRON_FRAME);
    public static final Entry<Item> RUSTY_BARRED_IRON_FRAME =
            registerBlockItem("rusty_barred_iron_frame", BlockInit.RUSTY_BARRED_IRON_FRAME);
    public static final Entry<Item> RUSTY_MESHED_IRON_FRAME =
            registerBlockItem("rusty_meshed_iron_frame", BlockInit.RUSTY_MESHED_IRON_FRAME);

    private static Entry<Item> register(String name, Function<ResourceKey<Item>, Item> factory) {
        ResourceKey<Item> key =
                ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(PortalMod.MODID, name));
        return ITEMS.register(name, () -> factory.apply(key));
    }

    public static Entry<Item> registerPanelBlockItem(String name, Entry<Block> block) {
        return ItemInit.register(
                name, key -> new PanelBlockItem(block.get(), properties(key).useBlockDescriptionPrefix()));
    }

    public static Entry<Item> registerBlockItem(String name, Entry<Block> block) {
        return ItemInit.register(name, key -> new BlockItem(block.get(), properties(key).useBlockDescriptionPrefix()));
    }

    public static Entry<Item> registerSpawnEgg(String name, Entry<? extends EntityType<?>> entity, String tooltip) {
        return register(name, key -> new ModSpawnEggItem(entity, properties(key).stacksTo(1), tooltip));
    }

    public static Entry<Item> registerSpawnEgg(String name, Entry<? extends EntityType<?>> entity) {
        return registerSpawnEgg(name, entity, null);
    }

    // net.minecraft.dispenser.DispenseItemBehavior:343
    public static void registerFluidDispenserBehavior() {
        DispenseItemBehavior bucketDispense =
                new DefaultDispenseItemBehavior() {
                    private final DefaultDispenseItemBehavior defaultDispenseItemBehavior =
                            new DefaultDispenseItemBehavior();

                    public ItemStack execute(BlockSource source, ItemStack stack) {
                        BucketItem bucketitem = (BucketItem) stack.getItem();
                        BlockPos blockpos = source.pos().relative(source.state().getValue(DispenserBlock.FACING));
                        Level world = source.level();
                        if (bucketitem.emptyContents(null, world, blockpos, null)) {
                            bucketitem.checkExtraContent(null, world, stack, blockpos);
                            return new ItemStack(Items.BUCKET);
                        } else {
                            return this.defaultDispenseItemBehavior.dispense(source, stack);
                        }
                    }
                };
        DispenserBlock.registerBehavior(ItemInit.GOO_BUCKET.get(), bucketDispense);
    }

    public static Item.Properties properties(ResourceKey<Item> key) {
        return new Item.Properties().setId(key);
    }
}
