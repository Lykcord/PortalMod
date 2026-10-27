package net.portalmod.common.sorted.cube.companion;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.portalmod.PortalMod;
import net.portalmod.common.sorted.cube.CubeModel;
import net.portalmod.common.sorted.cube.CubeRenderState;

public class CompanionCubeGlowLayer extends RenderLayer<CubeRenderState, CubeModel> {
    private static final RenderType GLOW =
            RenderTypes.entityCutout(
                    Identifier.fromNamespaceAndPath(PortalMod.MODID, "textures/entity/cube/companion_cube_glow.png"));
    private static final RenderType ACTIVE_GLOW =
            RenderTypes.entityCutout(
                    Identifier.fromNamespaceAndPath(
                            PortalMod.MODID, "textures/entity/cube/companion_cube_active_glow.png"));

    public CompanionCubeGlowLayer(RenderLayerParent<CubeRenderState, CubeModel> parent) {
        super(parent);
    }

    @Override
    public void submit(
            PoseStack poses, SubmitNodeCollector collector, int light, CubeRenderState state, float yRot, float xRot) {
        int glowLight =
                LightCoordsUtil.pack(
                        Math.max(LightCoordsUtil.block(light), 9), Math.max(LightCoordsUtil.sky(light), 9));
        collector.submitModel(
                getParentModel(),
                state,
                poses,
                state.active ? ACTIVE_GLOW : GLOW,
                glowLight,
                OverlayTexture.NO_OVERLAY,
                state.outlineColor);
    }
}
