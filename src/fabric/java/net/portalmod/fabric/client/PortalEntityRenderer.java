package net.portalmod.fabric.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.portalmod.fabric.PortalModFabric;
import net.portalmod.fabric.entity.PortalEntity;

public final class PortalEntityRenderer extends EntityRenderer<PortalEntity, PortalRenderState> {
    public PortalEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
        shadowStrength = 0.0F;
    }

    @Override
    public PortalRenderState createRenderState() {
        return new PortalRenderState();
    }

    @Override
    public void extractRenderState(PortalEntity entity, PortalRenderState state, float tickProgress) {
        super.extractRenderState(entity, state, tickProgress);
        state.primary = entity.primary();
        state.open = entity.isOpen();
        state.hue = entity.hue();
        state.face = entity.direction();
        state.up = entity.up();
    }

    @Override
    public void submit(PortalRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState) {
        Identifier texture = Identifier.fromNamespaceAndPath(
                PortalModFabric.MOD_ID,
                "textures/portal/" + (state.open ? "open_" : "closed_") + state.hue + ".png"
        );
        int color = state.primary ? 0xFFFFFFFF : 0xFFFFFFFF;
        submitNodeCollector.submitCustomGeometry(
                poseStack,
                RenderTypes.entityTranslucentEmissive(texture),
                (pose, consumer) -> submitPortalQuad(pose, consumer, state.face, state.up, color)
        );

        super.submit(state, poseStack, submitNodeCollector, cameraRenderState);
    }

    private static void submitPortalQuad(PoseStack.Pose pose, VertexConsumer consumer, Direction face, Direction upDirection, int color) {
        int light = 0xF000F0;
        int red = color >> 16 & 0xFF;
        int green = color >> 8 & 0xFF;
        int blue = color & 0xFF;
        int alpha = color >>> 24;
        net.minecraft.world.phys.Vec3 normal = face.getUnitVec3().scale(0.006D);
        net.minecraft.world.phys.Vec3 up = upDirection.getUnitVec3();
        net.minecraft.world.phys.Vec3 right = up.cross(face.getUnitVec3());
        net.minecraft.world.phys.Vec3 bottomLeft = right.scale(-0.5D).add(up.scale(-1.0D)).add(normal);
        net.minecraft.world.phys.Vec3 bottomRight = right.scale(0.5D).add(up.scale(-1.0D)).add(normal);
        net.minecraft.world.phys.Vec3 topRight = right.scale(0.5D).add(up.scale(1.0D)).add(normal);
        net.minecraft.world.phys.Vec3 topLeft = right.scale(-0.5D).add(up.scale(1.0D)).add(normal);

        vertex(pose, consumer, bottomLeft, 0.0F, 1.0F, red, green, blue, alpha, light, normal);
        vertex(pose, consumer, bottomRight, 1.0F, 1.0F, red, green, blue, alpha, light, normal);
        vertex(pose, consumer, topRight, 1.0F, 0.0F, red, green, blue, alpha, light, normal);
        vertex(pose, consumer, topLeft, 0.0F, 0.0F, red, green, blue, alpha, light, normal);
        vertex(pose, consumer, topLeft, 0.0F, 0.0F, red, green, blue, alpha, light, normal.scale(-1.0D));
        vertex(pose, consumer, topRight, 1.0F, 0.0F, red, green, blue, alpha, light, normal.scale(-1.0D));
        vertex(pose, consumer, bottomRight, 1.0F, 1.0F, red, green, blue, alpha, light, normal.scale(-1.0D));
        vertex(pose, consumer, bottomLeft, 0.0F, 1.0F, red, green, blue, alpha, light, normal.scale(-1.0D));
    }

    private static void vertex(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            net.minecraft.world.phys.Vec3 position,
            float u,
            float v,
            int red,
            int green,
            int blue,
            int alpha,
            int light,
            net.minecraft.world.phys.Vec3 normal
    ) {
        consumer.addVertex(pose, (float) position.x(), (float) position.y(), (float) position.z())
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setUv1(0, 10)
                .setLight(light)
                .setNormal(pose, (float) normal.x(), (float) normal.y(), (float) normal.z());
    }
}
