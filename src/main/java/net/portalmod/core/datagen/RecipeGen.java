package net.portalmod.core.datagen;

import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.predicates.BlockPredicate;
import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.advancements.predicates.LocationPredicate;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.advancements.triggers.ItemUsedOnLocationTrigger;
import net.minecraft.advancements.triggers.KilledTrigger;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.data.recipes.SingleItemRecipeBuilder;
import net.minecraft.data.recipes.SpecialRecipeBuilder;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.portalmod.common.sorted.portalgun.PortalGunDuplicateRecipe;
import net.portalmod.common.sorted.portalgun.PortalGunModifyRecipe;
import net.portalmod.core.init.ItemInit;
import net.portalmod.core.init.ItemTagInit;

public class RecipeGen extends RecipeProvider {
    public final Criterion<?> ROOT_CRITERION;
    private final HolderGetter<Item> itemLookup;
    public final Criterion<?> CAKE_CRITERION = KilledTrigger.TriggerInstance.playerKilledEntity();

    public final Criterion<?> HAS_CUBE = has(ItemTagInit.CUBES);
    public final Criterion<?> HAS_INDICATOR = has(ItemTagInit.INDICATORS);
    public final Criterion<?> HAS_LUNECAST = has(ItemTagInit.LUNECAST);
    public final Criterion<?> HAS_BLACKPLATE = has(ItemTagInit.BLACKPLATE);
    public final Criterion<?> HAS_CHAMBER_DECORATION = has(ItemTagInit.CHAMBER_DECORATION);
    public final Criterion<?> HAS_ELEMENT = has(ItemTagInit.TESTING_ELEMENTS);

    public RecipeGen(BootstrapContext<Recipe<?>> recipes, BootstrapContext<Advancement> advancements) {
        super(recipes, advancements);
        itemLookup = recipes.lookup(Registries.ITEM);
        ROOT_CRITERION =
                ItemUsedOnLocationTrigger.TriggerInstance.itemUsedOnBlock(
                        LocationPredicate.Builder.location()
                                .setBlock(
                                        BlockPredicate.Builder.block()
                                                .of(recipes.lookup(Registries.BLOCK), Blocks.CRAFTING_TABLE)),
                        ItemPredicate.Builder.item());
    }

    @Override
    public void buildRecipes() {
        RecipeOutput recipes = output;

        // --- Basic Items --- //

        shaped(RecipeCategory.MISC, ItemInit.PORTALGUN.get())
                .pattern("ii ")
                .pattern("ron")
                .define('i', Items.IRON_INGOT)
                .define('r', Items.REDSTONE)
                .define('o', Items.ENDER_PEARL)
                .define('n', Items.NETHERITE_INGOT)
                .unlockedBy("has_item", ROOT_CRITERION)
                .save(recipes);

        SpecialRecipeBuilder.special(PortalGunModifyRecipe::new).save(recipes, "portalmod:portalgun_edit");
        SpecialRecipeBuilder.special(PortalGunDuplicateRecipe::new).save(recipes, "portalmod:portalgun_duplicate");

        shaped(RecipeCategory.MISC, ItemInit.LONGFALL_BOOTS.get())
                .pattern("i i")
                .define('i', Items.IRON_INGOT)
                .pattern("n n")
                .define('n', Items.NETHERITE_SCRAP)
                .unlockedBy("has_item", ROOT_CRITERION)
                .save(recipes);

        shaped(RecipeCategory.MISC, ItemInit.WRENCH.get())
                .pattern(" i ")
                .define('i', Items.IRON_INGOT)
                .pattern(" ii")
                .define('/', Items.STICK)
                .pattern("/  ")
                .unlockedBy("has_item", ROOT_CRITERION)
                .save(recipes);

        shaped(RecipeCategory.MISC, ItemInit.RADIO.get())
                .pattern(" i ")
                .define('i', Items.IRON_INGOT)
                .pattern("ioi")
                .define('o', Items.JUKEBOX)
                .unlockedBy("has_item", ROOT_CRITERION)
                .save(recipes);

        shaped(RecipeCategory.MISC, ItemInit.FOREST_CAKE.get())
                .pattern("oio")
                .pattern("cfc")
                .pattern("www")
                .define('i', Items.TORCH)
                .define('o', Items.SWEET_BERRIES)
                .define('c', Items.COCOA_BEANS)
                .define('f', Items.ROTTEN_FLESH)
                .define('w', Items.WHEAT)
                .unlockedBy("kill_entity", CAKE_CRITERION)
                .save(recipes);

        shaped(RecipeCategory.MISC, ItemInit.TRIGGER.get())
                .pattern(" i ")
                .pattern(" l ")
                .pattern("rpr")
                .define('i', Items.REDSTONE_TORCH)
                .define('l', Items.REDSTONE_LAMP)
                .define('r', Items.REDSTONE)
                .define('p', ItemTags.PLANKS)
                .unlockedBy("has_item", HAS_ELEMENT)
                .save(recipes);

        shaped(RecipeCategory.MISC, ItemInit.AUTOPORTAL.get())
                .pattern("i i")
                .pattern("e e")
                .pattern("i i")
                .define('i', Items.IRON_INGOT)
                .define('e', Items.ENDER_PEARL)
                .unlockedBy("has_item", HAS_ELEMENT)
                .save(recipes);

        // --- Cubes --- //

        shaped(RecipeCategory.MISC, ItemInit.STORAGE_CUBE.get())
                .pattern("IiI")
                .define('I', Items.IRON_INGOT)
                .pattern("i i")
                .define('i', Items.IRON_NUGGET)
                .pattern("IiI")
                .unlockedBy("has_item", ROOT_CRITERION)
                .save(recipes);

        shaped(RecipeCategory.MISC, ItemInit.COMPANION_CUBE.get())
                .pattern("IiI")
                .define('I', Items.IRON_INGOT)
                .pattern("iai")
                .define('i', Items.IRON_NUGGET)
                .pattern("IiI")
                .define('a', Items.GOLDEN_APPLE)
                .unlockedBy("has_item", HAS_CUBE)
                .save(recipes);

        shaped(RecipeCategory.MISC, ItemInit.VINTAGE_CUBE.get())
                .pattern("IiI")
                .define('I', Items.IRON_INGOT)
                .pattern("ipi")
                .define('i', Items.IRON_NUGGET)
                .pattern("IiI")
                .define('p', ItemTags.PLANKS)
                .unlockedBy("has_item", HAS_CUBE)
                .save(recipes);

        // --- Antline --- //

        shapeless(RecipeCategory.MISC, ItemInit.ANTLINE.get(), 8)
                .requires(Items.GOLD_INGOT)
                .requires(Items.DYE.lightBlue())
                .requires(Items.GLOWSTONE_DUST)
                .unlockedBy("has_item", ROOT_CRITERION)
                .save(recipes);

        shaped(RecipeCategory.MISC, ItemInit.ANTLINE_INDICATOR.get())
                .pattern("iai")
                .define('i', Items.IRON_INGOT)
                .define('a', ItemInit.ANTLINE.get())
                .unlockedBy("has_item", ROOT_CRITERION)
                .save(recipes);

        shapeless(RecipeCategory.MISC, ItemInit.ANTLINE_TIMER.get())
                .requires(ItemInit.ANTLINE_INDICATOR.get())
                .requires(Items.CLOCK)
                .unlockedBy("has_item", HAS_INDICATOR)
                .save(recipes);

        shapeless(RecipeCategory.MISC, ItemInit.ANTLINE_DECODER.get())
                .requires(ItemInit.ANTLINE_INDICATOR.get())
                .requires(Items.REDSTONE)
                .unlockedBy("has_item", HAS_INDICATOR)
                .group("antline_decoder")
                .save(recipes, "portalmod:antline_decoder_from_indicator");

        shapeless(RecipeCategory.MISC, ItemInit.ANTLINE_DECODER.get())
                .requires(ItemInit.ANTLINE_ENCODER.get())
                .unlockedBy("has_item", HAS_INDICATOR)
                .group("antline_decoder")
                .save(recipes, "portalmod:antline_decoder_from_encoder");

        shapeless(RecipeCategory.MISC, ItemInit.ANTLINE_ENCODER.get())
                .requires(ItemInit.ANTLINE_DECODER.get())
                .unlockedBy("has_item", HAS_INDICATOR)
                .save(recipes);

        // --- Testing Elements --- //

        shaped(RecipeCategory.MISC, ItemInit.SUPER_BUTTON.get())
                .pattern("ipi")
                .pattern("#a#")
                .define('i', Items.IRON_NUGGET)
                .define('#', Items.BLACKSTONE)
                .define('p', ItemTagInit.PRESSURE_PLATES)
                .define('a', ItemInit.ANTLINE.get())
                .unlockedBy("has_item", ROOT_CRITERION)
                .save(recipes);

        shaped(RecipeCategory.MISC, ItemInit.STANDING_BUTTON.get())
                .pattern("b")
                .define('b', ItemTagInit.BUTTONS)
                .pattern("i")
                .define('i', Items.IRON_INGOT)
                .pattern("#")
                .define('#', Items.BLACKSTONE)
                .unlockedBy("has_item", ROOT_CRITERION)
                .save(recipes);

        shaped(RecipeCategory.MISC, ItemInit.CHAMBER_DOOR.get())
                .pattern("bb")
                .pattern("ii")
                .define('b', ItemInit.BLACKPLATE_SLAB.get())
                .define('i', Items.IRON_DOOR)
                .unlockedBy("has_item", ROOT_CRITERION)
                .save(recipes);

        shapeless(RecipeCategory.MISC, ItemInit.PUSH_DOOR.get())
                .requires(Items.IRON_DOOR)
                .requires(ItemTagInit.BUTTONS)
                .unlockedBy("has_item", HAS_ELEMENT)
                .save(recipes);

        shaped(RecipeCategory.MISC, ItemInit.TURRET.get())
                .pattern(" i ")
                .pattern("brb")
                .pattern("/i/")
                .define('i', Items.IRON_INGOT)
                .define('b', Items.BOW)
                .define('r', Items.REDSTONE)
                .define('/', Items.STICK)
                .unlockedBy("has_item", HAS_ELEMENT)
                .save(recipes);

        shapeless(RecipeCategory.MISC, ItemInit.BULLETS.get(), 4)
                .requires(Items.GUNPOWDER)
                .requires(Items.GOLD_INGOT)
                .unlockedBy("has_item", HAS_ELEMENT)
                .save(recipes);

        shaped(RecipeCategory.MISC, ItemInit.CUBE_DROPPER.get())
                .pattern("III")
                .pattern("gDg")
                .pattern("iii")
                .define('I', Items.IRON_INGOT)
                .define('i', Items.IRON_NUGGET)
                .define('D', Items.DISPENSER)
                .define('g', ConventionalItemTags.GLASS_PANES)
                .unlockedBy("has_item", HAS_ELEMENT)
                .save(recipes);

        shaped(RecipeCategory.MISC, ItemInit.FIZZLER_EMITTER.get(), 2)
                .pattern("Bi")
                .define('i', Items.IRON_INGOT)
                .pattern("Bd")
                .define('d', Items.PRISMARINE_CRYSTALS)
                .pattern("Bi")
                .define('B', Items.BLACKSTONE)
                .unlockedBy("has_item", HAS_ELEMENT)
                .save(recipes);

        shaped(RecipeCategory.MISC, ItemInit.CHAMBER_SIGN.get())
                .pattern("igi")
                .define('g', Items.GLOWSTONE)
                .pattern("ibi")
                .define('b', Items.BANNER.white())
                .pattern("igi")
                .define('i', Items.IRON_NUGGET)
                .unlockedBy("has_item", HAS_ELEMENT)
                .save(recipes);

        shaped(RecipeCategory.MISC, ItemInit.FAITHPLATE.get())
                .pattern(" p ")
                .pattern("Pai")
                .pattern("bbb")
                .define('p', Ingredient.of(Items.PISTON, Items.STICKY_PISTON))
                .define('P', ItemTagInit.PRESSURE_PLATES)
                .define('a', ItemInit.ANTLINE.get())
                .define('i', Items.IRON_INGOT)
                .define('b', ItemInit.BLACKPLATE_SLAB.get())
                .unlockedBy("has_item", HAS_ELEMENT)
                .save(recipes);

        shaped(RecipeCategory.MISC, ItemInit.CONTAINER.get())
                .pattern("i i")
                .define('i', Items.IRON_INGOT)
                .pattern("pip")
                .define('p', Items.PAPER)
                .unlockedBy("has_item", HAS_ELEMENT)
                .save(recipes);

        shapeless(RecipeCategory.MISC, ItemInit.REPULSION_GEL.get())
                .requires(Items.WARPED_FUNGUS)
                .requires(Items.SLIME_BALL)
                .requires(ItemInit.CONTAINER.get())
                .unlockedBy("has_item", HAS_ELEMENT)
                .save(recipes);

        shapeless(RecipeCategory.MISC, ItemInit.PROPULSION_GEL.get())
                .requires(Items.CRIMSON_FUNGUS)
                .requires(Items.MAGMA_CREAM)
                .requires(ItemInit.CONTAINER.get())
                .unlockedBy("has_item", HAS_ELEMENT)
                .save(recipes);

        // --- Building Blocks --- //

        shaped(RecipeCategory.MISC, ItemInit.LUNECAST.get(), 4)
                .pattern("q#")
                .define('q', Items.QUARTZ)
                .pattern("#q")
                .define('#', Items.END_STONE)
                .unlockedBy("has_item", ROOT_CRITERION)
                .save(recipes);

        shaped(RecipeCategory.MISC, ItemInit.BLACKPLATE.get(), 4)
                .pattern("i#")
                .define('i', Items.IRON_NUGGET)
                .pattern("#i")
                .define('#', Items.BLACKSTONE)
                .unlockedBy("has_item", ROOT_CRITERION)
                .save(recipes);

        lunecastVariant(
                recipes,
                ItemInit.ARBORED_LUNECAST.get(),
                Ingredient.of(Items.SHORT_GRASS, Items.TALL_GRASS, Items.SEAGRASS, Items.VINE));
        lunecastVariant(
                recipes, ItemInit.ERODED_LUNECAST.get(), Ingredient.of(Items.DIRT, Items.COARSE_DIRT, Items.GRAVEL));
        lunecastVariant(
                recipes, ItemInit.VINTAGE_LUNECAST.get(), Ingredient.of(itemLookup.getOrThrow(ItemTags.PLANKS)));
        blackplateVariant(
                recipes,
                ItemInit.ARBORED_BLACKPLATE.get(),
                Ingredient.of(Items.SHORT_GRASS, Items.TALL_GRASS, Items.SEAGRASS, Items.VINE));
        blackplateVariant(
                recipes, ItemInit.ERODED_BLACKPLATE.get(), Ingredient.of(Items.DIRT, Items.COARSE_DIRT, Items.GRAVEL));
        blackplateVariant(
                recipes, ItemInit.VINTAGE_BLACKPLATE.get(), Ingredient.of(itemLookup.getOrThrow(ItemTags.PLANKS)));

        SimpleCookingRecipeBuilder.smelting(
                        Ingredient.of(ItemInit.LUNECAST.get()),
                        RecipeCategory.MISC,
                        CookingBookCategory.BLOCKS,
                        ItemInit.FRACTURED_LUNECAST.get(),
                        0.1f,
                        200)
                .unlockedBy("has_item", HAS_LUNECAST)
                .save(recipes);

        SimpleCookingRecipeBuilder.smelting(
                        Ingredient.of(ItemInit.BLACKPLATE.get()),
                        RecipeCategory.MISC,
                        CookingBookCategory.BLOCKS,
                        ItemInit.FRACTURED_BLACKPLATE.get(),
                        0.1f,
                        200)
                .unlockedBy("has_item", HAS_BLACKPLATE)
                .save(recipes);

        lunecastSlab(recipes, ItemInit.LUNECAST_SLAB.get(), ItemInit.LUNECAST.get());
        lunecastSlab(recipes, ItemInit.ARBORED_LUNECAST_SLAB.get(), ItemInit.ARBORED_LUNECAST.get());
        lunecastSlab(recipes, ItemInit.ERODED_LUNECAST_SLAB.get(), ItemInit.ERODED_LUNECAST.get());
        lunecastSlab(recipes, ItemInit.FRACTURED_LUNECAST_SLAB.get(), ItemInit.FRACTURED_LUNECAST.get());
        lunecastSlab(recipes, ItemInit.VINTAGE_LUNECAST_SLAB.get(), ItemInit.VINTAGE_LUNECAST.get());
        blackplateSlab(recipes, ItemInit.BLACKPLATE_SLAB.get(), ItemInit.BLACKPLATE.get());
        blackplateSlab(recipes, ItemInit.ARBORED_BLACKPLATE_SLAB.get(), ItemInit.ARBORED_BLACKPLATE.get());
        blackplateSlab(recipes, ItemInit.ERODED_BLACKPLATE_SLAB.get(), ItemInit.ERODED_BLACKPLATE.get());
        blackplateSlab(recipes, ItemInit.FRACTURED_BLACKPLATE_SLAB.get(), ItemInit.FRACTURED_BLACKPLATE.get());
        blackplateSlab(recipes, ItemInit.VINTAGE_BLACKPLATE_SLAB.get(), ItemInit.VINTAGE_BLACKPLATE.get());

        lunecastStairs(recipes, ItemInit.LUNECAST_STAIRS.get(), ItemInit.LUNECAST.get());
        lunecastStairs(recipes, ItemInit.ARBORED_LUNECAST_STAIRS.get(), ItemInit.ARBORED_LUNECAST.get());
        lunecastStairs(recipes, ItemInit.ERODED_LUNECAST_STAIRS.get(), ItemInit.ERODED_LUNECAST.get());
        lunecastStairs(recipes, ItemInit.FRACTURED_LUNECAST_STAIRS.get(), ItemInit.FRACTURED_LUNECAST.get());
        lunecastStairs(recipes, ItemInit.VINTAGE_LUNECAST_STAIRS.get(), ItemInit.VINTAGE_LUNECAST.get());
        blackplateStairs(recipes, ItemInit.BLACKPLATE_STAIRS.get(), ItemInit.BLACKPLATE.get());
        blackplateStairs(recipes, ItemInit.ARBORED_BLACKPLATE_STAIRS.get(), ItemInit.ARBORED_BLACKPLATE.get());
        blackplateStairs(recipes, ItemInit.ERODED_BLACKPLATE_STAIRS.get(), ItemInit.ERODED_BLACKPLATE.get());
        blackplateStairs(recipes, ItemInit.FRACTURED_BLACKPLATE_STAIRS.get(), ItemInit.FRACTURED_BLACKPLATE.get());
        blackplateStairs(recipes, ItemInit.VINTAGE_BLACKPLATE_STAIRS.get(), ItemInit.VINTAGE_BLACKPLATE.get());

        lunecastPlatform(recipes, ItemInit.LUNECAST_PLATFORM.get(), ItemInit.LUNECAST.get());
        lunecastPlatform(recipes, ItemInit.ARBORED_LUNECAST_PLATFORM.get(), ItemInit.ARBORED_LUNECAST.get());
        lunecastPlatform(recipes, ItemInit.ERODED_LUNECAST_PLATFORM.get(), ItemInit.ERODED_LUNECAST.get());
        lunecastPlatform(recipes, ItemInit.FRACTURED_LUNECAST_PLATFORM.get(), ItemInit.FRACTURED_LUNECAST.get());
        lunecastPlatform(recipes, ItemInit.VINTAGE_LUNECAST_PLATFORM.get(), ItemInit.VINTAGE_LUNECAST.get());
        blackplatePlatform(recipes, ItemInit.BLACKPLATE_PLATFORM.get(), ItemInit.BLACKPLATE.get());
        blackplatePlatform(recipes, ItemInit.ARBORED_BLACKPLATE_PLATFORM.get(), ItemInit.ARBORED_BLACKPLATE.get());
        blackplatePlatform(recipes, ItemInit.ERODED_BLACKPLATE_PLATFORM.get(), ItemInit.ERODED_BLACKPLATE.get());
        blackplatePlatform(recipes, ItemInit.FRACTURED_BLACKPLATE_PLATFORM.get(), ItemInit.FRACTURED_BLACKPLATE.get());
        blackplatePlatform(recipes, ItemInit.VINTAGE_BLACKPLATE_PLATFORM.get(), ItemInit.VINTAGE_BLACKPLATE.get());

        shaped(RecipeCategory.MISC, ItemInit.CHAMBER_LIGHTS.get(), 2)
                .pattern("g#g")
                .define('#', ItemInit.BLACKPLATE.get())
                .pattern("g#g")
                .define('g', Items.GLOWSTONE_DUST)
                .unlockedBy("has_item", ROOT_CRITERION)
                .save(recipes);

        shaped(RecipeCategory.MISC, ItemInit.WIRE_MESH.get())
                .pattern("nnn")
                .define('n', Items.IRON_NUGGET)
                .pattern("nnn")
                .unlockedBy("has_item", HAS_CHAMBER_DECORATION)
                .group("wire_mesh")
                .save(recipes);

        shapeless(RecipeCategory.MISC, ItemInit.WIRE_MESH.get(), 4)
                .requires(ItemInit.WIRE_MESH_BLOCK.get())
                .unlockedBy("has_item", HAS_CHAMBER_DECORATION)
                .group("wire_mesh")
                .save(recipes, "portalmod:wire_mesh_from_block");

        shaped(RecipeCategory.MISC, ItemInit.WIRE_MESH_BLOCK.get())
                .pattern("ww")
                .define('w', ItemInit.WIRE_MESH.get())
                .pattern("ww")
                .unlockedBy("has_item", HAS_CHAMBER_DECORATION)
                .save(recipes);

        shapeless(RecipeCategory.MISC, ItemInit.GOO_BUCKET.get())
                .requires(Items.SOUL_SAND)
                .requires(Items.DIRT)
                .requires(Items.BUCKET)
                .unlockedBy("has_item", HAS_CHAMBER_DECORATION)
                .save(recipes);

        shaped(RecipeCategory.MISC, ItemInit.IRON_FRAME.get(), 8)
                .pattern(" i ")
                .define('i', Items.IRON_INGOT)
                .pattern("i i")
                .pattern(" i ")
                .unlockedBy("has_item", ROOT_CRITERION)
                .group("iron_frame")
                .save(recipes);

        shaped(RecipeCategory.MISC, ItemInit.MESHED_IRON_FRAME.get(), 8)
                .pattern(" i ")
                .define('i', Items.IRON_INGOT)
                .pattern("ixi")
                .define('x', ItemInit.WIRE_MESH.get())
                .pattern(" i ")
                .unlockedBy("has_item", HAS_CHAMBER_DECORATION)
                .group("iron_frame")
                .save(recipes);

        shaped(RecipeCategory.MISC, ItemInit.BARRED_IRON_FRAME.get(), 8)
                .pattern(" i ")
                .define('i', Items.IRON_INGOT)
                .pattern("ixi")
                .define('x', Items.IRON_BARS)
                .pattern(" i ")
                .unlockedBy("has_item", HAS_CHAMBER_DECORATION)
                .group("iron_frame")
                .save(recipes);

        shaped(RecipeCategory.MISC, ItemInit.PLATFORM_BEAM.get(), 6)
                .pattern("c")
                .define('c', Items.IRON_CHAIN)
                .pattern("i")
                .define('i', Items.IRON_INGOT)
                .pattern("i")
                .unlockedBy("has_item", HAS_CHAMBER_DECORATION)
                .save(recipes);

        rustyVariant(recipes, ItemInit.RUSTY_IRON_FRAME.get(), ItemInit.IRON_FRAME.get(), "rusty_iron_frame");
        rustyVariant(
                recipes, ItemInit.RUSTY_MESHED_IRON_FRAME.get(), ItemInit.MESHED_IRON_FRAME.get(), "rusty_iron_frame");
        rustyVariant(
                recipes, ItemInit.RUSTY_BARRED_IRON_FRAME.get(), ItemInit.BARRED_IRON_FRAME.get(), "rusty_iron_frame");
        rustyVariant(recipes, ItemInit.RUSTY_PLATFORM_BEAM.get(), ItemInit.PLATFORM_BEAM.get(), "rusty_beam");

        // --- Stonecutter --- //

        stonecutting(recipes, ItemInit.LUNECAST.get(), ItemInit.LUNECAST_SLAB.get(), 2);
        stonecutting(recipes, ItemInit.LUNECAST.get(), ItemInit.LUNECAST_STAIRS.get());
        stonecutting(recipes, ItemInit.BLACKPLATE.get(), ItemInit.BLACKPLATE_SLAB.get(), 2);
        stonecutting(recipes, ItemInit.BLACKPLATE.get(), ItemInit.BLACKPLATE_STAIRS.get());
        stonecutting(recipes, ItemInit.ARBORED_LUNECAST.get(), ItemInit.ARBORED_LUNECAST_SLAB.get(), 2);
        stonecutting(recipes, ItemInit.ARBORED_LUNECAST.get(), ItemInit.ARBORED_LUNECAST_STAIRS.get());
        stonecutting(recipes, ItemInit.ARBORED_BLACKPLATE.get(), ItemInit.ARBORED_BLACKPLATE_SLAB.get(), 2);
        stonecutting(recipes, ItemInit.ARBORED_BLACKPLATE.get(), ItemInit.ARBORED_BLACKPLATE_STAIRS.get());
        stonecutting(recipes, ItemInit.ERODED_LUNECAST.get(), ItemInit.ERODED_LUNECAST_SLAB.get(), 2);
        stonecutting(recipes, ItemInit.ERODED_LUNECAST.get(), ItemInit.ERODED_LUNECAST_STAIRS.get());
        stonecutting(recipes, ItemInit.ERODED_BLACKPLATE.get(), ItemInit.ERODED_BLACKPLATE_SLAB.get(), 2);
        stonecutting(recipes, ItemInit.ERODED_BLACKPLATE.get(), ItemInit.ERODED_BLACKPLATE_STAIRS.get());
        stonecutting(recipes, ItemInit.FRACTURED_LUNECAST.get(), ItemInit.FRACTURED_LUNECAST_SLAB.get(), 2);
        stonecutting(recipes, ItemInit.FRACTURED_LUNECAST.get(), ItemInit.FRACTURED_LUNECAST_STAIRS.get());
        stonecutting(recipes, ItemInit.FRACTURED_BLACKPLATE.get(), ItemInit.FRACTURED_BLACKPLATE_SLAB.get(), 2);
        stonecutting(recipes, ItemInit.FRACTURED_BLACKPLATE.get(), ItemInit.FRACTURED_BLACKPLATE_STAIRS.get());
        stonecutting(recipes, ItemInit.VINTAGE_LUNECAST.get(), ItemInit.VINTAGE_LUNECAST_SLAB.get(), 2);
        stonecutting(recipes, ItemInit.VINTAGE_LUNECAST.get(), ItemInit.VINTAGE_LUNECAST_STAIRS.get());
        stonecutting(recipes, ItemInit.VINTAGE_BLACKPLATE.get(), ItemInit.VINTAGE_BLACKPLATE_SLAB.get(), 2);
        stonecutting(recipes, ItemInit.VINTAGE_BLACKPLATE.get(), ItemInit.VINTAGE_BLACKPLATE_STAIRS.get());
    }

    public void lunecastVariant(RecipeOutput recipes, ItemLike variant, Ingredient ingredient) {
        shapeless(RecipeCategory.MISC, variant)
                .requires(ItemInit.LUNECAST.get())
                .requires(ingredient)
                .unlockedBy("has_item", HAS_LUNECAST)
                .group("lunecast_variant")
                .save(recipes);
    }

    public void blackplateVariant(RecipeOutput recipes, ItemLike variant, Ingredient ingredient) {
        shapeless(RecipeCategory.MISC, variant)
                .requires(ItemInit.BLACKPLATE.get())
                .requires(ingredient)
                .unlockedBy("has_item", HAS_BLACKPLATE)
                .group("blackplate_variant")
                .save(recipes);
    }

    public void lunecastSlab(RecipeOutput recipes, ItemLike slab, ItemLike baseBlock) {
        slab(recipes, slab, baseBlock, "lunecast_slab");
    }

    public void blackplateSlab(RecipeOutput recipes, ItemLike slab, ItemLike baseBlock) {
        slab(recipes, slab, baseBlock, "blackplate_slab");
    }

    public void slab(RecipeOutput recipes, ItemLike slab, ItemLike baseBlock, String group) {
        shaped(RecipeCategory.MISC, slab, 6)
                .pattern("###")
                .define('#', baseBlock)
                .group(group)
                .unlockedBy("has_item", has(baseBlock))
                .save(recipes);
    }

    public void lunecastStairs(RecipeOutput recipes, ItemLike stairs, ItemLike baseBlock) {
        stairs(recipes, stairs, baseBlock, "lunecast_stairs");
    }

    public void blackplateStairs(RecipeOutput recipes, ItemLike stairs, ItemLike baseBlock) {
        stairs(recipes, stairs, baseBlock, "blackplate_stairs");
    }

    public void stairs(RecipeOutput recipes, ItemLike stairs, ItemLike baseBlock, String group) {
        shaped(RecipeCategory.MISC, stairs, 4)
                .pattern("#  ")
                .define('#', baseBlock)
                .pattern("## ")
                .pattern("###")
                .group(group)
                .unlockedBy("has_item", has(baseBlock))
                .save(recipes);
    }

    public void lunecastPlatform(RecipeOutput recipes, ItemLike platform, ItemLike baseBlock) {
        platform(recipes, platform, baseBlock, "lunecast_platform");
    }

    public void blackplatePlatform(RecipeOutput recipes, ItemLike platform, ItemLike baseBlock) {
        platform(recipes, platform, baseBlock, "blackplate_platform");
    }

    public void platform(RecipeOutput recipes, ItemLike platform, ItemLike baseBlock, String group) {
        shaped(RecipeCategory.MISC, platform)
                .pattern("s")
                .define('s', baseBlock)
                .pattern("i")
                .define('i', ItemTagInit.IRON_FRAMES)
                .group(group)
                .unlockedBy("has_item", has(baseBlock))
                .save(recipes);
    }

    public void rustyVariant(RecipeOutput recipes, ItemLike rusty, ItemLike regular, String group) {
        shapeless(RecipeCategory.MISC, rusty, 8)
                .requires(regular)
                .requires(regular)
                .requires(regular)
                .requires(regular)
                .requires(Items.WATER_BUCKET)
                .requires(regular)
                .requires(regular)
                .requires(regular)
                .requires(regular)
                .unlockedBy("has_item", HAS_CHAMBER_DECORATION)
                .group(group)
                .save(recipes);
    }

    public void stonecutting(RecipeOutput recipes, ItemLike ingredient, ItemLike output) {
        stonecutting(recipes, ingredient, output, 1);
    }

    public void stonecutting(RecipeOutput recipes, ItemLike ingredient, ItemLike output, int amount) {
        SingleItemRecipeBuilder.stonecutting(Ingredient.of(ingredient), RecipeCategory.MISC, output, amount)
                .unlockedBy("has_item", has(ItemInit.LUNECAST.get()))
                .save(
                        recipes,
                        "portalmod:"
                                + BuiltInRegistries.ITEM.getKey(output.asItem()).getPath()
                                + "_from_"
                                + BuiltInRegistries.ITEM.getKey(ingredient.asItem()).getPath()
                                + "_stonecutting");
    }
}
