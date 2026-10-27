package net.portalmod.mixins.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PositionPath;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.NewMinecartBehavior;
import net.portalmod.common.sorted.portal.EulerConverter;
import net.portalmod.common.sorted.portal.PortalEntity;
import net.portalmod.common.sorted.portal.PortalHandler;
import net.portalmod.core.math.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.UnaryOperator;

@Mixin(AbstractMinecart.class)
public class AbstractMinecartMixin implements PortalHandler {
    @Inject(
            method = "tick",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/world/entity/vehicle/minecart/MinecartBehavior;tick()V"))
    private void pmTeleportMinecart(CallbackInfo callback) {
        var entity = (Entity) (Object) this;
        PortalEntity.teleportEntity(entity, entity.getDeltaMovement());
    }

    @Override
    public void onTeleport(PortalEntity from, PortalEntity to) {
        var cart = (AbstractMinecart) (Object) this;
        var matrix = PortalEntity.getPortalToPortalMatrix(from, to);
        var rotation = PortalEntity.getPortalToPortalRotationMatrix(from, to);
        var interpolation = cart.getInterpolation();
        var target = interpolation.target();
        if (target != null) {
            var position = new Vec3(target.position()).transform(matrix).to3d();
            var angles =
                    EulerConverter.toEulerAnglesLeastRoll(
                            EulerConverter.toVectors(target.xRot(), target.yRot(), 0).transform(rotation));
            interpolation.cancel();
            interpolation.interpolateTo(PositionPath.of(position), angles.getYaw(), angles.getPitch(), true);
        }
        if (cart.getBehavior() instanceof NewMinecartBehavior behavior) {
            if (cart.level().isClientSide()) {
                UnaryOperator<NewMinecartBehavior.MinecartStep> transform =
                        step -> {
                            var angles =
                                    EulerConverter.toEulerAnglesLeastRoll(
                                            EulerConverter.toVectors(step.xRot(), step.yRot(), 0).transform(rotation));
                            return new NewMinecartBehavior.MinecartStep(
                                    new Vec3(step.position()).transform(matrix).to3d(),
                                    new Vec3(step.movement()).transform(rotation).to3d(),
                                    angles.getYaw(),
                                    angles.getPitch(),
                                    step.weight());
                        };
                behavior.lerpSteps.replaceAll(transform);
                behavior.currentLerpSteps.replaceAll(transform);
                behavior.oldLerp = transform.apply(behavior.oldLerp);
            } else {
                behavior.lerpSteps.clear();
            }
        }
    }

    @Override
    public void onTeleportPacket() {}
}
