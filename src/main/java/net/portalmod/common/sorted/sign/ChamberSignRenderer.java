package net.portalmod.common.sorted.sign;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;

public class ChamberSignRenderer extends EntityRenderer<ChamberSignEntity, ChamberSignRenderState> {
    public static final Identifier TEXTURE_ON =
            Identifier.fromNamespaceAndPath("portalmod", "textures/entity/chamber_sign/chamber_sign_on.png");
    public static final Identifier TEXTURE_OFF =
            Identifier.fromNamespaceAndPath("portalmod", "textures/entity/chamber_sign/chamber_sign_off.png");
    private final ChamberSignModel model = new ChamberSignModel();

    public ChamberSignRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ChamberSignRenderState createRenderState() {
        return new ChamberSignRenderState();
    }

    @Override
    public void extractRenderState(ChamberSignEntity entity, ChamberSignRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.enabled = entity.getEnabled();
        state.yRot = entity.getYRot();
        state.progress = entity.getProgress();
        state.leftDigit = entity.getLeftDigit();
        state.rightDigit = entity.getRightDigit();
        state.icons =
                (entity.getIcon1() ? 1 : 0)
                        | (entity.getIcon2() ? 2 : 0)
                        | (entity.getIcon3() ? 4 : 0)
                        | (entity.getIcon4() ? 8 : 0)
                        | (entity.getIcon5() ? 16 : 0)
                        | (entity.getIcon6() ? 32 : 0)
                        | (entity.getIcon7() ? 64 : 0)
                        | (entity.getIcon8() ? 128 : 0);
        state.lightCoords =
                state.enabled
                        ? LightCoordsUtil.FULL_BRIGHT
                        : LightCoordsUtil.getLightCoords(entity.level(), entity.getPos().above());
    }

    @Override
    public void submit(
            ChamberSignRenderState state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
        poses.pushPose();
        poses.rotateDegrees(Axis.ZP, 180);
        poses.rotateDegrees(Axis.YP, state.yRot + 180);
        poses.translate(0, -1.505, -.005);
        collector.submitModel(
                model,
                state,
                poses,
                RenderTypes.entityCutout(state.enabled ? TEXTURE_ON : TEXTURE_OFF),
                state.lightCoords,
                OverlayTexture.NO_OVERLAY,
                state.outlineColor);
        poses.popPose();
        super.submit(state, poses, collector, camera);
    }
}
