package net.portalmod.fabric.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Forwards entity submissions while routing all quad geometry through a
 * {@link ClippingVertexConsumer}, so a duplicate entity rendered at the far portal is clipped
 * at the destination portal plane. Decorations that make no sense on a clipped duplicate
 * (shadow, name tag, flame, leash) are dropped; item quads pass through unclipped for now.
 */
public final class ClippedSubmitNodeCollector implements SubmitNodeCollector {
    private final SubmitNodeCollector root;
    private final OrderedSubmitNodeCollector target;
    /** Plane point and kept-side normal in the same space as post-pose vertices (camera-relative world). */
    private final Vec3 planePoint;
    private final Vec3 planeNormal;

    public ClippedSubmitNodeCollector(SubmitNodeCollector root, Vec3 planePoint, Vec3 planeNormal) {
        this(root, root, planePoint, planeNormal);
    }

    private ClippedSubmitNodeCollector(SubmitNodeCollector root, OrderedSubmitNodeCollector target, Vec3 planePoint, Vec3 planeNormal) {
        this.root = root;
        this.target = target;
        this.planePoint = planePoint;
        this.planeNormal = planeNormal;
    }

    @Override
    public OrderedSubmitNodeCollector order(int order) {
        return new ClippedSubmitNodeCollector(root, root.order(order), planePoint, planeNormal);
    }

    private ClippingVertexConsumer clip(VertexConsumer buffer) {
        return new ClippingVertexConsumer(buffer, planePoint.x, planePoint.y, planePoint.z, planeNormal.x, planeNormal.y, planeNormal.z);
    }

    @Override
    public <S> void submitModel(
            Model<? super S> model,
            S state,
            PoseStack poseStack,
            RenderType renderType,
            int lightCoords,
            int overlayCoords,
            int tintedColor,
            @Nullable TextureAtlasSprite sprite,
            int outlineColor,
            ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay
    ) {
        target.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            ClippingVertexConsumer clipper = clip(buffer);
            VertexConsumer wrapped = sprite == null ? clipper : sprite.wrap(clipper);
            PoseStack stack = new PoseStack();
            stack.last().set(pose);
            model.setupAnim(state);
            model.renderToBuffer(stack, wrapped, lightCoords, overlayCoords, tintedColor);
            clipper.flush();
        });
    }

    @Override
    public void submitModelPart(
            ModelPart modelPart,
            PoseStack poseStack,
            RenderType renderType,
            int lightCoords,
            int overlayCoords,
            @Nullable TextureAtlasSprite sprite,
            boolean sheeted,
            boolean hasFoil,
            int tintedColor,
            ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay,
            int outlineColor
    ) {
        target.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            ClippingVertexConsumer clipper = clip(buffer);
            VertexConsumer wrapped = sprite == null ? clipper : sprite.wrap(clipper);
            PoseStack stack = new PoseStack();
            stack.last().set(pose);
            modelPart.render(stack, wrapped, lightCoords, overlayCoords, tintedColor);
            clipper.flush();
        });
    }

    @Override
    public void submitCustomGeometry(PoseStack poseStack, RenderType renderType, SubmitNodeCollector.CustomGeometryRenderer customGeometryRenderer) {
        target.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            ClippingVertexConsumer clipper = clip(buffer);
            customGeometryRenderer.render(pose, clipper);
            clipper.flush();
        });
    }

    @Override
    public void submitItem(
            PoseStack poseStack,
            ItemDisplayContext displayContext,
            int lightCoords,
            int overlayCoords,
            int outlineColor,
            int[] tintLayers,
            List<BakedQuad> quads,
            ItemStackRenderState.FoilType foilType
    ) {
        // Held/worn item quads are small; pass through unclipped rather than reimplementing
        // the item feature renderer.
        target.submitItem(poseStack, displayContext, lightCoords, overlayCoords, outlineColor, tintLayers, quads, foilType);
    }

    @Override
    public void submitShadow(PoseStack poseStack, float radius, List<EntityRenderState.ShadowPiece> pieces) {
    }

    @Override
    public void submitNameTag(
            PoseStack poseStack,
            @Nullable Vec3 nameTagAttachment,
            int offset,
            Component name,
            boolean seeThrough,
            int lightCoords,
            double distanceToCameraSq,
            CameraRenderState camera
    ) {
    }

    @Override
    public void submitText(
            PoseStack poseStack,
            float x,
            float y,
            FormattedCharSequence string,
            boolean dropShadow,
            Font.DisplayMode displayMode,
            int lightCoords,
            int color,
            int backgroundColor,
            int outlineColor
    ) {
    }

    @Override
    public void submitFlame(PoseStack poseStack, EntityRenderState renderState, Quaternionf rotation) {
    }

    @Override
    public void submitLeash(PoseStack poseStack, EntityRenderState.LeashState leashState) {
    }

    @Override
    public void submitMovingBlock(PoseStack poseStack, MovingBlockRenderState movingBlockRenderState) {
        target.submitMovingBlock(poseStack, movingBlockRenderState);
    }

    @Override
    public void submitBlockModel(
            PoseStack poseStack, RenderType renderType, List<BlockStateModelPart> parts, int[] tintLayers, int lightCoords, int overlayCoords, int outlineColor
    ) {
        target.submitBlockModel(poseStack, renderType, parts, tintLayers, lightCoords, overlayCoords, outlineColor);
    }

    @Override
    public void submitBreakingBlockModel(PoseStack poseStack, BlockStateModel model, long seed, int progress) {
        target.submitBreakingBlockModel(poseStack, model, seed, progress);
    }

    @Override
    public void submitParticleGroup(SubmitNodeCollector.ParticleGroupRenderer particleGroupRenderer) {
        target.submitParticleGroup(particleGroupRenderer);
    }
}
