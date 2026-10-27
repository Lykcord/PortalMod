package net.portalmod.common.sorted.trigger;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.portalmod.common.items.WrenchItem;
import net.portalmod.common.sorted.faithplate.FaithPlateTargetGeometry;

public class TriggerTER implements BlockEntityRenderer<TriggerTileEntity, TriggerTER.State> {
    private final SpriteGetter sprites;

    public static class State extends BlockEntityRenderState {
        public AABB field;
        public TriggerType type;
    }

    public TriggerTER(BlockEntityRendererProvider.Context context) {
        sprites = context.sprites();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            TriggerTileEntity entity,
            State state,
            float partialTick,
            Vec3 camera,
            ModelFeatureRenderer.CrumblingOverlay breaking) {
        BlockEntityRenderer.super.extractRenderState(entity, state, partialTick, camera, breaking);
        var player = Minecraft.getInstance().player;
        state.field = null;
        if (player == null || !WrenchItem.holdingWrench(player)) {
            return;
        }
        state.field = TriggerSelectionClient.isSelecting(entity) ? TriggerSelectionClient.getBox() : entity.getField();
        state.type = entity.getTriggerType();
    }

    private static Identifier texture(TriggerType type, boolean inside) {
        return Identifier.fromNamespaceAndPath(
                "portalmod",
                "block/trigger_field_"
                        + (type == TriggerType.PLAYER ? "players" : "mobs")
                        + (inside ? "_inside" : "_outside"));
    }

    @Override
    public void submit(State state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.field == null) {
            return;
        }
        submitField(
                state.field,
                sprites.get(new SpriteId(TextureAtlas.LOCATION_BLOCKS, texture(state.type, false))),
                false,
                poses,
                collector);
        submitField(
                state.field,
                sprites.get(new SpriteId(TextureAtlas.LOCATION_BLOCKS, texture(state.type, true))),
                true,
                poses,
                collector);
    }

    private static void submitField(
            AABB box, TextureAtlasSprite sprite, boolean inside, PoseStack poses, SubmitNodeCollector collector) {
        collector.submitCustomGeometry(
                poses,
                RenderTypes.entityTranslucentCull(sprite.atlasLocation()),
                (pose, vertices) -> {
                    for (var outsideFace : Direction.values()) {
                        var face = inside ? outsideFace.getOpposite() : outsideFace;
                        var axis = face.getAxis();
                        double boundary =
                                outsideFace.getAxisDirection() == Direction.AxisDirection.POSITIVE
                                        ? box.max(axis)
                                        : box.min(axis);
                        double base = boundary - (face.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : 0);
                        var quad = FaithPlateTargetGeometry.vertices(face, 0);
                        switch (axis) {
                            case X -> {
                                for (double y = box.minY; y < box.maxY; y++) {
                                    for (double z = box.minZ; z < box.maxZ; z++) {
                                        emitPatch(vertices, pose, sprite, face, quad, base, y, z);
                                    }
                                }
                            }
                            case Y -> {
                                for (double x = box.minX; x < box.maxX; x++) {
                                    for (double z = box.minZ; z < box.maxZ; z++) {
                                        emitPatch(vertices, pose, sprite, face, quad, x, base, z);
                                    }
                                }
                            }
                            case Z -> {
                                for (double x = box.minX; x < box.maxX; x++) {
                                    for (double y = box.minY; y < box.maxY; y++) {
                                        emitPatch(vertices, pose, sprite, face, quad, x, y, base);
                                    }
                                }
                            }
                        }
                    }
                });
    }

    private static void emitPatch(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            TextureAtlasSprite sprite,
            Direction face,
            Vec3[] quad,
            double x,
            double y,
            double z) {
        for (int i = 0; i < 4; i++) {
            var point = quad[i];
            vertices.addVertex(
                            pose,
                            (float) (x + point.x + face.getStepX() * .001),
                            (float) (y + point.y + face.getStepY() * .001),
                            (float) (z + point.z + face.getStepZ() * .001))
                    .setColor(-1)
                    .setUv(sprite.getU(i < 2 ? 0 : 1), sprite.getV(i == 0 || i == 3 ? 0 : 1))
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(LightCoordsUtil.pack(15, 0))
                    .setNormal(pose, face.getStepX(), face.getStepY(), face.getStepZ());
        }
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }
}
