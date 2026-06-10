package net.portalmod.fabric.portal;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.portalmod.fabric.component.PortalGunState;
import net.portalmod.fabric.component.PortalTarget;
import net.portalmod.fabric.entity.PortalEntity;
import net.portalmod.fabric.network.PortalGunEventPayload;
import net.portalmod.fabric.registry.PortalModDataComponents;
import net.portalmod.fabric.registry.PortalModEntities;
import net.portalmod.fabric.registry.PortalModGameRules;
import net.portalmod.fabric.registry.PortalModSounds;
import net.portalmod.fabric.registry.PortalModTags;

import java.util.List;
import java.util.Optional;
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

        // portalSlowShot: the shot travels at ~2 blocks per tick instead of landing instantly.
        if (level instanceof ServerLevel serverLevel
                && Boolean.TRUE.equals(serverLevel.getGameRules().get(PortalModGameRules.PORTAL_SLOW_SHOT))) {
            double distance = hit.getLocation().subtract(player.getEyePosition()).length();
            int ticks = (int) Math.ceil(distance / 2.0D);
            PortalShotScheduler.schedule(state.gunUuid().orElseThrow(), ticks,
                    () -> executeShot(level, player, stack, state, effectivePrimary, hit));
            return true;
        }

        return executeShot(level, player, stack, state, effectivePrimary, hit);
    }

    private static boolean executeShot(Level level, Player player, ItemStack stack, PortalGunState state,
                                       boolean effectivePrimary, BlockHitResult hit) {
        if (hit.getType() == HitResult.Type.MISS || !isPortalable(level, level.getBlockState(hit.getBlockPos()))) {
            playMiss(level, player, hit.getLocation());
            stack.update(PortalModDataComponents.PORTAL_GUN_STATE, PortalGunState.DEFAULT, current ->
                    current.withLastShot(effectivePrimary ? PortalGunState.PRIMARY : PortalGunState.SECONDARY));
            return false;
        }

        Direction up = chooseUpVector(player, hit.getDirection());
        Optional<PortalTarget> targetOptional = findPlacementTarget(level, level.dimension().identifier().toString(), hit, up);
        UUID gunId = state.gunUuid().orElseThrow();
        if (targetOptional.isEmpty()) {
            playMiss(level, player, hit.getLocation());
            stack.update(PortalModDataComponents.PORTAL_GUN_STATE, PortalGunState.DEFAULT, current ->
                    current.withLastShot(effectivePrimary ? PortalGunState.PRIMARY : PortalGunState.SECONDARY));
            return false;
        }

        PortalTarget target = targetOptional.get();
        if (level instanceof ServerLevel serverLevel && !resolvePortalOverlap(serverLevel, gunId, effectivePrimary, target, up)) {
            playMiss(level, player, hit.getLocation());
            stack.update(PortalModDataComponents.PORTAL_GUN_STATE, PortalGunState.DEFAULT, current ->
                    current.withLastShot(effectivePrimary ? PortalGunState.PRIMARY : PortalGunState.SECONDARY));
            return false;
        }

        stack.update(PortalModDataComponents.PORTAL_GUN_STATE, PortalGunState.DEFAULT, current ->
                current.withPortalTarget(effectivePrimary, target));

        if (level instanceof ServerLevel serverLevel) {
            spawnPortalAnchor(serverLevel, gunId, effectivePrimary, target, up, hueFor(state, effectivePrimary));
            if (player instanceof ServerPlayer serverPlayer) {
                ServerPlayNetworking.send(serverPlayer, new PortalGunEventPayload(PortalGunEventPayload.Event.SHOOT, hit.getLocation()));
            }
        }
        return true;
    }

    private static String hueFor(PortalGunState state, boolean primary) {
        return primary ? state.primaryHue() : state.secondaryHue();
    }

    /**
     * Returns true if placement may proceed. With allowPortalOverwrite enabled (default),
     * overlapping foreign portals are evicted; otherwise an overlap rejects the shot.
     */
    private static boolean resolvePortalOverlap(ServerLevel level, UUID gunId, boolean primary, PortalTarget target, Direction up) {
        AABB box = portalBox(target, up).inflate(0.02D);
        List<? extends PortalEntity> overlapping = level.getEntities(PortalModEntities.PORTAL, box, portal ->
                !portal.isRemoved()
                        && !(portal.gunId().equals(gunId) && portal.primary() == primary)
                        && portal.getBoundingBox().inflate(0.02D).intersects(box));

        if (overlapping.isEmpty()) {
            return true;
        }

        if (!Boolean.TRUE.equals(level.getGameRules().get(PortalModGameRules.ALLOW_PORTAL_OVERWRITE))) {
            return false;
        }

        overlapping.forEach(Entity::discard);
        return true;
    }

    private static void spawnPortalAnchor(ServerLevel level, UUID gunId, boolean primary, PortalTarget target, Direction up, String hue) {
        level.getServer().getAllLevels().forEach(serverLevel ->
                serverLevel.getEntities(PortalModEntities.PORTAL, portal ->
                        portal.gunId().equals(gunId) && portal.primary() == primary).forEach(Entity::discard));

        PortalEntity portal = new PortalEntity(PortalModEntities.PORTAL, level);
        portal.configure(gunId, primary, target, up, hue);
        level.addFreshEntity(portal);
        PortalManager.get(level.getServer()).put(level.getServer(), gunId, primary, PortalRecord.of(portal));
        level.playSound(null, target.hitX(), target.hitY(), target.hitZ(), PortalModSounds.PORTAL_OPEN, SoundSource.NEUTRAL, 0.8F, 1.0F);
    }

    /**
     * Direct placement without surface validation, used by the /portal open command.
     */
    public static PortalEntity forcePlace(ServerLevel level, UUID gunId, boolean primary, String hue, Vec3 position, Direction face, Direction up) {
        if (face.getAxis() == up.getAxis()) {
            return null;
        }

        level.getServer().getAllLevels().forEach(serverLevel ->
                serverLevel.getEntities(PortalModEntities.PORTAL, portal ->
                        portal.gunId().equals(gunId) && portal.primary() == primary).forEach(Entity::discard));

        BlockPos anchor = BlockPos.containing(position.add(face.getUnitVec3().scale(-0.5D)));
        PortalTarget target = new PortalTarget(
                level.dimension().identifier().toString(),
                anchor.getX(),
                anchor.getY(),
                anchor.getZ(),
                face.getSerializedName(),
                position.x(),
                position.y(),
                position.z()
        );

        PortalEntity portal = new PortalEntity(PortalModEntities.PORTAL, level);
        portal.configure(gunId, primary, target, up, hue);
        level.addFreshEntity(portal);
        PortalManager.get(level.getServer()).put(level.getServer(), gunId, primary, PortalRecord.of(portal));
        level.playSound(null, position.x(), position.y(), position.z(), PortalModSounds.PORTAL_OPEN, SoundSource.NEUTRAL, 0.8F, 1.0F);
        return portal;
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

    public static boolean isPortalable(Level level, BlockState state) {
        if (state.isAir() || state.is(PortalModTags.UNPORTALABLE)) {
            return false;
        }

        // Game rules only exist server-side; placement and survival checks run there.
        boolean blacklistMode = level instanceof ServerLevel serverLevel
                && serverLevel.getGameRules().get(PortalModGameRules.USE_PORTALABLE_BLACKLIST);
        if (blacklistMode) {
            return state.is(PortalModTags.PORTALABLE) || state.canOcclude();
        }

        return state.is(PortalModTags.PORTALABLE);
    }

    private static Optional<PortalTarget> findPlacementTarget(Level level, String dimension, BlockHitResult hit, Direction up) {
        PortalTarget anchored = anchoredTarget(dimension, hit, up);
        Direction face = hit.getDirection();
        Direction right = rightVector(face, up);
        Vec3 hitCenter = new Vec3(anchored.hitX(), anchored.hitY(), anchored.hitZ());
        PortalTarget best = null;
        double bestDistance = Double.MAX_VALUE;

        for (int upStep = -16; upStep <= 16; upStep++) {
            for (int rightStep = -8; rightStep <= 8; rightStep++) {
                Vec3 candidatePos = hitCenter
                        .add(up.getUnitVec3().scale(upStep / 16.0D))
                        .add(right.getUnitVec3().scale(rightStep / 16.0D));
                PortalTarget candidate = new PortalTarget(
                        dimension,
                        hit.getBlockPos().getX(),
                        hit.getBlockPos().getY(),
                        hit.getBlockPos().getZ(),
                        face.getSerializedName(),
                        candidatePos.x(),
                        candidatePos.y(),
                        candidatePos.z()
                );

                if (!hasValidBacking(level, candidate, up) || !hasFrontClearance(level, candidate, up)) {
                    continue;
                }

                double distance = candidatePos.distanceToSqr(hit.getLocation());
                if (distance < bestDistance) {
                    best = candidate;
                    bestDistance = distance;
                }
            }
        }

        return Optional.ofNullable(best);
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
            if (!isPortalable(level, level.getBlockState(pos))) {
                return false;
            }
        }

        return true;
    }

    private static boolean hasFrontClearance(Level level, PortalTarget target, Direction up) {
        Direction face = Direction.byName(target.face());
        if (face == null) {
            return false;
        }

        Direction right = rightVector(face, up);
        Vec3 center = new Vec3(target.hitX(), target.hitY(), target.hitZ()).add(face.getUnitVec3().scale(0.12D));
        BlockPos[] front = new BlockPos[] {
                BlockPos.containing(center.add(right.getUnitVec3().scale(-0.49D)).add(up.getUnitVec3().scale(-0.99D))),
                BlockPos.containing(center.add(right.getUnitVec3().scale(0.49D)).add(up.getUnitVec3().scale(-0.99D))),
                BlockPos.containing(center.add(right.getUnitVec3().scale(-0.49D)).add(up.getUnitVec3().scale(0.99D))),
                BlockPos.containing(center.add(right.getUnitVec3().scale(0.49D)).add(up.getUnitVec3().scale(0.99D)))
        };

        for (BlockPos pos : front) {
            BlockState state = level.getBlockState(pos);
            if (!state.isAir() && !state.is(PortalModTags.PORTAL_NONBLOCKING) && state.getFluidState().isEmpty() && state.getCollisionShape(level, pos).isEmpty()) {
                continue;
            }
            if (!state.isAir() && !state.is(PortalModTags.PORTAL_NONBLOCKING)) {
                return false;
            }
        }

        return true;
    }

    private static AABB portalBox(PortalTarget target, Direction up) {
        Direction face = Direction.byName(target.face());
        if (face == null) {
            face = Direction.NORTH;
        }

        Direction right = rightVector(face, up);
        Vec3 center = new Vec3(target.hitX(), target.hitY(), target.hitZ()).add(face.getUnitVec3().scale(0.001D));
        AABB box = null;

        for (double sx : new double[]{-0.5D, 0.5D}) {
            for (double sy : new double[]{-1.0D, 1.0D}) {
                for (double sz : new double[]{-1.0D / 32.0D, 1.0D / 32.0D}) {
                    Vec3 point = center
                            .add(right.getUnitVec3().scale(sx))
                            .add(up.getUnitVec3().scale(sy))
                            .add(face.getUnitVec3().scale(sz));
                    AABB pointBox = new AABB(point, point);
                    box = box == null ? pointBox : box.minmax(pointBox);
                }
            }
        }

        return box == null ? new AABB(center, center) : box;
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

    private static void playMiss(Level level, Player player, Vec3 location) {
        level.playSound(null, location.x(), location.y(), location.z(), PortalModSounds.PORTALGUN_MISS, SoundSource.PLAYERS, 0.8F, 1.0F);

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SMOKE, location.x(), location.y(), location.z(), 8, 0.1D, 0.1D, 0.1D, 0.01D);
            if (player instanceof ServerPlayer serverPlayer) {
                ServerPlayNetworking.send(serverPlayer, new PortalGunEventPayload(PortalGunEventPayload.Event.FAIL_SHOT, location));
            }
        }
    }
}
