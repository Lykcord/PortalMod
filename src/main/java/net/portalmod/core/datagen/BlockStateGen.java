package net.portalmod.core.datagen;

import com.mojang.math.Quadrant;

import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.minecraft.client.color.item.GrassColorSource;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.client.renderer.item.EmptyModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.portalmod.PortalMod;
import net.portalmod.common.sorted.panel.PanelBlock;
import net.portalmod.common.sorted.panel.PanelState;
import net.portalmod.common.sorted.portalgun.PortalGun;
import net.portalmod.core.init.BlockInit;
import net.portalmod.core.init.ItemInit;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class BlockStateGen extends FabricModelProvider {

    private BlockModelGenerators models;

    public BlockStateGen(FabricPackOutput output) {
        super(output);
    }

    @Override
    public void generateItemModels(ItemModelGenerators items) {
        ItemInit.ITEMS
                .getRegistry()
                .forEach(
                        (name, entry) -> {
                            var item = entry.get();
                            if (item instanceof PortalGun) {
                                items.itemModelOutput.accept(item, new EmptyModel.Unbaked());
                                return;
                            }
                            var model = Identifier.fromNamespaceAndPath(PortalMod.MODID, "item/" + name);
                            items.itemModelOutput.accept(
                                    item,
                                    name.startsWith("arbored_blackplate")
                                            ? ItemModelUtils.tintedModel(model, new GrassColorSource(.5f, 1))
                                            : ItemModelUtils.plainModel(model));
                        });
    }

    @Override
    public void generateBlockStateModels(BlockModelGenerators generators) {
        models = generators;
        genPanel(BlockInit.LUNECAST.get(), false);
        genPanel(BlockInit.ARBORED_LUNECAST.get(), false);
        genPanel(BlockInit.ERODED_LUNECAST.get(), false);
        genPanel(BlockInit.FRACTURED_LUNECAST.get(), false);
        genPanel(BlockInit.VINTAGE_LUNECAST.get(), false);

        genPanel(BlockInit.BLACKPLATE.get(), true);
        genPanel(BlockInit.ARBORED_BLACKPLATE.get(), "", "", false, true); // Blockstates are manually handled
        genPanel(BlockInit.ARBORED_BLACKPLATE.get(), "_foliage", "_tint", false, true); // Foliage overlay
        genPanel(BlockInit.ERODED_BLACKPLATE.get(), true);
        genPanel(BlockInit.FRACTURED_BLACKPLATE.get(), true);
        genPanel(BlockInit.VINTAGE_BLACKPLATE.get(), true);

        genPanelSlab(BlockInit.LUNECAST_SLAB.get(), "lunecast");
        genPanelSlab(BlockInit.ARBORED_LUNECAST_SLAB.get(), "arbored_lunecast");
        genPanelSlab(BlockInit.ERODED_LUNECAST_SLAB.get(), "eroded_lunecast");
        genPanelSlab(BlockInit.FRACTURED_LUNECAST_SLAB.get(), "fractured_lunecast");
        genPanelSlab(BlockInit.VINTAGE_LUNECAST_SLAB.get(), "vintage_lunecast");

        genPanelSlab(BlockInit.BLACKPLATE_SLAB.get(), "blackplate");
        genPanelSlab(BlockInit.ERODED_BLACKPLATE_SLAB.get(), "eroded_blackplate");
        genPanelSlab(BlockInit.FRACTURED_BLACKPLATE_SLAB.get(), "fractured_blackplate");
        genPanelSlab(BlockInit.VINTAGE_BLACKPLATE_SLAB.get(), "vintage_blackplate");

        genPanelStairs(BlockInit.LUNECAST_STAIRS.get(), "lunecast");
        genPanelStairs(BlockInit.ARBORED_LUNECAST_STAIRS.get(), "arbored_lunecast");
        genPanelStairs(BlockInit.ERODED_LUNECAST_STAIRS.get(), "eroded_lunecast");
        genPanelStairs(BlockInit.FRACTURED_LUNECAST_STAIRS.get(), "fractured_lunecast");
        genPanelStairsFull(BlockInit.VINTAGE_LUNECAST_STAIRS.get(), "vintage_lunecast");

        genPanelStairs(BlockInit.BLACKPLATE_STAIRS.get(), "blackplate");
        genPanelStairs(BlockInit.ERODED_BLACKPLATE_STAIRS.get(), "eroded_blackplate");
        genPanelStairs(BlockInit.FRACTURED_BLACKPLATE_STAIRS.get(), "fractured_blackplate");
        genPanelStairs(BlockInit.VINTAGE_BLACKPLATE_STAIRS.get(), "vintage_blackplate");
    }

    public void genPanel(Block block, boolean blackplateYouBe) {
        this.genPanel(block, "", "", true, blackplateYouBe);
    }

    public void genPanel(
            Block block, String suffix, String parentSuffix, boolean genBlockStateFile, boolean blackplateYouBe) {
        String name = BuiltInRegistries.BLOCK.getKey(block).getPath();

        String parentPrefix = Objects.equals(parentSuffix, "") ? "minecraft:" : "portalmod:";
        Identifier singleModel =
                model(
                        name + suffix,
                        parentPrefix + "block/cube_bottom_top" + parentSuffix,
                        Map.of(
                                "side",
                                blockPath(name + suffix),
                                "top",
                                blockPath(name + "_top" + suffix),
                                "bottom",
                                blockPath(name + "_bottom" + suffix)));

        // Double

        Identifier topModel =
                model(
                        name + "_panel_top" + suffix,
                        "portalmod:block/cube_part_top" + parentSuffix,
                        Map.of(
                                "side",
                                blockPath(name + "_panel_top" + suffix),
                                "top",
                                blockPath(name + "_top" + suffix)));

        Identifier bottomModel =
                model(
                        name + "_panel_bottom" + suffix,
                        "portalmod:block/cube_part_bottom" + parentSuffix,
                        Map.of(
                                "side",
                                blockPath(name + "_panel_bottom" + suffix),
                                "bottom",
                                blockPath(name + "_bottom" + suffix)));

        // Wall

        Identifier cornerBottomModel =
                model(
                        name + "_panel_corner_bottom" + suffix,
                        "portalmod:block/cube_corner_bottom" + parentSuffix,
                        Map.of(
                                "bottom",
                                blockPath(name + "_bottom" + suffix),
                                "back",
                                blockPath(name + "_panel_bottom" + suffix),
                                "left",
                                blockPath(name + "_panel_bottom_left" + suffix),
                                "right",
                                blockPath(name + "_panel_bottom_right" + suffix)));

        Identifier cornerTopModel =
                model(
                        name + "_panel_corner_top" + suffix,
                        "portalmod:block/cube_corner_top" + parentSuffix,
                        Map.of(
                                "top",
                                blockPath(name + "_top" + suffix),
                                "back",
                                blockPath(name + "_panel_top" + suffix),
                                "left",
                                blockPath(name + "_panel_top_left" + suffix),
                                "right",
                                blockPath(name + "_panel_top_right" + suffix)));

        // Floor
        if (blackplateYouBe) {
            Identifier floorBottomLeftModel =
                    model(
                            name + "_panel_floor_bottom_left" + suffix,
                            "portalmod:block/cube_corner_floor" + parentSuffix,
                            Map.of(
                                    "top",
                                    blockPath(name + "_panel_floor_bottom_left" + suffix),
                                    "bottom",
                                    blockPath(name + "_panel_ceiling_bottom_right" + suffix),
                                    "side",
                                    blockPath(name + suffix)));

            Identifier floorBottomRightModel =
                    model(
                            name + "_panel_floor_bottom_right" + suffix,
                            "portalmod:block/cube_corner_floor" + parentSuffix,
                            Map.of(
                                    "top",
                                    blockPath(name + "_panel_floor_bottom_right" + suffix),
                                    "bottom",
                                    blockPath(name + "_panel_ceiling_bottom_left" + suffix),
                                    "side",
                                    blockPath(name + suffix)));

            Identifier floorTopLeftModel =
                    model(
                            name + "_panel_floor_top_left" + suffix,
                            "portalmod:block/cube_corner_floor" + parentSuffix,
                            Map.of(
                                    "top",
                                    blockPath(name + "_panel_floor_top_left" + suffix),
                                    "bottom",
                                    blockPath(name + "_panel_ceiling_top_right" + suffix),
                                    "side",
                                    blockPath(name + suffix)));

            Identifier floorTopRightModel =
                    model(
                            name + "_panel_floor_top_right" + suffix,
                            "portalmod:block/cube_corner_floor" + parentSuffix,
                            Map.of(
                                    "top",
                                    blockPath(name + "_panel_floor_top_right" + suffix),
                                    "bottom",
                                    blockPath(name + "_panel_ceiling_top_left" + suffix),
                                    "side",
                                    blockPath(name + suffix)));
        }

        if (!blackplateYouBe) {
            Identifier floorXBottomLeftModel =
                    model(
                            name + "_panel_floor_x_bottom_left" + suffix,
                            "portalmod:block/cube_corner_floor" + parentSuffix,
                            Map.of(
                                    "top",
                                    blockPath(name + "_panel_floor_x_bottom_left" + suffix),
                                    "bottom",
                                    blockPath(name + "_panel_ceiling_x_top_left" + suffix),
                                    "side",
                                    blockPath(name + suffix)));

            Identifier floorZBottomLeftModel =
                    model(
                            name + "_panel_floor_z_bottom_left" + suffix,
                            "portalmod:block/cube_corner_floor" + parentSuffix,
                            Map.of(
                                    "top",
                                    blockPath(name + "_panel_floor_z_bottom_left" + suffix),
                                    "bottom",
                                    blockPath(name + "_panel_ceiling_z_top_left" + suffix),
                                    "side",
                                    blockPath(name + suffix)));

            Identifier floorXBottomRightModel =
                    model(
                            name + "_panel_floor_x_bottom_right" + suffix,
                            "portalmod:block/cube_corner_floor" + parentSuffix,
                            Map.of(
                                    "top",
                                    blockPath(name + "_panel_floor_x_bottom_right" + suffix),
                                    "bottom",
                                    blockPath(name + "_panel_ceiling_x_top_right" + suffix),
                                    "side",
                                    blockPath(name + suffix)));

            Identifier floorZBottomRightModel =
                    model(
                            name + "_panel_floor_z_bottom_right" + suffix,
                            "portalmod:block/cube_corner_floor" + parentSuffix,
                            Map.of(
                                    "top",
                                    blockPath(name + "_panel_floor_z_bottom_right" + suffix),
                                    "bottom",
                                    blockPath(name + "_panel_ceiling_z_top_right" + suffix),
                                    "side",
                                    blockPath(name + suffix)));

            Identifier floorXTopLeftModel =
                    model(
                            name + "_panel_floor_x_top_left" + suffix,
                            "portalmod:block/cube_corner_floor" + parentSuffix,
                            Map.of(
                                    "top",
                                    blockPath(name + "_panel_floor_x_top_left" + suffix),
                                    "bottom",
                                    blockPath(name + "_panel_ceiling_x_bottom_left" + suffix),
                                    "side",
                                    blockPath(name + suffix)));

            Identifier floorZTopLeftModel =
                    model(
                            name + "_panel_floor_z_top_left" + suffix,
                            "portalmod:block/cube_corner_floor" + parentSuffix,
                            Map.of(
                                    "top",
                                    blockPath(name + "_panel_floor_z_top_left" + suffix),
                                    "bottom",
                                    blockPath(name + "_panel_ceiling_z_bottom_left" + suffix),
                                    "side",
                                    blockPath(name + suffix)));

            Identifier floorXTopRightModel =
                    model(
                            name + "_panel_floor_x_top_right" + suffix,
                            "portalmod:block/cube_corner_floor" + parentSuffix,
                            Map.of(
                                    "top",
                                    blockPath(name + "_panel_floor_x_top_right" + suffix),
                                    "bottom",
                                    blockPath(name + "_panel_ceiling_x_bottom_right" + suffix),
                                    "side",
                                    blockPath(name + suffix)));

            Identifier floorZTopRightModel =
                    model(
                            name + "_panel_floor_z_top_right" + suffix,
                            "portalmod:block/cube_corner_floor" + parentSuffix,
                            Map.of(
                                    "top",
                                    blockPath(name + "_panel_floor_z_top_right" + suffix),
                                    "bottom",
                                    blockPath(name + "_panel_ceiling_z_bottom_right" + suffix),
                                    "side",
                                    blockPath(name + suffix)));
        }

        if (!genBlockStateFile) {
            return;
        }
        models.blockStateOutput.accept(
                MultiVariantGenerator.dispatch(block)
                        .with(
                                PropertyDispatch.initial(PanelBlock.AXIS, PanelBlock.STATE)
                                        .generate(
                                                (axis, state) ->
                                                        BlockModelGenerators.variant(
                                                                panelVariant(
                                                                        name, suffix, blackplateYouBe, axis, state)))));
    }

    static Variant panelVariant(String name, String suffix, boolean blackplate, Direction.Axis axis, PanelState state) {
        String part;
        int rotation = 0;
        if (state == PanelState.SINGLE) {
            part = "";
        } else if (state == PanelState.TOP) {
            part = "_panel_top";
        } else if (state == PanelState.BOTTOM) {
            part = "_panel_bottom";
        } else if (state.isFloor()) {
            String direction = blackplate ? "" : axis.getSerializedName() + "_";
            part = "_panel_floor_" + direction + state.getSerializedName().substring(6);
            rotation =
                    switch (state) {
                        case FLOOR_BOTTOM_LEFT -> 180;
                        case FLOOR_BOTTOM_RIGHT -> 90;
                        case FLOOR_TOP_LEFT -> 270;
                        default -> 0;
                    };
        } else {
            part = state.isBottom() ? "_panel_corner_bottom" : "_panel_corner_top";
            rotation = (axis == Direction.Axis.X ? 90 : 0) + (state.isLeft() ? 0 : 180);
        }
        return new Variant(blockPath(name + part + suffix))
                .withYRot(Quadrant.values()[rotation / 90])
                .withUvLock(state.isFloor() || rotation != 0);
    }

    private Identifier model(String name, String parent, Map<String, Identifier> textures) {
        var mapping = new TextureMapping();
        var slots =
                textures.entrySet().stream()
                        .map(
                                entry -> {
                                    var slot = TextureSlot.create(entry.getKey());
                                    mapping.put(slot, new Material(entry.getValue()));
                                    return slot;
                                })
                        .toArray(TextureSlot[]::new);
        return new ModelTemplate(Optional.of(Identifier.parse(parent)), Optional.empty(), slots)
                .create(blockPath(name), mapping, models.modelOutput);
    }

    public void genPanelSlab(Block block, String name) {
        this.panelSlabBlock(
                (SlabBlock) block,
                name,
                blockPath(name + "_tiles"),
                blockPath(name + "_slab_bottom"),
                blockPath(name + "_slab_top"));
    }

    public void panelSlabBlock(SlabBlock block, String name, Identifier side, Identifier bottom, Identifier top) {
        var textures = panelTextures(side, bottom, top);
        var lower = ModelTemplates.SLAB_BOTTOM.create(blockPath(name + "_slab"), textures, models.modelOutput);
        var upper = ModelTemplates.SLAB_TOP.create(blockPath(name + "_slab_top"), textures, models.modelOutput);
        var doubled =
                ModelTemplates.CUBE_BOTTOM_TOP.create(blockPath(name + "_slab_double"), textures, models.modelOutput);
        models.blockStateOutput.accept(
                BlockModelGenerators.createSlab(
                        block,
                        BlockModelGenerators.plainVariant(lower),
                        BlockModelGenerators.plainVariant(upper),
                        BlockModelGenerators.plainVariant(doubled)));
    }

    private static TextureMapping panelTextures(Identifier side, Identifier bottom, Identifier top) {
        return new TextureMapping()
                .put(TextureSlot.SIDE, new Material(side))
                .put(TextureSlot.BOTTOM, new Material(bottom))
                .put(TextureSlot.TOP, new Material(top));
    }

    public void genPanelStairs(Block block, String name) {
        stairs(
                (StairBlock) block,
                panelTextures(blockPath(name + "_tiles"), blockPath(name + "_bottom"), blockPath(name + "_top")));
    }

    public void genPanelStairsFull(Block block, String name) {
        var texture = blockPath(name + "_tiles");
        stairs((StairBlock) block, panelTextures(texture, texture, texture));
    }

    private void stairs(StairBlock block, TextureMapping textures) {
        var straight = ModelTemplates.STAIRS_STRAIGHT.create(block, textures, models.modelOutput);
        var inner = ModelTemplates.STAIRS_INNER.create(block, textures, models.modelOutput);
        var outer = ModelTemplates.STAIRS_OUTER.create(block, textures, models.modelOutput);
        models.blockStateOutput.accept(
                BlockModelGenerators.createStairs(
                        block,
                        BlockModelGenerators.plainVariant(straight),
                        BlockModelGenerators.plainVariant(inner),
                        BlockModelGenerators.plainVariant(outer)));
    }

    public static Identifier blockPath(String name) {
        return Identifier.fromNamespaceAndPath(PortalMod.MODID, "block/" + name);
    }
}
