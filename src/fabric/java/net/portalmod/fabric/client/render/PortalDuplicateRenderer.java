package net.portalmod.fabric.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.portalmod.fabric.entity.PortalEntity;
import net.portalmod.fabric.registry.PortalModEntities;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders a plane-clipped copy of every entity straddling an open portal at the destination
 * portal (Forge {@code DuplicateEntityRenderer} parity), so bodies emerge from the far side
 * while passing through. Also covers the local player, who is normally excluded from entity
 * extraction in first person, making your own arms/body visible coming out of the far portal.
 */
public final class PortalDuplicateRenderer {
    private static final double PORTAL_SEARCH_DISTANCE = 96.0D;
    private static final double ENTITY_SEARCH_INFLATION = 0.5D;

    private record Duplicate(EntityRenderState state, PortalEntity portal, PortalEntity destination) {
    }

    private static final List<Duplicate> duplicates = new ArrayList<>();

    private PortalDuplicateRenderer() {
    }

    /** Called at the end of {@code LevelRenderer.extractVisibleEntities} for every render pass. */
    public static void extract(ClientLevel level, EntityRenderDispatcher dispatcher, Camera camera, DeltaTracker deltaTracker) {
        duplicates.clear();
        Vec3 cameraPosition = camera.position();
        AABB portalSearch = new AABB(cameraPosition, cameraPosition).inflate(PORTAL_SEARCH_DISTANCE);
        float partialTicks = deltaTracker.getGameTimeDeltaPartialTick(true);

        for (PortalEntity portal : level.getEntities(PortalModEntities.PORTAL, portalSearch, PortalEntity::isOpen)) {
            PortalEntity destination = findDestination(level, portal);
            if (destination == null) {
                continue;
            }
            for (Entity entity : level.getEntities((Entity) null, portal.getBoundingBox().inflate(ENTITY_SEARCH_INFLATION),
                    candidate -> !(candidate instanceof PortalEntity) && !candidate.isSpectator() && straddlesPlane(candidate, portal))) {
                duplicates.add(new Duplicate(dispatcher.extractEntity(entity, partialTicks), portal, destination));
            }
        }
    }

    /** Called at the end of {@code LevelRenderer.submitEntities} for every render pass. */
    public static void submit(EntityRenderDispatcher dispatcher, PoseStack poseStack, LevelRenderState levelRenderState, SubmitNodeCollector output) {
        if (duplicates.isEmpty()) {
            return;
        }
        Vec3 cameraPos = levelRenderState.cameraRenderState.pos;
        for (Duplicate duplicate : duplicates) {
            PortalEntity portal = duplicate.portal;
            PortalEntity destination = duplicate.destination;
            EntityRenderState state = duplicate.state;

            Vec3 duplicatePos = portal.teleportPoint(new Vec3(state.x, state.y, state.z), destination);
            // Keep only geometry in front of the destination plane: the part that crossed the
            // source plane maps exactly there under the portal isometry.
            Vec3 planeNormal = destination.direction().getUnitVec3();
            Vec3 planePoint = destination.position().subtract(cameraPos);
            ClippedSubmitNodeCollector clipped = new ClippedSubmitNodeCollector(output, planePoint, planeNormal);

            poseStack.pushPose();
            poseStack.translate(duplicatePos.x - cameraPos.x, duplicatePos.y - cameraPos.y, duplicatePos.z - cameraPos.z);
            poseStack.mulPose(portalRotation(portal, destination));
            dispatcher.submit(state, levelRenderState.cameraRenderState, 0.0D, 0.0D, 0.0D, poseStack, clipped);
            poseStack.popPose();
        }
    }

    private static PortalEntity findDestination(ClientLevel level, PortalEntity source) {
        AABB search = source.getBoundingBox().inflate(192.0D);
        return level.getEntities(PortalModEntities.PORTAL, search, portal ->
                        !portal.isRemoved()
                                && portal.isOpen()
                                && portal.gunId().equals(source.gunId())
                                && portal.primary() != source.primary())
                .stream()
                .findFirst()
                .orElse(null);
    }

    /** True when the entity's bounding box crosses the portal plane (portal faces are axis-aligned). */
    private static boolean straddlesPlane(Entity entity, PortalEntity portal) {
        Direction.Axis axis = portal.direction().getAxis();
        Vec3 portalPos = portal.position();
        double planeCoordinate = axis.choose(portalPos.x, portalPos.y, portalPos.z);
        AABB boundingBox = entity.getBoundingBox();
        double min = axis.choose(boundingBox.minX, boundingBox.minY, boundingBox.minZ);
        double max = axis.choose(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ);
        return min < planeCoordinate && max > planeCoordinate;
    }

    /** Rotation part of the portal-to-portal isometry as a column-basis matrix. */
    private static Matrix4f portalRotation(PortalEntity portal, PortalEntity destination) {
        Vec3 x = portal.teleportVector(new Vec3(1.0D, 0.0D, 0.0D), destination);
        Vec3 y = portal.teleportVector(new Vec3(0.0D, 1.0D, 0.0D), destination);
        Vec3 z = portal.teleportVector(new Vec3(0.0D, 0.0D, 1.0D), destination);
        return new Matrix4f(
                (float) x.x, (float) x.y, (float) x.z, 0.0F,
                (float) y.x, (float) y.y, (float) y.z, 0.0F,
                (float) z.x, (float) z.y, (float) z.z, 0.0F,
                0.0F, 0.0F, 0.0F, 1.0F
        );
    }
}
