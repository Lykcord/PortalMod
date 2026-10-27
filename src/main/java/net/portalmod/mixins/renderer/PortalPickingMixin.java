package net.portalmod.mixins.renderer;

import com.mojang.datafixers.util.Pair;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.portalmod.common.items.WrenchItem;
import net.portalmod.common.sorted.portal.PortalEntity;
import net.portalmod.core.math.AABBUtil;
import net.portalmod.core.math.Mat4;
import net.portalmod.core.math.Vec3;
import net.portalmod.core.util.ModUtil;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.function.Predicate;

@Mixin(LocalPlayer.class)
public class PortalPickingMixin {
    @Redirect(
            method = "pick",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/projectile/ProjectileUtil;getEntityHitResult(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;D)Lnet/minecraft/world/phys/EntityHitResult;"))
    private static EntityHitResult pmCustomEntityPick(
            Entity entity,
            net.minecraft.world.phys.Vec3 from,
            net.minecraft.world.phys.Vec3 to,
            AABB aabb,
            Predicate<Entity> predicate,
            double d) {
        EntityHitResult rayResult =
                ProjectileUtil.getEntityHitResult(
                        entity,
                        from,
                        to,
                        aabb,
                        target -> {
                            if (target instanceof PortalEntity && entity instanceof LivingEntity) {
                                if (WrenchItem.hitWithWrench((LivingEntity) entity)
                                        || !((PortalEntity) target).isOpen()) {
                                    return true;
                                }
                            }
                            return predicate.test(target);
                        },
                        d);

        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return rayResult;
        }

        List<PortalEntity> portalChain =
                ModUtil.getPortalsAlongRay(player.level(), new Vec3(from), new Vec3(to), portal -> true);
        Mat4 portalMatrix = ModUtil.getMatrixFromPortalChain(portalChain);
        Pair<net.minecraft.world.phys.Vec3, net.minecraft.world.phys.Vec3> ray =
                ModUtil.teleportRay(portalChain, from, to);
        aabb = AABBUtil.transform(aabb, portalMatrix);

        EntityHitResult teleportedRayResult =
                ProjectileUtil.getEntityHitResult(
                        entity,
                        ray.getFirst(),
                        ray.getSecond(),
                        aabb,
                        target -> {
                            if (target instanceof PortalEntity && entity instanceof LivingEntity) {
                                if (WrenchItem.hitWithWrench((LivingEntity) entity)
                                        || !((PortalEntity) target).isOpen()) {
                                    return true;
                                }
                            }
                            return predicate.test(target);
                        },
                        d);

        if (teleportedRayResult == null) {
            return rayResult;
        } else if (rayResult == null) {
            return teleportedRayResult;
        }

        net.minecraft.world.phys.Vec3 teleportedFrom = new Vec3(from).transform(portalMatrix).to3d();
        double realWorldDistance = rayResult.getLocation().subtract(from).lengthSqr();
        double teleportedDistance = teleportedRayResult.getLocation().subtract(teleportedFrom).lengthSqr();

        return realWorldDistance <= teleportedDistance ? rayResult : teleportedRayResult;
    }
}
