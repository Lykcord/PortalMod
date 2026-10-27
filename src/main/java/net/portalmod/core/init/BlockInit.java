package net.portalmod.core.init;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.portalmod.PortalMod;
import net.portalmod.common.blocks.*;
import net.portalmod.common.sorted.antline.AntlineBlock;
import net.portalmod.common.sorted.antline.indicator.AntlineDecoderBlock;
import net.portalmod.common.sorted.antline.indicator.AntlineEncoderBlock;
import net.portalmod.common.sorted.antline.indicator.AntlineIndicatorBlock;
import net.portalmod.common.sorted.antline.indicator.AntlineTimerBlock;
import net.portalmod.common.sorted.autoportal.AutoPortalBlock;
import net.portalmod.common.sorted.button.StandingButtonBlock;
import net.portalmod.common.sorted.button.SuperButtonBlock;
import net.portalmod.common.sorted.cubedropper.CubeDropperBlock;
import net.portalmod.common.sorted.door.ChamberDoorBlock;
import net.portalmod.common.sorted.faithplate.FaithPlateBlock;
import net.portalmod.common.sorted.fizzler.FizzlerEmitterBlock;
import net.portalmod.common.sorted.fizzler.FizzlerFieldBlock;
import net.portalmod.common.sorted.gel.PropulsionGelBlock;
import net.portalmod.common.sorted.gel.RepulsionGelBlock;
import net.portalmod.common.sorted.goo.GooBlock;
import net.portalmod.common.sorted.panel.PanelBlock;
import net.portalmod.common.sorted.platform.PlatformBeamBlock;
import net.portalmod.common.sorted.platform.PlatformBlock;
import net.portalmod.common.sorted.radio.RadioBlock;
import net.portalmod.common.sorted.trigger.TriggerBlock;
import net.portalmod.common.sorted.trigger.TriggerState;
import net.portalmod.core.util.Registry;
import net.portalmod.core.util.Registry.Entry;

import java.util.function.Function;

public class BlockInit {
    public static final Registry<Block> BLOCKS =
            new Registry<>(
                    (id, value) ->
                            net.minecraft.core.Registry.register(
                                    BuiltInRegistries.BLOCK,
                                    Identifier.fromNamespaceAndPath(PortalMod.MODID, id),
                                    value));

    private BlockInit() {}

    private static BlockBehaviour.Properties testingElement() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).pushReaction(PushReaction.POPPED);
    }

    public static final Entry<Block> LUNECAST = registerLunecast("lunecast");
    public static final Entry<Block> BLACKPLATE = registerBlackplate("blackplate");
    public static final Entry<Block> ARBORED_LUNECAST = registerLunecast("arbored_lunecast");
    public static final Entry<Block> ARBORED_BLACKPLATE = registerBlackplate("arbored_blackplate");
    public static final Entry<Block> ERODED_LUNECAST = registerLunecast("eroded_lunecast");
    public static final Entry<Block> ERODED_BLACKPLATE = registerBlackplate("eroded_blackplate");
    public static final Entry<Block> FRACTURED_LUNECAST = registerLunecast("fractured_lunecast");
    public static final Entry<Block> FRACTURED_BLACKPLATE = registerBlackplate("fractured_blackplate");
    public static final Entry<Block> VINTAGE_LUNECAST = registerLunecast("vintage_lunecast");
    public static final Entry<Block> VINTAGE_BLACKPLATE = registerBlackplate("vintage_blackplate");

    public static final Entry<Block> LUNECAST_SLAB = registerLunecastSlab("lunecast");
    public static final Entry<Block> BLACKPLATE_SLAB = registerBlackplateSlab("blackplate");
    public static final Entry<Block> ARBORED_LUNECAST_SLAB = registerLunecastSlab("arbored_lunecast");
    public static final Entry<Block> ARBORED_BLACKPLATE_SLAB = registerBlackplateSlab("arbored_blackplate");
    public static final Entry<Block> ERODED_LUNECAST_SLAB = registerLunecastSlab("eroded_lunecast");
    public static final Entry<Block> ERODED_BLACKPLATE_SLAB = registerBlackplateSlab("eroded_blackplate");
    public static final Entry<Block> FRACTURED_LUNECAST_SLAB = registerLunecastSlab("fractured_lunecast");
    public static final Entry<Block> FRACTURED_BLACKPLATE_SLAB = registerBlackplateSlab("fractured_blackplate");
    public static final Entry<Block> VINTAGE_LUNECAST_SLAB = registerLunecastSlab("vintage_lunecast");
    public static final Entry<Block> VINTAGE_BLACKPLATE_SLAB = registerBlackplateSlab("vintage_blackplate");

    public static final Entry<Block> LUNECAST_STAIRS = registerLunecastStairs("lunecast");
    public static final Entry<Block> BLACKPLATE_STAIRS = registerBlackplateStairs("blackplate");
    public static final Entry<Block> ARBORED_LUNECAST_STAIRS = registerLunecastStairs("arbored_lunecast");
    public static final Entry<Block> ARBORED_BLACKPLATE_STAIRS = registerBlackplateStairs("arbored_blackplate");
    public static final Entry<Block> ERODED_LUNECAST_STAIRS = registerLunecastStairs("eroded_lunecast");
    public static final Entry<Block> ERODED_BLACKPLATE_STAIRS = registerBlackplateStairs("eroded_blackplate");
    public static final Entry<Block> FRACTURED_LUNECAST_STAIRS = registerLunecastStairs("fractured_lunecast");
    public static final Entry<Block> FRACTURED_BLACKPLATE_STAIRS = registerBlackplateStairs("fractured_blackplate");
    public static final Entry<Block> VINTAGE_LUNECAST_STAIRS = registerLunecastStairs("vintage_lunecast");
    public static final Entry<Block> VINTAGE_BLACKPLATE_STAIRS = registerBlackplateStairs("vintage_blackplate");

    public static final Entry<Block> PLATFORM_BEAM =
            register(
                    "platform_beam",
                    key ->
                            new PlatformBeamBlock(
                                    BlockBehaviour.Properties.ofFullCopy(Blocks.CONCRETE.lightGray()).setId(key)));
    public static final Entry<Block> RUSTY_PLATFORM_BEAM =
            register(
                    "rusty_platform_beam",
                    key ->
                            new PlatformBeamBlock(
                                    BlockBehaviour.Properties.ofFullCopy(Blocks.CONCRETE.lightGray()).setId(key)));

    public static final Entry<Block> LUNECAST_PLATFORM = registerLunecastPlatform("lunecast_platform");
    public static final Entry<Block> BLACKPLATE_PLATFORM = registerBlackplatePlatform("blackplate_platform");
    public static final Entry<Block> ARBORED_LUNECAST_PLATFORM = registerLunecastPlatform("arbored_lunecast_platform");
    public static final Entry<Block> ARBORED_BLACKPLATE_PLATFORM =
            registerBlackplatePlatform("arbored_blackplate_platform");
    public static final Entry<Block> ERODED_LUNECAST_PLATFORM = registerLunecastPlatform("eroded_lunecast_platform");
    public static final Entry<Block> ERODED_BLACKPLATE_PLATFORM =
            registerBlackplatePlatform("eroded_blackplate_platform");
    public static final Entry<Block> FRACTURED_LUNECAST_PLATFORM =
            registerLunecastPlatform("fractured_lunecast_platform");
    public static final Entry<Block> FRACTURED_BLACKPLATE_PLATFORM =
            registerBlackplatePlatform("fractured_blackplate_platform");
    public static final Entry<Block> VINTAGE_LUNECAST_PLATFORM = registerLunecastPlatform("vintage_lunecast_platform");
    public static final Entry<Block> VINTAGE_BLACKPLATE_PLATFORM =
            registerBlackplatePlatform("vintage_blackplate_platform");

    public static final Entry<Block> IRON_FRAME =
            register(
                    "iron_frame",
                    key ->
                            new FrameBlock(
                                    BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).setId(key).noOcclusion(),
                                    false));

    public static final Entry<Block> BARRED_IRON_FRAME =
            register(
                    "barred_iron_frame",
                    key ->
                            new FrameBlock(
                                    BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).setId(key).noOcclusion(),
                                    true));

    public static final Entry<Block> MESHED_IRON_FRAME =
            register(
                    "meshed_iron_frame",
                    key ->
                            new FrameBlock(
                                    BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).setId(key).noOcclusion(),
                                    true));

    public static final Entry<Block> RUSTY_IRON_FRAME =
            register(
                    "rusty_iron_frame",
                    key ->
                            new FrameBlock(
                                    BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).setId(key).noOcclusion(),
                                    false));

    public static final Entry<Block> RUSTY_BARRED_IRON_FRAME =
            register(
                    "rusty_barred_iron_frame",
                    key ->
                            new FrameBlock(
                                    BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).setId(key).noOcclusion(),
                                    true));

    public static final Entry<Block> RUSTY_MESHED_IRON_FRAME =
            register(
                    "rusty_meshed_iron_frame",
                    key ->
                            new FrameBlock(
                                    BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).setId(key).noOcclusion(),
                                    true));

    public static final Entry<Block> RADIO =
            register("radio", key -> new RadioBlock(testingElement().setId(key).strength(1.0F)));

    public static final Entry<Block> FAITHPLATE =
            register("faithplate", key -> new FaithPlateBlock(stoneCopy(MapColor.COLOR_BLACK).setId(key)));

    public static final Entry<Block> WIRE_MESH_BLOCK =
            register(
                    "wire_mesh_block",
                    key -> new WireMeshBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_CHAIN).setId(key)));

    public static final Entry<Block> WIRE_MESH =
            register(
                    "wire_mesh",
                    key -> new IronBarsBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_CHAIN).setId(key)));

    public static final Entry<Block> FOREST_CAKE =
            register(
                    "forest_cake",
                    key -> new ForestCakeBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CAKE).setId(key)));

    public static final Entry<Block> CHAMBER_DOOR =
            register(
                    "chamber_door",
                    key ->
                            new ChamberDoorBlock(
                                    stoneCopy(MapColor.COLOR_BLACK).setId(key).sound(SoundType.STONE).noOcclusion()));

    public static final Entry<Block> CUBE_DROPPER =
            register(
                    "cube_dropper",
                    key ->
                            new CubeDropperBlock(
                                    BlockBehaviour.Properties.of().setId(key).mapColor(MapColor.STONE).noOcclusion()));

    public static final Entry<Block> PUSH_DOOR =
            register(
                    "push_door",
                    key -> new PushDoorBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_DOOR).setId(key)));

    public static final Entry<Block> STANDING_BUTTON =
            register(
                    "standing_button",
                    key -> new StandingButtonBlock(stoneCopy(MapColor.COLOR_RED).setId(key).noOcclusion()));

    public static final Entry<Block> SUPER_BUTTON =
            register(
                    "super_button",
                    key -> new SuperButtonBlock(stoneCopy(MapColor.COLOR_RED).setId(key).noOcclusion()));

    public static final Entry<Block> CHAMBER_LIGHTS =
            register(
                    "chamber_lights",
                    key ->
                            new ChamberLightsBlock(
                                    BlockBehaviour.Properties.ofFullCopy(Blocks.REDSTONE_LAMP)
                                            .setId(key)
                                            .lightLevel(i -> i.getValue(ChamberLightsBlock.ACTIVE) ? 15 : 0)));

    public static final Entry<Block> TRIGGER =
            register(
                    "trigger",
                    key ->
                            new TriggerBlock(
                                    BlockBehaviour.Properties.ofFullCopy(Blocks.COBBLESTONE_SLAB)
                                            .setId(key)
                                            .lightLevel(
                                                    i ->
                                                            i.getValue(TriggerBlock.STATE) == TriggerState.ACTIVE
                                                                    ? 15
                                                                    : 0)));

    public static final Entry<Block> AUTOPORTAL =
            register(
                    "autoportal",
                    key -> new AutoPortalBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).setId(key)));

    // Making just Antlines emissive doesn't make sense as we can't make the indicators etc have emissive layers (not in
    // 1.16 at least)
    public static final Entry<Block> ANTLINE =
            register(
                    "antline",
                    key ->
                            new AntlineBlock(
                                    testingElement()
                                            .setId(key)
                                            .mapColor(MapColor.COLOR_LIGHT_BLUE)
                                            .lightLevel(i -> 2)
                                            .noCollision()
                                            .instabreak()));

    public static final Entry<Block> ANTLINE_INDICATOR =
            register(
                    "antline_indicator",
                    key ->
                            new AntlineIndicatorBlock(
                                    testingElement()
                                            .setId(key)
                                            .noOcclusion()
                                            .strength(1.0F)
                                            .lightLevel(
                                                    i ->
                                                            7) /*.hasPostProcess(BlockInit::always).emissiveRendering(BlockInit::always)*/));

    public static final Entry<Block> ANTLINE_TIMER =
            register(
                    "antline_timer",
                    key ->
                            new AntlineTimerBlock(
                                    testingElement()
                                            .setId(key)
                                            .noOcclusion()
                                            .strength(1.0F)
                                            .lightLevel(
                                                    i ->
                                                            7) /*.hasPostProcess(BlockInit::always).emissiveRendering(BlockInit::always)*/));

    public static final Entry<Block> ANTLINE_DECODER =
            register(
                    "antline_decoder",
                    key ->
                            new AntlineDecoderBlock(
                                    testingElement()
                                            .setId(key)
                                            .noOcclusion()
                                            .strength(1.0F)
                                            .lightLevel(
                                                    i ->
                                                            7) /*.hasPostProcess(BlockInit::always).emissiveRendering(BlockInit::always)*/));

    public static final Entry<Block> ANTLINE_ENCODER =
            register(
                    "antline_encoder",
                    key ->
                            new AntlineEncoderBlock(
                                    testingElement()
                                            .setId(key)
                                            .noOcclusion()
                                            .strength(1.0F)
                                            .lightLevel(
                                                    i ->
                                                            7) /*.hasPostProcess(BlockInit::always).emissiveRendering(BlockInit::always)*/));

    public static final Entry<Block> FIZZLER_EMITTER =
            register(
                    "fizzler_emitter",
                    key ->
                            new FizzlerEmitterBlock(
                                    stoneCopy(MapColor.COLOR_BLACK)
                                            .setId(key)
                                            .noOcclusion()
                                            .lightLevel(
                                                    blockState ->
                                                            blockState.getValue(FizzlerEmitterBlock.ACTIVE) ? 10 : 0)));

    public static final Entry<Block> FIZZLER_FIELD =
            register(
                    "fizzler_field",
                    key ->
                            new FizzlerFieldBlock(
                                    BlockBehaviour.Properties.of()
                                            .setId(key)
                                            .noCollision()
                                            .noOcclusion()
                                            .strength(-1.0F, 3600000.0F)
                                            .noLootTable()
                                            .lightLevel(blockState -> 10)));

    public static final Entry<LiquidBlock> GOO =
            register(
                    "goo",
                    key ->
                            new GooBlock(
                                    FluidInit.GOO_FLUID,
                                    BlockBehaviour.Properties.of()
                                            .setId(key)
                                            .mapColor(MapColor.PODZOL)
                                            .noCollision()
                                            .liquid()
                                            .replaceable()
                                            .pushReaction(PushReaction.POPPED)));

    // GEL

    public static final Entry<Block> REPULSION_GEL =
            register(
                    "repulsion_gel",
                    key ->
                            new RepulsionGelBlock(
                                    testingElement()
                                            .setId(key)
                                            .mapColor(MapColor.COLOR_BLUE)
                                            .sound(SoundTypeInit.GEL)
                                            .noOcclusion()
                                            .noCollision()
                                            .strength(-1)));

    public static final Entry<Block> PROPULSION_GEL =
            register(
                    "propulsion_gel",
                    key ->
                            new PropulsionGelBlock(
                                    testingElement()
                                            .setId(key)
                                            .mapColor(MapColor.COLOR_ORANGE)
                                            .sound(SoundTypeInit.GEL)
                                            .noOcclusion()
                                            .noCollision()
                                            .strength(-1)));

    public static final Entry<Block> TEST_BLOCK =
            register(
                    "test_block",
                    key -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK).setId(key)));

    private static <T extends Block> Entry<T> register(String name, Function<ResourceKey<Block>, T> factory) {
        ResourceKey<Block> key =
                ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(PortalMod.MODID, name));
        return BLOCKS.register(name, () -> factory.apply(key));
    }

    public static Entry<Block> registerLunecast(String name) {
        return register(
                name, key -> new PanelBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CONCRETE.white()).setId(key)));
    }

    public static Entry<Block> registerBlackplate(String name) {
        return register(
                name, key -> new PanelBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CONCRETE.black()).setId(key)));
    }

    public static Entry<Block> registerLunecastPlatform(String name) {
        return register(
                name,
                key -> new PlatformBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CONCRETE.white()).setId(key)));
    }

    public static Entry<Block> registerBlackplatePlatform(String name) {
        return register(
                name,
                key -> new PlatformBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CONCRETE.black()).setId(key)));
    }

    public static Entry<Block> registerLunecastSlab(String name) {
        return register(
                name + "_slab",
                key -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CONCRETE.white()).setId(key)));
    }

    public static Entry<Block> registerBlackplateSlab(String name) {
        return register(
                name + "_slab",
                key -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CONCRETE.black()).setId(key)));
    }

    public static Entry<Block> registerLunecastStairs(String name) {
        return register(
                name + "_stairs",
                key ->
                        new StairBlock(
                                LUNECAST.get().defaultBlockState(),
                                BlockBehaviour.Properties.ofFullCopy(Blocks.CONCRETE.white()).setId(key)));
    }

    public static Entry<Block> registerBlackplateStairs(String name) {
        return register(
                name + "_stairs",
                key ->
                        new StairBlock(
                                BLACKPLATE.get().defaultBlockState(),
                                BlockBehaviour.Properties.ofFullCopy(Blocks.CONCRETE.black()).setId(key)));
    }

    public static BlockBehaviour.Properties stoneCopy(MapColor color) {
        return BlockBehaviour.Properties.of().mapColor(color).requiresCorrectToolForDrops().strength(1.5F, 6.0F);
    }

    public static boolean always(BlockState state, BlockGetter blockGetter, BlockPos pos) {
        return true;
    }
}
