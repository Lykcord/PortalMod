package net.portalmod.common.sorted.portal;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.portalmod.PMState;
import net.portalmod.client.compat.PortalRendererCompatibility;
import net.portalmod.client.render.PortalCamera;
import net.portalmod.core.math.Mat4;
import net.portalmod.core.math.Vec3;
import net.portalmod.core.util.ModUtil;
import net.portalmod.mixins.accessors.ActiveRenderInfoAccessor;

import java.util.List;
import java.util.Optional;

public class PortalEntityClient {
    // todo teleport player in spectator

    public static PortalCamera teleportCamera(
            PortalCamera camera, float partialTicks, Mat4 bobMat, boolean useBobbing) {

        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        Level level = minecraft.level;

        if (player == null || level == null) {
            return null;
        }

        PortalCamera newCamera = new PortalCamera(camera, partialTicks);

        AABB playerAABB =
                player.getBoundingBox()
                        .move(new Vec3(player.position()).negate().to3d())
                        .move(player.xo, player.yo, player.zo)
                        .expandTowards(new Vec3(player.position()).sub(player.xo, player.yo, player.zo).to3d());

        if (PMState.cameraPosOverrideForRenderingSelf != null) {
            playerAABB = playerAABB.inflate(1);
        }

        List<PortalEntity> entities =
                PortalEntity.getOpenPortals(
                        level,
                        playerAABB.deflate(0.001),
                        portal -> portal.arePointsAlignedToPortal(new Vec3(newCamera.position())));

        Optional<PortalEntity> optionalPortal =
                entities.stream()
                        .reduce(
                                (o, n) ->
                                        new Vec3(player.getBoundingBox().getCenter()).sub(n.position()).magnitudeSqr()
                                                        < new Vec3(player.getBoundingBox().getCenter())
                                                                .sub(o.position())
                                                                .magnitudeSqr()
                                                ? n
                                                : o);

        if (!optionalPortal.isPresent()) {
            return newCamera;
        }

        PortalEntity portal = optionalPortal.get();

        if (!portal.isOpen() || !portal.getOtherPortal().isPresent()) {
            return newCamera;
        }

        float affinity = (float) new Vec3(player.getDeltaMovement()).normalize().dot(portal.getDirection().getStep());
        if (PMState.cameraPosOverrideForRenderingSelf != null && affinity < 0.5) {
            return newCamera;
        }

        PortalEntity targetPortal = portal.getOtherPortal().get();
        net.minecraft.world.phys.Vec3 portalPos = portal.position();
        net.minecraft.world.phys.Vec3 targetPortalPos = targetPortal.position();
        net.minecraft.world.phys.Vec3 cameraPos = camera.position();

        PoseStack view = new PoseStack();
        view.rotate(Axis.ZP.rotationDegrees(newCamera.getRoll()));
        view.rotate(Axis.XP.rotationDegrees(newCamera.xRot()));
        view.rotate(Axis.YP.rotationDegrees(newCamera.yRot() + 180.0F));
        view.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        Mat4 viewMat = new Mat4(view.last().pose());

        Vec3 portalPosBobbed = new Vec3(portalPos).transform(viewMat);
        if (useBobbing) {
            portalPosBobbed.transform(bobMat);
        }

        Vec3 normalBobbed = new Vec3(portal.getDirection().getOpposite()).add(portalPos).transform(viewMat);
        if (useBobbing) {
            normalBobbed.transform(bobMat);
        }
        normalBobbed.sub(portalPosBobbed);

        OrthonormalBasis portalBasis = portal.getSourceBasis();
        OrthonormalBasis targetPortalBasis = targetPortal.getDestinationBasis();
        Mat4 changeOfBasisMatrix = portalBasis.getChangeOfBasisMatrix(targetPortalBasis);

        if (portalPosBobbed.dot(normalBobbed) < 0) {
            Mat4 portalTransform = PortalEntity.getPortalToPortalMatrix(portal, targetPortal);
            Vec3 newCameraPos = new Vec3(cameraPos).transform(portalTransform);
            ((ActiveRenderInfoAccessor) newCamera).pmSetPosition(newCameraPos.to3d());

            if (useBobbing) {
                if (PMState.cameraPosOverrideForRenderingSelf != null) {
                    PMState.cameraPosOverrideForRenderingSelf =
                            PMState.cameraPosOverrideForRenderingSelf.clone().transform(portalTransform);
                }
            }

            float xRot = newCamera.xRot();
            float yRot = newCamera.yRot();
            float roll = newCamera.getRoll();

            OrthonormalBasis cameraBasis = EulerConverter.toVectors(xRot, yRot, roll).transform(changeOfBasisMatrix);
            EulerConverter.EulerAngles angles = EulerConverter.toEulerAngles(cameraBasis);
            newCamera.setPitch(angles.getPitch());
            newCamera.setYaw(angles.getYaw());
            newCamera.setRoll(angles.getRoll());
        }

        return newCamera;
    }

    private static PortalCamera teleportThirdPersonCamera(PortalCamera camera, float partialTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        Level level = minecraft.level;

        if (player == null || level == null) {
            return null;
        }

        PortalCamera newCamera = new PortalCamera(camera, partialTicks);

        Vec3 from = new Vec3(camera.entity().getEyePosition(partialTicks));
        Vec3 to = new Vec3(newCamera.position());
        List<PortalEntity> portalChain =
                ModUtil.getPortalsAlongRay(Minecraft.getInstance().level, from, to, portal -> true);

        Mat4 matrix = ModUtil.getMatrixFromPortalChain(portalChain);
        Mat4 rotationMatrix = ModUtil.getRotationMatrixFromPortalChain(portalChain);

        Vec3 eyePos = new Vec3(camera.entity().getEyePosition(partialTicks));
        Vec3 newEyePos = eyePos.clone().transform(matrix);

        Vec3 newCameraPos = new Vec3(camera.position()).sub(eyePos).transform(rotationMatrix).add(newEyePos);
        ((ActiveRenderInfoAccessor) newCamera).pmSetPosition(newCameraPos.to3d());

        float xRot = newCamera.xRot();
        float yRot = newCamera.yRot();
        float roll = newCamera.getRoll();

        OrthonormalBasis cameraBasis = EulerConverter.toVectors(xRot, yRot, roll).transform(rotationMatrix);
        EulerConverter.EulerAngles angles = EulerConverter.toEulerAngles(cameraBasis);
        ((ActiveRenderInfoAccessor) newCamera).pmSetRotation(angles.getYaw(), angles.getPitch());
        newCamera.setRoll(angles.getRoll());

        return newCamera;
    }

    public static void teleportCameraAndApply(Camera camera, PortalCamera animated, float partialTicks, Mat4 bob) {
        boolean thirdPerson = camera.isDetached();
        PortalCamera transformed =
                thirdPerson
                        ? teleportThirdPersonCamera(animated, partialTicks)
                        : teleportCamera(animated, partialTicks, bob, true);
        PortalCamera forLight = thirdPerson ? transformed : teleportCamera(animated, partialTicks, bob, false);
        if (transformed == null) {
            return;
        }
        boolean warped =
                transformed.position().distanceToSqr(animated.position()) > 1.0E-10
                        || Math.abs(transformed.rotation().dot(animated.rotation())) < .99999F;
        var access = (ActiveRenderInfoAccessor) camera;
        access.pmSetPosition(transformed.position());
        access.pmSetRotation(transformed.yRot(), transformed.xRot());
        PortalCamera.applyRoll(camera, transformed.getRoll());
        if (forLight != null) {
            access.pmGetBlockPosition().set(BlockPos.containing(forLight.position()));
        }
        PMState.cameraRoll = transformed.getRoll();
        PortalRendererCompatibility.onCameraUpdated(camera, warped);
    }

    public static boolean isLocalPlayerMoving() {
        Options options = Minecraft.getInstance().options;
        return options.keyLeft.isDown()
                || options.keyRight.isDown()
                || options.keyUp.isDown()
                || options.keyDown.isDown();
    }
}
