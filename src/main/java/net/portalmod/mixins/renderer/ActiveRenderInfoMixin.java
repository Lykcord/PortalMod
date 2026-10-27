package net.portalmod.mixins.renderer;

import com.mojang.datafixers.util.Pair;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.portalmod.common.sorted.portal.PortalEntity;
import net.portalmod.core.event.ClientEvents;
import net.portalmod.core.math.Mat4;
import net.portalmod.core.math.Vec3;
import net.portalmod.core.util.ModUtil;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(Camera.class)
public abstract class ActiveRenderInfoMixin {
    @Inject(
            method = "update",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/Camera;getViewRotationMatrix(Lorg/joml/Matrix4f;)Lorg/joml/Matrix4f;"))
    private void pmPortalCamera(DeltaTracker delta, CallbackInfo callback) {
        ClientEvents.onCameraSetup((Camera) (Object) this, delta);
    }

    @Inject(method = "getMaxZoom", at = @At(value = "HEAD"), cancellable = true)
    private void pmTeleportCameraRay(float zoom, CallbackInfoReturnable<Float> info) {
        Camera camera = (Camera) (Object) this;
        Entity entity = camera.entity();
        if (entity == null) {
            return;
        }
        Vec3 from = new Vec3(camera.position());
        Vec3 to = from.clone().sub(new Vec3(camera.forwardVector()).mul(zoom));

        List<PortalEntity> portalChain = ModUtil.getPortalsAlongRay(entity.level(), from, to, portal -> true);
        if (portalChain.isEmpty()) {
            List<PortalEntity> portals =
                    PortalEntity.getOpenPortals(entity.level(), entity.getBoundingBox(), portal -> true);
            if (portals.isEmpty()) {
                return;
            }

            Vec3 rayStart = from.clone().sub(new Vec3(camera.forwardVector()).mul(0.2));
            ClipContext context =
                    new ClipContext(
                            rayStart.to3d(),
                            to.to3d(),
                            ClipContext.Block.VISUAL,
                            ClipContext.Fluid.NONE,
                            camera.entity());
            HitResult result = entity.level().clip(context);
            double maxZoom = Math.min(zoom, result.getLocation().distanceTo(from.to3d()) - 0.1);
            info.setReturnValue((float) Math.max(0, maxZoom));

            return;
        }

        Mat4 portalMatrix = ModUtil.getMatrixFromPortalChain(portalChain);
        Vec3 teleportedEyePos = from.clone().transform(portalMatrix);
        Pair<net.minecraft.world.phys.Vec3, net.minecraft.world.phys.Vec3> ray =
                ModUtil.teleportRay(portalChain, from.to3d(), to.to3d());

        ClipContext context =
                new ClipContext(
                        ray.getFirst(),
                        ray.getSecond(),
                        ClipContext.Block.VISUAL,
                        ClipContext.Fluid.NONE,
                        camera.entity());
        HitResult result = entity.level().clip(context);
        double maxZoom = Math.min(zoom, result.getLocation().distanceTo(teleportedEyePos.to3d()) - 0.1);
        info.setReturnValue((float) Math.max(0, maxZoom));
    }
}
