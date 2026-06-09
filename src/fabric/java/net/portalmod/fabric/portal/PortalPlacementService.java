package net.portalmod.fabric.portal;

import net.minecraft.sounds.SoundSource;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.portalmod.fabric.component.PortalGunState;
import net.portalmod.fabric.component.PortalTarget;
import net.portalmod.fabric.entity.PortalEntity;
import net.portalmod.fabric.registry.PortalModDataComponents;
import net.portalmod.fabric.registry.PortalModEntities;
import net.portalmod.fabric.registry.PortalModSounds;
import net.portalmod.fabric.registry.PortalModTags;

import java.util.UUID;

public final class PortalPlacementService {
    private static final double MAX_REACH = 96.0D;

    private PortalPlacementService() {
    }

    public static boolean tryPlaceFromGun(Level level, Player player, ItemStack stack, boolean primary) {
        if (level.isClientSide() || player.isSpectator()) {
            return false;
        }

        PortalGunState state = ensureGunUuid(stack);
        boolean effectivePrimary = primary || state.singlePortal();
        BlockHitResult hit = raycast(level, player);

        if (hit.getType() == HitResult.Type.MISS || !isPortalable(level.getBlockState(hit.getBlockPos()))) {
            playMiss(level, hit.getLocation());
            stack.update(PortalModDataComponents.PORTAL_GUN_STATE, PortalGunState.DEFAULT, current ->
                    current.withLastShot(effectivePrimary ? PortalGunState.PRIMARY : PortalGunState.SECONDARY));
            return false;
        }

        Direction up = chooseUpVector(player, hit.getDirection());
        PortalTarget target = anchoredTarget(level.dimension().identifier().toString(), hit, up);
        if (!hasValidBacking(level, target, up)) {
            playMiss(level, hit.getLocation());
            stack.update(PortalModDataComponents.PORTAL_GUN_STATE, PortalGunState.DEFAULT, current ->
                    current.withLastShot(effectivePrimary ? PortalGunState.PRIMARY : PortalGunState.SECONDARY));
            return false;
        }

        stack.update(PortalModDataComponents.PORTAL_GUN_STATE, PortalGunState.DEFAULT, current ->
                current.withPortalTarget(effectivePrimary, target));

        if (level instanceof ServerLevel serverLevel) {
            UUID gunId = stack.getOrDefault(PortalModDataComponents.PORTAL_GUN_STATE, PortalGunState.DEFAULT)
                    .gunUuid()
                    .orElse(player.getUUID());
            spawnPortalAnchor(serverLevel, gunId, effectivePrimary, target, up);
        }
        return true;
    }

    private static void spawnPortalAnchor(ServerLevel level, UUID gunId, boolean primary, PortalTarget target, Direction up) {
        level.getServer().getAllLevels().forEach(serverLevel ->
                serverLevel.getEntities(PortalModEntities.PORTAL, portal ->
                        portal.gunId().equals(gunId) && portal.primary() == primary).forEach(portal -> portal.discard()));

        PortalEntity portal = new PortalEntity(PortalModEntities.PORTAL, level);
        portal.configure(gunId, primary, target, up, primary ? "blue" : "orange");
        level.addFreshEntity(portal);
        level.playSound(null, target.hitX(), target.hitY(), target.hitZ(), PortalModSounds.PORTAL_OPEN, SoundSource.NEUTRAL, 0.8F, 1.0F);
    }

    private static BlockHitResult raycast(Level level, Player player) {
        Vec3 from = player.getEyePosition();
        Vec3 to = from.add(player.getLookAngle().normalize().scale(MAX_REACH));
        return level.clip(new ClipContext(from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
    }

    private static PortalGunState ensureGunUuid(ItemStack stack) {
        PortalGunState state = stack.getOrDefault(PortalModDataComponents.PORTAL_GUN_STATE, PortalGunState.DEFAULT);
        if (state.gunUuid().isPresent()) {
            return state;
        }

        PortalGunState updated = state.withGunUuid(UUID.randomUUID());
        stack.set(PortalModDataComponents.PORTAL_GUN_STATE, updated);
        return updated;
    }

    private static boolean isPortalable(BlockState state) {
        return !state.isAir() && !state.is(PortalModTags.UNPORTALABLE) && (state.is(PortalModTags.PORTALABLE) || state.canOcclude());
    }

    private static PortalTarget anchoredTarget(String dimension, BlockHitResult hit, Direction up) {
        BlockPos pos = hit.getBlockPos();
        Direction face = hit.getDirection();
        Direction right = rightVector(face, up);
        Vec3 hitPos = hit.getLocation();

        double x = snap(hitPos.x());
        double y = snap(hitPos.y());
        double z = snap(hitPos.z());

        if (face.getAxis() == Direction.Axis.X) {
            x = face == Direction.EAST ? pos.getX() + 1.0D : pos.getX();
        } else if (face.getAxis() == Direction.Axis.Y) {
            y = face == Direction.UP ? pos.getY() + 1.0D : pos.getY();
        } else {
            z = face == Direction.SOUTH ? pos.getZ() + 1.0D : pos.getZ();
        }

        Vec3 anchored = new Vec3(x, y, z);
        anchored = clampLocal(anchored, Vec3.atCenterOf(pos), right, 0.5D);
        anchored = clampLocal(anchored, Vec3.atCenterOf(pos), up, 1.0D);
        return new PortalTarget(dimension, pos.getX(), pos.getY(), pos.getZ(), face.getSerializedName(), anchored.x(), anchored.y(), anchored.z());
    }

    private static Vec3 clampLocal(Vec3 value, Vec3 origin, Direction axis, double halfSize) {
        Vec3 normal = axis.getUnitVec3();
        double local = value.subtract(origin).dot(normal);
        double clamped = Math.max(-halfSize, Math.min(halfSize, local));
        return value.add(normal.scale(clamped - local));
    }

    private static boolean hasValidBacking(Level level, PortalTarget target, Direction up) {
        Direction face = Direction.byName(target.face());
        if (face == null) {
            return false;
        }

        Direction right = rightVector(face, up);
        Vec3 center = new Vec3(target.hitX(), target.hitY(), target.hitZ()).add(face.getUnitVec3().scale(-0.01D));
        BlockPos[] support = new BlockPos[] {
                BlockPos.containing(center.add(right.getUnitVec3().scale(-0.49D)).add(up.getUnitVec3().scale(-0.99D))),
                BlockPos.containing(center.add(right.getUnitVec3().scale(0.49D)).add(up.getUnitVec3().scale(-0.99D))),
                BlockPos.containing(center.add(right.getUnitVec3().scale(-0.49D)).add(up.getUnitVec3().scale(0.99D))),
                BlockPos.containing(center.add(right.getUnitVec3().scale(0.49D)).add(up.getUnitVec3().scale(0.99D)))
        };

        for (BlockPos pos : support) {
            if (!isPortalable(level.getBlockState(pos))) {
                return false;
            }
        }

        return true;
    }

    private static Direction rightVector(Direction face, Direction up) {
        Vec3 right = up.getUnitVec3().cross(face.getUnitVec3());
        return Direction.getApproximateNearest(right.x(), right.y(), right.z());
    }

    private static double snap(double value) {
        return Math.round(value * 16.0D) / 16.0D;
    }

    private static Direction chooseUpVector(Player player, Direction face) {
        if (face.getAxis().isHorizontal()) {
            return Direction.UP;
        }

        Direction horizontal = player.getDirection();
        return face == Direction.UP ? horizontal : horizontal.getOpposite();
    }

    private static void playMiss(Level level, Vec3 location) {
        level.playSound(null, location.x(), location.y(), location.z(), PortalModSounds.PORTALGUN_MISS, SoundSource.PLAYERS, 0.8F, 1.0F);
    }
}
