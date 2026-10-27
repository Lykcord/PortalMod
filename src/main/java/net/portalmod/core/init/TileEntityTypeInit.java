package net.portalmod.core.init;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.portalmod.PortalMod;
import net.portalmod.common.sorted.antline.AntlineTileEntity;
import net.portalmod.common.sorted.autoportal.AutoPortalTileEntity;
import net.portalmod.common.sorted.cubedropper.CubeDropperTileEntity;
import net.portalmod.common.sorted.door.ChamberDoorTileEntity;
import net.portalmod.common.sorted.faithplate.FaithPlateTileEntity;
import net.portalmod.common.sorted.fizzler.FizzlerEmitterTileEntity;
import net.portalmod.common.sorted.radio.RadioBlockTileEntity;
import net.portalmod.common.sorted.trigger.TriggerTileEntity;
import net.portalmod.core.util.Registry;
import net.portalmod.core.util.Registry.Entry;

import java.util.HashSet;
import java.util.Set;
import java.util.function.UnaryOperator;

@SuppressWarnings("DataFlowIssue")
public class TileEntityTypeInit {
    public static final Registry<BlockEntityType<?>> TILE_ENTITY_TYPES =
            new Registry<>(
                    (id, value) ->
                            net.minecraft.core.Registry.register(
                                    BuiltInRegistries.BLOCK_ENTITY_TYPE,
                                    Identifier.fromNamespaceAndPath(PortalMod.MODID, id),
                                    value));

    public static final Entry<BlockEntityType<RadioBlockTileEntity>> RADIO =
            TILE_ENTITY_TYPES.register(
                    "radio",
                    () ->
                            FabricBlockEntityTypeBuilder.create(
                                            RadioBlockTileEntity::new, getBlocks(TileEntityTypeInit::getRadioBlocks))
                                    .build());

    public static final Entry<BlockEntityType<AntlineTileEntity>> ANTLINE =
            TILE_ENTITY_TYPES.register(
                    "antline",
                    () ->
                            FabricBlockEntityTypeBuilder.create(
                                            AntlineTileEntity::new, getBlocks(TileEntityTypeInit::getAntlineBlocks))
                                    .build());

    public static final Entry<BlockEntityType<FaithPlateTileEntity>> FAITHPLATE =
            TILE_ENTITY_TYPES.register(
                    "faithplate",
                    () ->
                            FabricBlockEntityTypeBuilder.create(
                                            FaithPlateTileEntity::new,
                                            getBlocks(TileEntityTypeInit::getFaithplateBlocks))
                                    .build());

    public static final Entry<BlockEntityType<ChamberDoorTileEntity>> CHAMBER_DOOR =
            TILE_ENTITY_TYPES.register(
                    "chamber_door",
                    () ->
                            FabricBlockEntityTypeBuilder.create(
                                            ChamberDoorTileEntity::new,
                                            getBlocks(TileEntityTypeInit::getChamberDoorBlocks))
                                    .build());

    public static final Entry<BlockEntityType<CubeDropperTileEntity>> CUBE_DROPPER =
            TILE_ENTITY_TYPES.register(
                    "cube_dropper",
                    () ->
                            FabricBlockEntityTypeBuilder.create(
                                            CubeDropperTileEntity::new,
                                            getBlocks(TileEntityTypeInit::getCubeDropperBlocks))
                                    .build());

    public static final Entry<BlockEntityType<FizzlerEmitterTileEntity>> FIZZLER_EMITTER =
            TILE_ENTITY_TYPES.register(
                    "fizzler_emitter",
                    () ->
                            FabricBlockEntityTypeBuilder.create(
                                            FizzlerEmitterTileEntity::new,
                                            getBlocks(TileEntityTypeInit::getFizzlerEmitterBlocks))
                                    .build());

    public static final Entry<BlockEntityType<TriggerTileEntity>> TRIGGER =
            TILE_ENTITY_TYPES.register(
                    "trigger",
                    () ->
                            FabricBlockEntityTypeBuilder.create(
                                            TriggerTileEntity::new, getBlocks(TileEntityTypeInit::getTriggerBlocks))
                                    .build());

    public static final Entry<BlockEntityType<AutoPortalTileEntity>> AUTOPORTAL =
            TILE_ENTITY_TYPES.register(
                    "autoportal",
                    () ->
                            FabricBlockEntityTypeBuilder.create(
                                            AutoPortalTileEntity::new,
                                            getBlocks(TileEntityTypeInit::getAutoPortalBlocks))
                                    .build());

    public static Block[] getBlocks(UnaryOperator<Set<Block>> function) {
        return function.apply(new HashSet<>()).toArray(new Block[] {});
    }

    public static Set<Block> getRadioBlocks(Set<Block> blocks) {
        blocks.add(BlockInit.RADIO.get());
        return blocks;
    }

    public static Set<Block> getAntlineBlocks(Set<Block> blocks) {
        blocks.add(BlockInit.ANTLINE.get());
        return blocks;
    }

    public static Set<Block> getFaithplateBlocks(Set<Block> blocks) {
        blocks.add(BlockInit.FAITHPLATE.get());
        return blocks;
    }

    public static Set<Block> getChamberDoorBlocks(Set<Block> blocks) {
        blocks.add(BlockInit.CHAMBER_DOOR.get());
        return blocks;
    }

    public static Set<Block> getCubeDropperBlocks(Set<Block> blocks) {
        blocks.add(BlockInit.CUBE_DROPPER.get());
        return blocks;
    }

    public static Set<Block> getFizzlerEmitterBlocks(Set<Block> blocks) {
        blocks.add(BlockInit.FIZZLER_EMITTER.get());
        return blocks;
    }

    public static Set<Block> getTriggerBlocks(Set<Block> blocks) {
        blocks.add(BlockInit.TRIGGER.get());
        return blocks;
    }

    public static Set<Block> getAutoPortalBlocks(Set<Block> blocks) {
        blocks.add(BlockInit.AUTOPORTAL.get());
        return blocks;
    }
}
