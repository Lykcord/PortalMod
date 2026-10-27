package net.portalmod.common.sorted.turret;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.portalmod.PortalMod;

public class TurretEyeLayer extends EyesLayer<TurretRenderState, TurretModel> {
    private static final RenderType EYE =
            RenderTypes.eyes(Identifier.fromNamespaceAndPath(PortalMod.MODID, "textures/entity/turret/turret_eye.png"));

    public TurretEyeLayer(RenderLayerParent<TurretRenderState, TurretModel> parent) {
        super(parent);
    }

    @Override
    public void submit(
            PoseStack poses,
            SubmitNodeCollector collector,
            int light,
            TurretRenderState state,
            float yRot,
            float xRot) {
        if (state.turretState != TurretState.DEAD) {
            super.submit(poses, collector, light, state, yRot, xRot);
        }
    }

    @Override
    public RenderType renderType() {
        return EYE;
    }
}
