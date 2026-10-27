package net.portalmod.common.sorted.autoportal;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.UvMapping;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.portalmod.common.sorted.button.QuadBlockCorner;
import net.portalmod.common.sorted.portal.OrthonormalBasis;
import net.portalmod.core.math.Vec3;

public class AutoPortalTER implements BlockEntityRenderer<AutoPortalTileEntity, AutoPortalRenderState> {
    public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("portalmod", "entity/autoportal");
    private final AutoPortalModel model = new AutoPortalModel();
    private final SpriteGetter sprites;

    public AutoPortalTER(BlockEntityRendererProvider.Context context) {
        sprites = context.sprites();
    }

    @Override
    public AutoPortalRenderState createRenderState() {
        return new AutoPortalRenderState();
    }

    @Override
    public void extractRenderState(
            AutoPortalTileEntity entity,
            AutoPortalRenderState state,
            float partialTick,
            net.minecraft.world.phys.Vec3 camera,
            ModelFeatureRenderer.CrumblingOverlay breaking) {
        BlockEntityRenderer.super.extractRenderState(entity, state, partialTick, camera, breaking);
        state.basis = null;
        var blockState = entity.getBlockState();
        if (!(blockState.getBlock() instanceof AutoPortalBlock block)) {
            return;
        }
        var facing = blockState.getValue(AutoPortalBlock.FACING);
        var direction = blockState.getValue(AutoPortalBlock.DIRECTION);
        var pos = entity.getBlockPos();
        var up =
                new Vec3(
                        block.getOtherBlock(pos, QuadBlockCorner.DOWN_LEFT, QuadBlockCorner.UP_LEFT, facing, direction)
                                .subtract(pos));
        var right =
                new Vec3(
                        block.getOtherBlock(
                                        pos, QuadBlockCorner.DOWN_LEFT, QuadBlockCorner.DOWN_RIGHT, facing, direction)
                                .subtract(pos));
        state.basis = new OrthonormalBasis(right, up).getChangeOfBasisFromCanonicalMatrix().to4f();
        state.offset = right.mul(.5).add(up.mul(.5)).add(new Vec3(facing).mul(-.499)).add(.5).to3d();
        state.closed = entity.lastOpenedUUID == null;
        state.colorIndex = entity.getCurrentColorIndex().orElse(-1);
    }

    @Override
    public void submit(
            AutoPortalRenderState state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.basis == null) {
            return;
        }
        poses.pushPose();
        poses.translate(state.offset.x, state.offset.y, state.offset.z);
        poses.mulPose(state.basis);
        poses.rotateDegrees(Axis.ZP, 180);
        poses.translate(0, -1.5, 0);
        var sprite = sprites.get(new SpriteId(TextureAtlas.LOCATION_BLOCKS, TEXTURE));
        var type = RenderTypes.entityTranslucent(sprite.atlasLocation());
        collector.submitModelPart(model.frame, poses, type, state.lightCoords, OverlayTexture.NO_OVERLAY, sprite);
        if (state.colorIndex >= 0) {
            float u = (state.colorIndex % 4 * 11 + (state.closed ? 2 : 0)) / 64F;
            float v = (state.colorIndex / 4) * 16 / 64F;
            collector.submitModelPart(
                    model.wawas,
                    poses,
                    type,
                    LightCoordsUtil.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY,
                    new OffsetUv(sprite, u, v));
        }
        poses.popPose();
    }

    private record OffsetUv(TextureAtlasSprite sprite, float u, float v) implements UvMapping {
        @Override
        public float getU(float value) {
            return sprite.getU(value + u);
        }

        @Override
        public float getV(float value) {
            return sprite.getV(value + v);
        }
    }
}
