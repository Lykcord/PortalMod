package net.portalmod.mixins.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.util.Pair;

import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityFluidInteraction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.portalmod.common.entities.Fizzleable;
import net.portalmod.common.entities.TestElementEntity;
import net.portalmod.common.items.WrenchItem;
import net.portalmod.common.sorted.cube.Cube;
import net.portalmod.common.sorted.faithplate.Flingable;
import net.portalmod.common.sorted.goo.GooBlock;
import net.portalmod.common.sorted.portal.*;
import net.portalmod.common.sorted.portalgun.PortalGun;
import net.portalmod.core.init.FluidTagInit;
import net.portalmod.core.interfaces.IDiscontinuouslyLerpable;
import net.portalmod.core.interfaces.IDragCancelable;
import net.portalmod.core.interfaces.ITeleportLerpable;
import net.portalmod.core.math.Mat4;
import net.portalmod.core.math.Vec3;
import net.portalmod.core.util.ClientExecution;
import net.portalmod.core.util.ModUtil;
import net.portalmod.core.util.RayTraceContextWrapper;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Mixin(Entity.class)
public abstract class EntityMixin
        implements ITeleportable, ITeleportable2, IDiscontinuouslyLerpable, ITeleportLerpable {
    // Held test elements use the player as their vehicle; players cannot serialize as ordinary vehicles.
    @ModifyExpressionValue(
            method = "startRiding(Lnet/minecraft/world/entity/Entity;ZZ)Z",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityType;canSerialize()Z"))
    private boolean pmAllowHoldingVehicle(boolean serializable, @Local(argsOnly = true) Entity vehicle) {
        return serializable || (Object) this instanceof TestElementEntity && vehicle instanceof Player;
    }

    @Shadow
    protected abstract net.minecraft.world.phys.Vec3 maybeBackOffFromEdge(
            net.minecraft.world.phys.Vec3 movement, MoverType moverType);

    @Shadow public Level level;
    @Shadow protected boolean onGround;

    @Shadow
    public abstract EntityDimensions getDimensions(Pose pose);

    @Shadow @Final private EntityFluidInteraction fluidInteraction;

    @ModifyArg(
            method = "<init>",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/world/entity/EntityFluidInteraction;<init>(Ljava/util/Set;)V"),
            index = 0)
    private Set<TagKey<Fluid>> pmTrackGooCurrent(Set<TagKey<Fluid>> original) {
        var tags = new HashSet<>(original);
        tags.add(FluidTagInit.GOO);
        return Set.copyOf(tags);
    }

    private int lastUsedPortal = -1;

    @Override
    public void setLastUsedPortal(int lastUsedPortal) {
        this.lastUsedPortal = lastUsedPortal;
    }

    @Override
    public int getLastUsedPortal() {
        return this.lastUsedPortal;
    }

    @Override
    public boolean hasLastUsedPortal() {
        return this.lastUsedPortal != -1;
    }

    @Override
    public void removeLastUsedPortal() {
        this.lastUsedPortal = -1;
    }

    private int justUsedPortal = -1;

    @Unique private final Deque<DiscontinuousLerpPos> lerpPosQueue = new ArrayDeque<>();

    @Override
    public Deque<DiscontinuousLerpPos> getLerpPosQueue() {
        return this.lerpPosQueue;
    }

    @Unique
    private final Deque<Pair<net.minecraft.world.phys.Vec3, net.minecraft.world.phys.Vec3>> lerpPositions =
            new ArrayDeque<>();

    @Unique private boolean hasUsedPortal = false;

    @Override
    public Deque<Pair<net.minecraft.world.phys.Vec3, net.minecraft.world.phys.Vec3>> getLerpPositions() {
        return this.lerpPositions;
    }

    @Override
    public boolean hasUsedPortal() {
        return this.hasUsedPortal;
    }

    @Override
    public void setHasUsedPortal(boolean hasUsedPortal) {
        this.hasUsedPortal = hasUsedPortal;
    }

    @Override
    public void setJustUsedPortal(int justUsedPortal) {
        this.justUsedPortal = justUsedPortal;
    }

    @Override
    public int getJustUsedPortal() {
        return this.justUsedPortal;
    }

    @Override
    public boolean hasJustUsedPortal() {
        return this.justUsedPortal != -1;
    }

    @Override
    public void removeJustUsedPortal() {
        this.justUsedPortal = -1;
    }

    // TODO this causes the player to not be able to swim
    // this may cause the player not to be able to swim, however it is required for the portals to work
    @Redirect(
            method = "move",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/Entity;maybeBackOffFromEdge(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/entity/MoverType;)Lnet/minecraft/world/phys/Vec3;"))
    private net.minecraft.world.phys.Vec3 pmTeleport(
            Entity instance, net.minecraft.world.phys.Vec3 delta, MoverType moverType) {
        Entity entity = (Entity) (Object) this;
        return this.maybeBackOffFromEdge(
                PortalEntity.teleportEntity(entity, PortalEntity.doFunneling(entity, delta)), moverType);
    }

    @Inject(
            method = {"setOnGround(Z)V", "setOnGroundWithMovement(ZZLnet/minecraft/world/phys/Vec3;)V"},
            at = @At("RETURN"))
    private void pmResetDragOnLand(CallbackInfo info) {
        if (this.onGround && this instanceof IDragCancelable) {
            ((IDragCancelable) this).pmSetCancelDrag(false);
        }
    }

    @Unique net.minecraft.world.phys.Vec3 capturedDelta;

    @Inject(method = "collide", at = @At("HEAD"))
    private void pmCaptureCollideDelta(
            net.minecraft.world.phys.Vec3 delta, CallbackInfoReturnable<net.minecraft.world.phys.Vec3> info) {
        this.capturedDelta = delta;
    }

    @Redirect(
            method = "collide",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/Entity;getBoundingBox()Lnet/minecraft/world/phys/AABB;"))
    private AABB pmGetSquashedHitbox(Entity entity) {
        AABB hitbox = entity.getBoundingBox();
        if (!(entity instanceof LivingEntity)) {
            return hitbox;
        }

        AABB travelAABB = hitbox.expandTowards(capturedDelta);
        boolean tall = hitbox.getYsize() > hitbox.getXsize();
        boolean flinging = ((Flingable) entity).isFlinging();
        List<PortalEntity> portalsInside =
                PortalEntity.getPortals(
                        entity.level(),
                        travelAABB,
                        portal ->
                                portal.getDirection().getAxis().isHorizontal()
                                        && portal.isOpen()
                                        && portal.isEntityAlignedToPortal(entity));

        if (tall && flinging && !portalsInside.isEmpty()) {
            double shrink = hitbox.getYsize() / 2 - hitbox.getXsize() / 2;
            return hitbox.inflate(0, -shrink, 0);
        }

        return hitbox;
    }

    @Redirect(
            method = "collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/Level;getEntityCollisions(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"))
    private List<VoxelShape> pmAddAdditionalCollisions(Level level, Entity mover, AABB sweptBounds) {
        List<VoxelShape> collisions = new ArrayList<>(level.getEntityCollisions(mover, sweptBounds));
        AABB bounds = mover.getBoundingBox();
        for (Entity entity :
                level.getEntities(
                        mover,
                        sweptBounds.inflate(1.0E-7D),
                        entity ->
                                entity instanceof Cube
                                        && !entity.isPassenger()
                                        && bounds.minY > entity.getBoundingBox().maxY - 0.001)) {
            collisions.add(Shapes.create(entity.getBoundingBox()));
        }
        collisions.add(PortalEntity.getCollisionShape(mover));
        return collisions;
    }

    @Redirect(
            method = "pick",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/Level;clip(Lnet/minecraft/world/level/ClipContext;)Lnet/minecraft/world/phys/BlockHitResult;"))
    private BlockHitResult pmPickThroughPortal(Level level, ClipContext context) {
        List<PortalEntity> portalChain =
                ModUtil.getPortalsAlongRay(
                        level, new Vec3(context.getFrom()), new Vec3(context.getTo()), portal -> true);

        Entity entity = (Entity) (Object) this;
        boolean holdingSpecialItem =
                entity instanceof LivingEntity
                        && (WrenchItem.hitWithWrench((LivingEntity) entity)
                                || ((LivingEntity) entity).getMainHandItem().getItem() instanceof PortalGun);
        if (level.isClientSide()) {
            ClientExecution.run(() -> () -> PortalRenderer.getInstance().outlineRenderingPortalChain = null);
        }

        if (!portalChain.isEmpty() && !holdingSpecialItem) {
            BlockHitResult normalRay = level.clip(context);
            net.minecraft.world.phys.Vec3 positionBeforePortal = normalRay.getLocation();
            Optional<net.minecraft.world.phys.Vec3> optionalPositionOnPortal =
                    portalChain.get(0).getBoundingBox().clip(context.getFrom(), context.getTo());

            if (optionalPositionOnPortal.isPresent()) {
                double distanceBeforePortal = positionBeforePortal.subtract(context.getFrom()).length();
                double distanceOnPortal = optionalPositionOnPortal.get().subtract(context.getFrom()).length();

                if (distanceBeforePortal < distanceOnPortal) {
                    return normalRay;
                }
            }

            if (level.isClientSide()) {
                ClientExecution.run(
                        () ->
                                () ->
                                        PortalRenderer.getInstance().outlineRenderingPortalChain =
                                                List.copyOf(portalChain));
            }
            Mat4 portalMatrix = Mat4.identity();

            for (PortalEntity portal : portalChain) {
                if (!portal.getOtherPortal().isPresent()) {
                    break;
                }

                Mat4 matrix =
                        portal.getSourceBasis()
                                .getChangeOfBasisMatrix(portal.getOtherPortal().get().getDestinationBasis());

                portalMatrix =
                        Mat4.identity()
                                .translate(new Vec3(portal.getOtherPortal().get().position()))
                                .mul(matrix)
                                .translate(new Vec3(portal.position()).negate())
                                .mul(portalMatrix);
            }

            net.minecraft.world.phys.Vec3 to = new Vec3(context.getTo()).transform(portalMatrix).to3d();
            net.minecraft.world.phys.Vec3 from = new Vec3(context.getFrom()).transform(portalMatrix).to3d();

            PortalEntity last = portalChain.get(portalChain.size() - 1);

            Optional<net.minecraft.world.phys.Vec3> intersection =
                    last.getOtherPortal().get().getBoundingBox().clip(from, to);

            if (intersection.isPresent()) {
                from = intersection.get();
            }

            context = new RayTraceContextWrapper(context);
            ((RayTraceContextWrapper) context).setTo(to);
            ((RayTraceContextWrapper) context).setFrom(from);
        }

        return level.clip(context);
    }

    @Inject(method = "tick", at = @At("RETURN"))
    public void checkFizzlers(CallbackInfo ci) {
        if (this instanceof Fizzleable) {
            ((Fizzleable) this).checkForFizzlers((Entity) (Object) this);
        }
    }

    // Replaced

    @Inject(method = "updateFluidInteraction", at = @At("RETURN"))
    private void pmAddFlowingGooPhysics(CallbackInfoReturnable<Boolean> cir) {
        var entity = (Entity) (Object) this;
        if (entity.isPushedByFluid() && fluidInteraction.isInFluid(FluidTagInit.GOO)) {
            fluidInteraction.applyCurrentTo(FluidTagInit.GOO, entity, GooBlock.FLOW_PUSH_STRENGTH);
        }
    }
}
