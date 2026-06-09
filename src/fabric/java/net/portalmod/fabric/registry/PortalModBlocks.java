package net.portalmod.fabric.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.portalmod.fabric.PortalModFabric;
import net.portalmod.fabric.block.FrameBlock;
import net.portalmod.fabric.block.PanelBlock;
import net.portalmod.fabric.block.PortalStairBlock;
import net.portalmod.fabric.block.PlatformBeamBlock;
import net.portalmod.fabric.block.PlatformBlock;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

public final class PortalModBlocks {
    private static final Map<String, Block> DECORATIVE_BLOCKS = new LinkedHashMap<>();

    public static final Block LUNECAST = registerPanel("lunecast", Blocks.WHITE_CONCRETE);
    public static final Block BLACKPLATE = registerPanel("blackplate", Blocks.BLACK_CONCRETE);
    public static final Block ARBORED_LUNECAST = registerPanel("arbored_lunecast", Blocks.WHITE_CONCRETE);
    public static final Block ARBORED_BLACKPLATE = registerPanel("arbored_blackplate", Blocks.BLACK_CONCRETE);
    public static final Block ERODED_LUNECAST = registerPanel("eroded_lunecast", Blocks.WHITE_CONCRETE);
    public static final Block ERODED_BLACKPLATE = registerPanel("eroded_blackplate", Blocks.BLACK_CONCRETE);
    public static final Block FRACTURED_LUNECAST = registerPanel("fractured_lunecast", Blocks.WHITE_CONCRETE);
    public static final Block FRACTURED_BLACKPLATE = registerPanel("fractured_blackplate", Blocks.BLACK_CONCRETE);
    public static final Block VINTAGE_LUNECAST = registerPanel("vintage_lunecast", Blocks.WHITE_CONCRETE);
    public static final Block VINTAGE_BLACKPLATE = registerPanel("vintage_blackplate", Blocks.BLACK_CONCRETE);

    public static final Block LUNECAST_SLAB = registerSlab("lunecast_slab", Blocks.WHITE_CONCRETE);
    public static final Block BLACKPLATE_SLAB = registerSlab("blackplate_slab", Blocks.BLACK_CONCRETE);
    public static final Block ARBORED_LUNECAST_SLAB = registerSlab("arbored_lunecast_slab", Blocks.WHITE_CONCRETE);
    public static final Block ARBORED_BLACKPLATE_SLAB = registerSlab("arbored_blackplate_slab", Blocks.BLACK_CONCRETE);
    public static final Block ERODED_LUNECAST_SLAB = registerSlab("eroded_lunecast_slab", Blocks.WHITE_CONCRETE);
    public static final Block ERODED_BLACKPLATE_SLAB = registerSlab("eroded_blackplate_slab", Blocks.BLACK_CONCRETE);
    public static final Block FRACTURED_LUNECAST_SLAB = registerSlab("fractured_lunecast_slab", Blocks.WHITE_CONCRETE);
    public static final Block FRACTURED_BLACKPLATE_SLAB = registerSlab("fractured_blackplate_slab", Blocks.BLACK_CONCRETE);
    public static final Block VINTAGE_LUNECAST_SLAB = registerSlab("vintage_lunecast_slab", Blocks.WHITE_CONCRETE);
    public static final Block VINTAGE_BLACKPLATE_SLAB = registerSlab("vintage_blackplate_slab", Blocks.BLACK_CONCRETE);

    public static final Block LUNECAST_STAIRS = registerStairs("lunecast_stairs", LUNECAST);
    public static final Block BLACKPLATE_STAIRS = registerStairs("blackplate_stairs", BLACKPLATE);
    public static final Block ARBORED_LUNECAST_STAIRS = registerStairs("arbored_lunecast_stairs", ARBORED_LUNECAST);
    public static final Block ARBORED_BLACKPLATE_STAIRS = registerStairs("arbored_blackplate_stairs", ARBORED_BLACKPLATE);
    public static final Block ERODED_LUNECAST_STAIRS = registerStairs("eroded_lunecast_stairs", ERODED_LUNECAST);
    public static final Block ERODED_BLACKPLATE_STAIRS = registerStairs("eroded_blackplate_stairs", ERODED_BLACKPLATE);
    public static final Block FRACTURED_LUNECAST_STAIRS = registerStairs("fractured_lunecast_stairs", FRACTURED_LUNECAST);
    public static final Block FRACTURED_BLACKPLATE_STAIRS = registerStairs("fractured_blackplate_stairs", FRACTURED_BLACKPLATE);
    public static final Block VINTAGE_LUNECAST_STAIRS = registerStairs("vintage_lunecast_stairs", VINTAGE_LUNECAST);
    public static final Block VINTAGE_BLACKPLATE_STAIRS = registerStairs("vintage_blackplate_stairs", VINTAGE_BLACKPLATE);

    public static final Block LUNECAST_PLATFORM = registerPlatform("lunecast_platform", Blocks.WHITE_CONCRETE);
    public static final Block BLACKPLATE_PLATFORM = registerPlatform("blackplate_platform", Blocks.BLACK_CONCRETE);
    public static final Block ARBORED_LUNECAST_PLATFORM = registerPlatform("arbored_lunecast_platform", Blocks.WHITE_CONCRETE);
    public static final Block ARBORED_BLACKPLATE_PLATFORM = registerPlatform("arbored_blackplate_platform", Blocks.BLACK_CONCRETE);
    public static final Block ERODED_LUNECAST_PLATFORM = registerPlatform("eroded_lunecast_platform", Blocks.WHITE_CONCRETE);
    public static final Block ERODED_BLACKPLATE_PLATFORM = registerPlatform("eroded_blackplate_platform", Blocks.BLACK_CONCRETE);
    public static final Block FRACTURED_LUNECAST_PLATFORM = registerPlatform("fractured_lunecast_platform", Blocks.WHITE_CONCRETE);
    public static final Block FRACTURED_BLACKPLATE_PLATFORM = registerPlatform("fractured_blackplate_platform", Blocks.BLACK_CONCRETE);
    public static final Block VINTAGE_LUNECAST_PLATFORM = registerPlatform("vintage_lunecast_platform", Blocks.WHITE_CONCRETE);
    public static final Block VINTAGE_BLACKPLATE_PLATFORM = registerPlatform("vintage_blackplate_platform", Blocks.BLACK_CONCRETE);

    public static final Block IRON_FRAME = registerFrame("iron_frame", Blocks.IRON_BLOCK);
    public static final Block BARRED_IRON_FRAME = registerFrame("barred_iron_frame", Blocks.IRON_BLOCK);
    public static final Block MESHED_IRON_FRAME = registerFrame("meshed_iron_frame", Blocks.IRON_BLOCK);
    public static final Block RUSTY_IRON_FRAME = registerFrame("rusty_iron_frame", Blocks.IRON_BLOCK);
    public static final Block RUSTY_BARRED_IRON_FRAME = registerFrame("rusty_barred_iron_frame", Blocks.IRON_BLOCK);
    public static final Block RUSTY_MESHED_IRON_FRAME = registerFrame("rusty_meshed_iron_frame", Blocks.IRON_BLOCK);
    public static final Block WIRE_MESH_BLOCK = registerNoOcclusion("wire_mesh_block", Blocks.IRON_BARS);
    public static final Block WIRE_MESH = registerIronBars("wire_mesh", Blocks.IRON_BARS);

    public static final Block PLATFORM_BEAM = registerPlatformBeam("platform_beam", Blocks.LIGHT_GRAY_CONCRETE);
    public static final Block RUSTY_PLATFORM_BEAM = registerPlatformBeam("rusty_platform_beam", Blocks.LIGHT_GRAY_CONCRETE);

    private PortalModBlocks() {
    }

    public static void register() {
        PortalModFabric.LOGGER.info("Registered {} PortalMod decorative blocks.", DECORATIVE_BLOCKS.size());
    }

    public static Iterable<Map.Entry<String, Block>> decorativeBlocks() {
        return DECORATIVE_BLOCKS.entrySet();
    }

    private static Block registerPanel(String name, Block base) {
        return register(name, PanelBlock::new, BlockBehaviour.Properties.ofFullCopy(base));
    }

    private static Block registerNoOcclusion(String name, Block base) {
        return register(name, properties -> new Block(properties), BlockBehaviour.Properties.ofFullCopy(base).noOcclusion());
    }

    private static Block registerSlab(String name, Block base) {
        return register(name, SlabBlock::new, BlockBehaviour.Properties.ofFullCopy(base));
    }

    private static Block registerIronBars(String name, Block base) {
        return register(name, IronBarsBlock::new, BlockBehaviour.Properties.ofFullCopy(base).noOcclusion());
    }

    private static Block registerStairs(String name, Block base) {
        return register(name, properties -> new PortalStairBlock(base.defaultBlockState(), properties), BlockBehaviour.Properties.ofFullCopy(base));
    }

    private static Block registerPlatform(String name, Block base) {
        return register(name, PlatformBlock::new, BlockBehaviour.Properties.ofFullCopy(base).noOcclusion());
    }

    private static Block registerPlatformBeam(String name, Block base) {
        return register(name, PlatformBeamBlock::new, BlockBehaviour.Properties.ofFullCopy(base).noOcclusion());
    }

    private static Block registerFrame(String name, Block base) {
        return register(name, FrameBlock::new, BlockBehaviour.Properties.ofFullCopy(base).noOcclusion());
    }

    private static Block register(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
        Identifier id = Identifier.fromNamespaceAndPath(PortalModFabric.MOD_ID, name);
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id);
        Block block = factory.apply(properties.setId(key));
        DECORATIVE_BLOCKS.put(name, block);
        return Registry.register(BuiltInRegistries.BLOCK, id, block);
    }
}
