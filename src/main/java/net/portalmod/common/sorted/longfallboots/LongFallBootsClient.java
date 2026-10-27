package net.portalmod.common.sorted.longfallboots;

import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.portalmod.PortalMod;
import net.portalmod.core.init.ItemInit;

public final class LongFallBootsClient {
    private static final ModelLayerLocation LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(PortalMod.MODID, "longfall_boots"), "main");
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(PortalMod.MODID, "textures/models/armor/longfall_boots.png");

    public static void init() {
        ModelLayerRegistry.registerModelLayer(LAYER, LongFallBootsModel::createLayer);
        ArmorRenderer.register(
                context -> {
                    LongFallBootsModel model = new LongFallBootsModel(context.bakeLayer(LAYER));
                    return (poses, collector, stack, state, slot, light, parent) -> {
                        if (slot == EquipmentSlot.FEET) {
                            ArmorRenderer.submitTransformCopyingModel(
                                    parent,
                                    state,
                                    model,
                                    state,
                                    false,
                                    collector,
                                    poses,
                                    stack.hasFoil()
                                            ? RenderTypes.armorCutoutNoCullGlint(TEXTURE)
                                            : model.renderType(TEXTURE),
                                    light,
                                    OverlayTexture.NO_OVERLAY,
                                    -1);
                        }
                    };
                },
                ItemInit.LONGFALL_BOOTS.get());
    }
}
