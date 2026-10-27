package net.portalmod.common.sorted.portal;

import com.mojang.blaze3d.vertex.PoseStack;

import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.fabricmc.fabric.api.client.rendering.v1.SubmitRenderPhase;
import net.fabricmc.fabric.api.client.rendering.v1.SubmitRenderPhases;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.phase.FeatureRenderPhase;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.portalmod.PMState;
import net.portalmod.client.render.DuplicateFeatureRenderer;
import net.portalmod.core.config.PortalModConfigManager;
import net.portalmod.core.math.AABBUtil;
import net.portalmod.core.math.Mat4;
import net.portalmod.core.math.Vec3;
import net.portalmod.core.util.ModUtil;
import net.portalmod.mixins.accessors.EntityRendererAccessor;

import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.*;

public final class DuplicateEntityRenderer {
    private static final RenderStateDataKey<List<Duplicate>> DUPLICATES =
            RenderStateDataKey.create(() -> "portalmod:duplicates");
    private static final SubmitRenderPhase<SubmitNode> OIT =
            new SubmitRenderPhase<>(collection -> collection.oitTranslucent);

    private record Duplicate(
            EntityRenderState entity,
            CameraRenderState sourceCamera,
            Matrix4f transform,
            Matrix4f shadowTransform,
            List<EntityRenderState.ShadowPiece> shadows,
            Vector4f clip) {}

    private DuplicateEntityRenderer() {}

    public static void extract(ClientLevel level, Camera camera, LevelRenderState scene, DeltaTracker delta) {
        var client = Minecraft.getInstance();
        var dispatcher = client.levelRenderer.entityRenderDispatcher();
        var duplicates = new ArrayList<Duplicate>();
        scene.setData(DUPLICATES, List.of());
        if (client.player == null) {
            return;
        }
        float partialTicks = scene.worldPartialTicks;
        try {
            for (var entity : level.entitiesForRendering()) {
                if (entity instanceof PortalEntity
                        || entity instanceof LocalPlayer
                                && entity != camera.entity()
                                && (entity != client.player || client.player.isSpectator())) {
                    continue;
                }
                if (!PortalModConfigManager.RENDER_SELF.get()
                        && entity == camera.entity()
                        && !camera.isDetached()
                        && !(entity instanceof LivingEntity living && living.isSleeping())) {
                    continue;
                }
                for (var portal :
                        PortalEntity.getOpenPortals(level, entity.getBoundingBox().inflate(.2), ignored -> true)) {
                    var exit = portal.getOtherPortal();
                    if (exit.isEmpty() || !portal.isEntityAlignedToPortal(entity)) {
                        continue;
                    }
                    var other = exit.get();
                    var transform = PortalEntity.getPortalToPortalMatrix(portal, other);
                    var renderer = (EntityRendererAccessor) dispatcher.getRenderer(entity);
                    var bounds = transformBounds(renderer.pmCullBox(entity, partialTicks), transform);
                    var position = new Vec3(entity.getPosition(partialTicks)).transform(transform);
                    double distance = position.clone().sub(camera.position()).magnitudeSqr();
                    if (!entity.hasIndirectPassenger(client.player)
                            && (!entity.shouldRenderAtSqrDistance(distance)
                                    || renderer.pmAffectedByCulling(entity)
                                            && !camera.getCullFrustum().isVisible(bounds)
                                    || !visibleSelf(entity, camera, transform, partialTicks))) {
                        continue;
                    }
                    if (entity.tickCount == 0) {
                        entity.xOld = entity.getX();
                        entity.yOld = entity.getY();
                        entity.zOld = entity.getZ();
                    }
                    var sourceCamera = PortalRenderer.setupCamera(camera, other, partialTicks);
                    dispatcher.prepare(sourceCamera, client.crosshairPickEntity);
                    var state = dispatcher.extractEntity(entity, partialTicks);
                    state.outlineColor = 0;
                    state.distanceToCameraSq = distance;
                    position = new Vec3(state.x, state.y, state.z).transform(transform);
                    var shadows = List.<EntityRenderState.ShadowPiece>of();
                    if (portal.getDirection().getAxis().isHorizontal()
                            && other.getDirection().getAxis().isHorizontal()) {
                        double x = state.x, y = state.y, z = state.z;
                        try {
                            state.x = position.x;
                            state.y = position.y;
                            state.z = position.z;
                            renderer.pmExtractShadow(state, client, level);
                            shadows = List.copyOf(state.shadowPieces);
                        } finally {
                            state.x = x;
                            state.y = y;
                            state.z = z;
                        }
                    }
                    state.shadowPieces.clear();
                    var relative = position.clone().sub(camera.position());
                    var model =
                            PortalEntity.getPortalToPortalRotationMatrix(portal, other)
                                    .to4f()
                                    .setTranslation((float) relative.x, (float) relative.y, (float) relative.z);
                    var shadowModel =
                            new Matrix4f().translation((float) relative.x, (float) relative.y, (float) relative.z);
                    var sourceState = new CameraRenderState();
                    sourceCamera.extractRenderState(sourceState, delta);
                    var normal = new Vec3(other.getDirection());
                    var surface =
                            new Vec3(.5, 1, 0)
                                    .transform(
                                            PortalEntity.setupMatrix(
                                                    other.getDirection(), other.getUpVector(), other.getPivotPoint()))
                                    .sub(camera.position());
                    var plane =
                            new Vector4f(
                                    (float) normal.x, (float) normal.y, (float) normal.z, (float) -normal.dot(surface));
                    duplicates.add(new Duplicate(state, sourceState, model, shadowModel, shadows, plane));
                }
            }
            scene.setData(DUPLICATES, List.copyOf(duplicates));
        } finally {
            dispatcher.prepare(client.gameRenderer.mainCamera(), client.crosshairPickEntity);
        }
    }

    private static boolean visibleSelf(Entity entity, Camera camera, Mat4 transform, float partialTicks) {
        var client = Minecraft.getInstance();
        if (entity != client.getCameraEntity()) {
            return true;
        }
        var cameraPos =
                PMState.cameraPosOverrideForRenderingSelf == null
                        ? new Vec3(camera.position())
                        : PMState.cameraPosOverrideForRenderingSelf
                                .clone()
                                .transform(
                                        ModUtil.getMatrixFromPortalChain(
                                                new ArrayList<>(PortalRenderer.getInstance().portalChain)));
        var offset = new Vec3(entity.getPosition(partialTicks)).sub(entity.position());
        var body = transformBounds(entity.getBoundingBox().move(offset.to3d()), transform);
        float height = entity.getEyeHeight();
        if (body.inflate(Camera.PROJECTION_Z_NEAR * 1.5).contains(cameraPos.to3d())) {
            return false;
        }
        if (body.contains(cameraPos.to3d()) ^ body.contains(cameraPos.clone().sub(0, height, 0).to3d())) {
            return false;
        }
        var eye = new Vec3(entity.getPosition(partialTicks)).add(0, height, 0).transform(transform);
        return eye.sub(cameraPos).magnitudeSqr() > .001;
    }

    static AABB transformBounds(AABB bounds, Mat4 matrix) {
        return AABBUtil.transform(bounds, matrix);
    }

    public static void submit(LevelRenderContext context) {
        var dispatcher = Minecraft.getInstance().levelRenderer.entityRenderDispatcher();
        for (var duplicate : context.levelState().getDataOrDefault(DUPLICATES, List.of())) {
            var storage = new SubmitNodeStorage();
            storage.setUseImprovedTransparency(context.gameRenderer().useImprovedTransparency());
            var poses = new PoseStack();
            poses.mulPose(duplicate.transform());
            dispatcher.submit(duplicate.entity(), duplicate.sourceCamera(), 0, 0, 0, poses, storage);
            if (!duplicate.shadows().isEmpty()) {
                var shadowPoses = new PoseStack();
                shadowPoses.mulPose(duplicate.shadowTransform());
                storage.submitShadow(shadowPoses, duplicate.entity().shadowRadius, duplicate.shadows());
            }
            for (var entry : storage.getSubmitsPerOrder().int2ObjectEntrySet()) {
                var source = entry.getValue();
                var target = context.submitNodeCollector().order(entry.getIntKey());
                forward(source.solid, SubmitRenderPhases.SOLID, target, duplicate);
                forward(source.shadows, SubmitRenderPhases.SHADOWS, target, duplicate);
                forward(source.nameTags, SubmitRenderPhases.NAME_TAGS, target, duplicate);
                forward(source.texts, SubmitRenderPhases.TEXTS, target, duplicate);
                forward(source.shapeOutlines, SubmitRenderPhases.SHAPE_OUTLINES, target, duplicate);
                forward(source.waterMask, SubmitRenderPhases.WATER_MASK, target, duplicate);
                forward(
                        source.translucentBlocksAndItems,
                        SubmitRenderPhases.TRANSLUCENT_BLOCKS_AND_ITEMS,
                        target,
                        duplicate);
                forward(source.translucentModels, SubmitRenderPhases.TRANSLUCENT_MODELS, target, duplicate);
                forward(
                        source.translucentCustomGeometry,
                        SubmitRenderPhases.TRANSLUCENT_CUSTOM_GEOMETRY,
                        target,
                        duplicate);
                forward(source.oitTranslucent, OIT, target, duplicate);
                forward(source.breakingOverlay, SubmitRenderPhases.BREAKING_OVERLAY, target, duplicate);
                forward(source.afterTerrain, SubmitRenderPhases.AFTER_TERRAIN, target, duplicate);
                forward(source.alwaysOnTopGizmos, SubmitRenderPhases.ALWAYS_ON_TOP, target, duplicate);
                forward(source.translucentGizmos, SubmitRenderPhases.GIZMOS, target, duplicate);
            }
            forward(
                    storage.seeThrough(),
                    SubmitRenderPhases.SEE_THROUGH_NAME_TAGS,
                    context.submitNodeCollector().order(0),
                    duplicate);
        }
    }

    private static void forward(
            FeatureRenderPhase<?> phase,
            SubmitRenderPhase<? super DuplicateFeatureRenderer.Submit> targetPhase,
            OrderedSubmitNodeCollector collector,
            Duplicate duplicate) {
        var nodes = new ArrayList<SubmitNode>();
        phase.sortInto((node, outline) -> nodes.add(node));
        if (!nodes.isEmpty()) {
            collector.submitCustom(
                    targetPhase,
                    new DuplicateFeatureRenderer.Submit(
                            nodes, duplicate.clip(), (float) duplicate.entity().distanceToCameraSq));
        }
    }
}
