package net.portalmod.fabric.entity;

import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.portalmod.fabric.component.PortalTarget;
import net.portalmod.fabric.portal.PortalManager;
import net.portalmod.fabric.portal.PortalRecord;
import net.portalmod.fabric.registry.PortalModEntities;
import net.portalmod.fabric.registry.PortalModTags;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiPredicate;

public final class PortalEntity extends Entity {
    /**
     * Client-side pair lookup, wired by the client initializer to ClientPortalManager.
     * Lets common code ask "does this gun have a portal on the given side?" without
     * referencing client-only classes.
     */
    public static BiPredicate<UUID, Boolean> clientPairLookup;
    private static final EntityDataAccessor<Boolean> DATA_PRIMARY = SynchedEntityData.defineId(PortalEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> DATA_FACE = SynchedEntityData.defineId(PortalEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_UP = SynchedEntityData.defineId(PortalEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_HUE = SynchedEntityData.defineId(PortalEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_TARGET_X = SynchedEntityData.defineId(PortalEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TARGET_Y = SynchedEntityData.defineId(PortalEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TARGET_Z = SynchedEntityData.defineId(PortalEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> DATA_GUN_MOST = SynchedEntityData.defineId(PortalEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> DATA_GUN_LEAST = SynchedEntityData.defineId(PortalEntity.class, EntityDataSerializers.LONG);
    private static final double WIDTH = 1.0D;
    private static final double HEIGHT = 2.0D;
    private static final double DEPTH = 1.0D / 16.0D;

    private UUID gunId = new UUID(0L, 0L);
    private boolean primary = true;
    private PortalTarget target;
    private int age;

    public PortalEntity(EntityType<? extends PortalEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
        setInvulnerable(true);
    }

    public void configure(UUID gunId, boolean primary, PortalTarget target) {
        configure(gunId, primary, target, Direction.UP, primary ? "blue" : "orange");
    }

    public void configure(UUID gunId, boolean primary, PortalTarget target, Direction up, String hue) {
        this.gunId = gunId;
        this.primary = primary;
        this.target = target;
        Direction face = direction(target);
        Vec3 normal = face.getUnitVec3();
        snapTo(target.hitX() + normal.x() * 0.001D, target.hitY() + normal.y() * 0.001D, target.hitZ() + normal.z() * 0.001D);
        entityData.set(DATA_GUN_MOST, gunId.getMostSignificantBits());
        entityData.set(DATA_GUN_LEAST, gunId.getLeastSignificantBits());
        entityData.set(DATA_PRIMARY, primary);
        entityData.set(DATA_FACE, target.face());
        entityData.set(DATA_UP, up.getSerializedName());
        entityData.set(DATA_HUE, hue);
        entityData.set(DATA_TARGET_X, target.x());
        entityData.set(DATA_TARGET_Y, target.y());
        entityData.set(DATA_TARGET_Z, target.z());
        setYRot(face.toYRot());
        setXRot(face.getAxis() == Direction.Axis.Y ? -90.0F * face.getAxisDirection().getStep() : 0.0F);
        recalculateBoundingBox();
    }

    public UUID gunId() {
        return new UUID(entityData.get(DATA_GUN_MOST), entityData.get(DATA_GUN_LEAST));
    }

    public boolean primary() {
        return entityData.get(DATA_PRIMARY);
    }

    public String hue() {
        return entityData.get(DATA_HUE);
    }

    public int portalAge() {
        return age;
    }

    public Direction direction() {
        return direction(target().orElse(null), entityData.get(DATA_FACE));
    }

    public Direction up() {
        Direction up = Direction.byName(entityData.get(DATA_UP));
        Direction face = direction();
        if (up == null || up.getAxis() == face.getAxis()) {
            return face.getAxis() == Direction.Axis.Y ? Direction.NORTH : Direction.UP;
        }
        return up;
    }

    public Direction right() {
        Vec3 cross = up().getUnitVec3().cross(direction().getUnitVec3());
        return Direction.getApproximateNearest(cross.x(), cross.y(), cross.z());
    }

    public boolean isOpen() {
        if (level() instanceof ServerLevel serverLevel) {
            return PortalManager.get(serverLevel.getServer()).end(gunId(), !primary()).isPresent();
        }

        if (clientPairLookup != null) {
            return clientPairLookup.test(gunId(), !primary());
        }

        AABB search = getBoundingBox().inflate(128.0D);
        return level().getEntities(PortalModEntities.PORTAL, search, portal ->
                !portal.isRemoved() && portal.gunId().equals(gunId()) && portal.primary() != primary()).stream().findAny().isPresent();
    }

    public Optional<PortalTarget> target() {
        if (target != null) {
            return Optional.of(target);
        }

        return Optional.of(new PortalTarget(
                level().dimension().identifier().toString(),
                entityData.get(DATA_TARGET_X),
                entityData.get(DATA_TARGET_Y),
                entityData.get(DATA_TARGET_Z),
                entityData.get(DATA_FACE),
                getX(),
                getY(),
                getZ()
        ));
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        builder.define(DATA_PRIMARY, true);
        builder.define(DATA_FACE, "north");
        builder.define(DATA_UP, "up");
        builder.define(DATA_HUE, "blue");
        builder.define(DATA_TARGET_X, 0);
        builder.define(DATA_TARGET_Y, 0);
        builder.define(DATA_TARGET_Z, 0);
        builder.define(DATA_GUN_MOST, 0L);
        builder.define(DATA_GUN_LEAST, 0L);
    }

    @Override
    public void tick() {
        super.tick();
        age++;

        recalculateBoundingBox();

        if (level().isClientSide() || !(level() instanceof ServerLevel serverLevel) || isRemoved() || isOnPortalCooldown()) {
            return;
        }

        PortalManager manager = PortalManager.get(serverLevel.getServer());
        if (manager.isRevoked(getUUID())) {
            manager.clearRevoked(getUUID());
            discard();
            return;
        }

        Optional<PortalRecord> registered = manager.end(gunId(), primary());
        if (registered.isPresent() && !registered.get().entityUuid().equals(getUUID())) {
            // A newer portal was placed for this gun side while this entity was unloaded.
            discard();
            return;
        }

        if (registered.isEmpty()) {
            manager.ensureRegistered(serverLevel.getServer(), this);
        }

        if (!survives()) {
            discard();
            return;
        }

        keepDestinationLoaded(serverLevel, manager);
        teleportEntityIfCrossingPortal(serverLevel, null);
    }

    /**
     * Keeps the destination portal's chunks ticking while this end is open so entities can
     * pass through even when no player is near the far side.
     */
    private void keepDestinationLoaded(ServerLevel level, PortalManager manager) {
        if ((age % 40) != 0) {
            return;
        }

        manager.end(gunId(), !primary()).ifPresent(record -> {
            ServerLevel destinationLevel = manager.level(level.getServer(), record);
            if (destinationLevel == null) {
                return;
            }

            BlockPos destinationPos = BlockPos.containing(record.position());
            destinationLevel.getChunkSource().addTicketWithRadius(
                    net.minecraft.server.level.TicketType.PORTAL,
                    new net.minecraft.world.level.ChunkPos(destinationPos.getX() >> 4, destinationPos.getZ() >> 4),
                    1
            );
        });
    }

    @Override
    public void remove(RemovalReason reason) {
        if (reason.shouldDestroy() && !level().isClientSide() && level() instanceof ServerLevel serverLevel) {
            PortalManager.get(serverLevel.getServer()).remove(serverLevel.getServer(), gunId(), primary(), getUUID());
        }

        super.remove(reason);
    }

    public static void handleEntityMoved(Entity entity) {
        if (entity instanceof PortalEntity || entity.isRemoved()) {
            return;
        }

        if (entity.level() instanceof ServerLevel serverLevel) {
            teleportEntityIfCrossingPortal(serverLevel, entity);
        } else {
            teleportEntityIfCrossingPortalClient(entity);
        }
    }

    private static void teleportEntityIfCrossingPortal(ServerLevel serverLevel, Entity movedEntity) {
        if (movedEntity != null) {
            if (!canTeleport(movedEntity)) {
                return;
            }

            Vec3 centerOffset = movedEntity.getBoundingBox().getCenter().subtract(movedEntity.position());
            Vec3 oldCenter = new Vec3(movedEntity.xOld, movedEntity.yOld, movedEntity.zOld).add(centerOffset);
            Vec3 centerNow = movedEntity.getBoundingBox().getCenter();
            AABB travelBox = new AABB(oldCenter, centerNow).inflate(1.25D);

            for (PortalEntity portal : serverLevel.getEntities(PortalModEntities.PORTAL, travelBox, PortalEntity::isOpen)) {
                PortalEntity pair = portal.findPair(serverLevel);
                if (pair != null && portal.tryTeleportEntity(movedEntity, pair, oldCenter, centerNow, centerOffset)) {
                    return;
                }
            }
            return;
        }

        for (PortalEntity portal : serverLevel.getEntities(PortalModEntities.PORTAL, portal -> !portal.isRemoved() && portal.isOpen())) {
            PortalEntity pair = portal.findPair(serverLevel);
            if (pair == null || pair.target().isEmpty()) {
                continue;
            }

            AABB area = portal.getBoundingBox().inflate(1.25D);
            for (Entity entity : serverLevel.getEntities(portal, area, PortalEntity::canTeleport)) {
                Vec3 centerOffset = entity.getBoundingBox().getCenter().subtract(entity.position());
                Vec3 oldCenter = new Vec3(entity.xOld, entity.yOld, entity.zOld).add(centerOffset);
                Vec3 centerNow = entity.getBoundingBox().getCenter();
                if (portal.tryTeleportEntity(entity, pair, oldCenter, centerNow, centerOffset)) {
                    break;
                }
            }
        }
    }

    private static void teleportEntityIfCrossingPortalClient(Entity movedEntity) {
        if (!canTeleport(movedEntity)) {
            return;
        }

        Vec3 centerOffset = movedEntity.getBoundingBox().getCenter().subtract(movedEntity.position());
        Vec3 oldCenter = new Vec3(movedEntity.xOld, movedEntity.yOld, movedEntity.zOld).add(centerOffset);
        Vec3 centerNow = movedEntity.getBoundingBox().getCenter();
        AABB travelBox = new AABB(oldCenter, centerNow).inflate(1.25D);

        for (PortalEntity portal : movedEntity.level().getEntities(PortalModEntities.PORTAL, travelBox, PortalEntity::isOpen)) {
            PortalEntity pair = portal.findPairClient();
            if (pair != null && portal.tryTeleportEntityClient(movedEntity, pair, oldCenter, centerNow, centerOffset)) {
                return;
            }
        }
    }

    private boolean tryTeleportEntity(Entity entity, PortalEntity pair, Vec3 oldCenter, Vec3 centerNow, Vec3 centerOffset) {
        Direction face = direction();
        Vec3 center = position();
        Vec3 normal = face.getUnitVec3();
        Crossing crossing = findCrossing(oldCenter, centerNow, center, normal);

        if (crossing != null && entity.getDeltaMovement().dot(normal) <= 0.02D && isTeleportPointWithinPortal(crossing.point())) {
            teleportEntity(entity, this, pair, centerOffset, crossing.point(), centerNow.subtract(crossing.point()));
            return true;
        }

        return false;
    }

    private boolean tryTeleportEntityClient(Entity entity, PortalEntity pair, Vec3 oldCenter, Vec3 centerNow, Vec3 centerOffset) {
        Direction face = direction();
        Vec3 center = position();
        Vec3 normal = face.getUnitVec3();
        Crossing crossing = findCrossing(oldCenter, centerNow, center, normal);

        if (crossing != null && entity.getDeltaMovement().dot(normal) <= 0.02D && isTeleportPointWithinPortal(crossing.point())) {
            teleportEntityClient(entity, this, pair, centerOffset, crossing.point(), centerNow.subtract(crossing.point()));
            return true;
        }

        return false;
    }

    private PortalEntity findPair(ServerLevel level) {
        PortalManager manager = PortalManager.get(level.getServer());
        PortalRecord record = manager.end(gunId(), !primary()).orElse(null);
        if (record != null) {
            PortalEntity resolved = manager.resolve(level.getServer(), record);
            if (resolved != null) {
                return resolved;
            }

            // Destination entity not loaded yet: request its chunk and fall through to a scan.
            ServerLevel destinationLevel = manager.level(level.getServer(), record);
            if (destinationLevel != null) {
                BlockPos destinationPos = BlockPos.containing(record.position());
                destinationLevel.getChunkSource().addTicketWithRadius(
                        net.minecraft.server.level.TicketType.PORTAL,
                        new net.minecraft.world.level.ChunkPos(destinationPos.getX() >> 4, destinationPos.getZ() >> 4),
                        1
                );
            }
        }

        UUID id = gunId();
        boolean otherSide = !primary();
        for (ServerLevel serverLevel : level.getServer().getAllLevels()) {
            PortalEntity pair = serverLevel.getEntities(PortalModEntities.PORTAL, portal ->
                    !portal.isRemoved() && portal.gunId().equals(id) && portal.primary() == otherSide).stream().findFirst().orElse(null);
            if (pair != null) {
                return pair;
            }
        }

        return null;
    }

    private PortalEntity findPairClient() {
        UUID id = gunId();
        boolean otherSide = !primary();
        AABB search = getBoundingBox().inflate(128.0D);
        return level().getEntities(PortalModEntities.PORTAL, search, portal ->
                !portal.isRemoved() && portal.gunId().equals(id) && portal.primary() == otherSide).stream().findFirst().orElse(null);
    }

    private static boolean canTeleport(Entity entity) {
        return !(entity instanceof PortalEntity) && entity.canInteractWithLevel() && !entity.isOnPortalCooldown();
    }

    private static Crossing findCrossing(Vec3 oldCenter, Vec3 newCenter, Vec3 portalCenter, Vec3 normal) {
        double oldPlane = oldCenter.subtract(portalCenter).dot(normal);
        double newPlane = newCenter.subtract(portalCenter).dot(normal);
        if (oldPlane < -0.001D || newPlane > 0.001D) {
            return null;
        }

        double denominator = oldPlane - newPlane;
        if (Math.abs(denominator) < 1.0E-7D) {
            return null;
        }

        double progress = oldPlane / denominator;
        if (progress < 0.0D || progress > 1.0D) {
            return null;
        }

        return new Crossing(oldCenter.lerp(newCenter, progress), progress);
    }

    private static void teleportEntity(Entity entity, PortalEntity source, PortalEntity destination, Vec3 centerOffset, Vec3 crossingPoint, Vec3 remainingMovement) {
        PortalTarget target = destination.target().orElse(null);
        if (target == null || !(destination.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        Vec3 teleportedCrossing = source.teleportPoint(crossingPoint, destination);
        Vec3 teleportedRemaining = source.teleportVector(remainingMovement, destination);
        Vec3 destinationCenter = teleportedCrossing
                .add(teleportedRemaining)
                .add(destination.direction().getUnitVec3().scale(0.28D));
        Vec3 destinationPos = destinationCenter.subtract(centerOffset);
        Vec3 velocity = source.teleportVector(entity.getDeltaMovement(), destination);
        Vec3 look = source.teleportVector(entity.getLookAngle(), destination).normalize();
        float yaw = (float) (Math.atan2(look.z(), look.x()) * 180.0D / Math.PI) - 90.0F;
        float pitch = (float) (-(Math.atan2(look.y(), Math.sqrt(look.x() * look.x() + look.z() * look.z())) * 180.0D / Math.PI));

        entity.teleportTo(serverLevel, destinationPos.x(), destinationPos.y(), destinationPos.z(), java.util.Set.of(), yaw, pitch, true);
        entity.setOldPosAndRot(destinationPos, yaw, pitch);
        entity.setDeltaMovement(velocity);
        entity.setPortalCooldown(2);
    }

    private static void teleportEntityClient(Entity entity, PortalEntity source, PortalEntity destination, Vec3 centerOffset, Vec3 crossingPoint, Vec3 remainingMovement) {
        PortalTarget target = destination.target().orElse(null);
        if (target == null) {
            return;
        }

        Vec3 teleportedCrossing = source.teleportPoint(crossingPoint, destination);
        Vec3 teleportedRemaining = source.teleportVector(remainingMovement, destination);
        Vec3 destinationCenter = teleportedCrossing
                .add(teleportedRemaining)
                .add(destination.direction().getUnitVec3().scale(0.28D));
        Vec3 destinationPos = destinationCenter.subtract(centerOffset);
        Vec3 velocity = source.teleportVector(entity.getDeltaMovement(), destination);
        Vec3 look = source.teleportVector(entity.getLookAngle(), destination).normalize();
        float yaw = (float) (Math.atan2(look.z(), look.x()) * 180.0D / Math.PI) - 90.0F;
        float pitch = (float) (-(Math.atan2(look.y(), Math.sqrt(look.x() * look.x() + look.z() * look.z())) * 180.0D / Math.PI));

        entity.teleportTo(destinationPos.x(), destinationPos.y(), destinationPos.z());
        entity.setYRot(yaw);
        entity.setXRot(pitch);
        entity.setOldPosAndRot(destinationPos, yaw, pitch);
        entity.setDeltaMovement(velocity);
        entity.setPortalCooldown(2);
    }

    /**
     * Portal funneling, ported from the Forge mod: entities falling fast toward a floor
     * portal are gently pulled toward its center so they pass through cleanly.
     */
    public static Vec3 applyFunneling(Entity entity, Vec3 delta) {
        final double funnelHeight = 32.0D;

        if (entity instanceof PortalEntity || entity.isSpectator() || entity.isOnPortalCooldown()) {
            return delta;
        }

        boolean fastEnough = delta.y() < -0.5D;
        boolean fallingMore = Math.abs(delta.y()) > Math.abs(delta.x()) && Math.abs(delta.y()) > Math.abs(delta.z());
        if (!fastEnough || !fallingMore) {
            return delta;
        }

        if (entity instanceof Player player) {
            double downDot = player.getViewVector(1.0F).dot(new Vec3(0.0D, -1.0D, 0.0D));
            boolean steering = Math.abs(player.getDeltaMovement().x()) + Math.abs(player.getDeltaMovement().z()) > 0.45D;
            if (downDot < 0.5D || steering) {
                return delta;
            }
        }

        Vec3 entityPos = entity.position();
        AABB travelBox = entity.getBoundingBox()
                .expandTowards(delta)
                .expandTowards(0.0D, -funnelHeight, 0.0D)
                .inflate(3.0D, 0.0D, 3.0D);

        List<? extends PortalEntity> portals = entity.level().getEntities(PortalModEntities.PORTAL, travelBox, portal ->
                portal.isOpen() && portal.direction() == Direction.UP && portal.position().y() < entityPos.y());

        if (portals.isEmpty()) {
            return delta;
        }

        PortalEntity portal = portals.stream().reduce((first, second) -> {
            double hDistance1 = horizontalDistanceSqr(first.position(), entityPos);
            double hDistance2 = horizontalDistanceSqr(second.position(), entityPos);
            if (hDistance1 == hDistance2) {
                return (second.position().y() - entityPos.y()) < (first.position().y() - entityPos.y()) ? second : first;
            }
            return hDistance2 < hDistance1 ? second : first;
        }).orElseThrow();

        ClipContext clipContext = new ClipContext(
                entity.getEyePosition(1.0F),
                portal.position(),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.ANY,
                entity
        );
        if (entity.level().clip(clipContext).getType() != HitResult.Type.MISS) {
            return delta;
        }

        Vec3 relative = entityPos.subtract(portal.position());
        Vec3 flatRelative = new Vec3(relative.x(), 0.0D, relative.z());
        double coneRadius = relative.y() * 0.2D;
        boolean inCone = relative.y() < funnelHeight && relative.y() > 0.0D && flatRelative.length() < coneRadius;
        if (!inCone) {
            return delta;
        }

        double currentHeight = relative.y();
        double startHeight = Math.min(currentHeight + entity.fallDistance, funnelHeight);
        double progress = 1.0D - currentHeight / startHeight;
        double distanceFactor = 1.0D - Math.exp(-2.0D * progress);
        Vec3 funnelAcceleration = flatRelative.scale(-distanceFactor);

        return delta.add(funnelAcceleration);
    }

    private static double horizontalDistanceSqr(Vec3 a, Vec3 b) {
        double dx = a.x() - b.x();
        double dz = a.z() - b.z();
        return dx * dx + dz * dz;
    }

    public Vec3 teleportPoint(Vec3 point, PortalEntity destination) {
        Vec3 offset = point.subtract(position());
        Vec3 srcRight = right().getUnitVec3();
        Vec3 srcUp = up().getUnitVec3();
        Vec3 srcNormal = direction().getUnitVec3();
        Vec3 dstRight = destination.up().getUnitVec3().cross(destination.direction().getOpposite().getUnitVec3());
        Vec3 dstUp = destination.up().getUnitVec3();
        Vec3 dstNormal = destination.direction().getOpposite().getUnitVec3();
        return destination.position()
                .add(dstRight.scale(offset.dot(srcRight)))
                .add(dstUp.scale(offset.dot(srcUp)))
                .add(dstNormal.scale(offset.dot(srcNormal)));
    }

    public Vec3 teleportVector(Vec3 vector, PortalEntity destination) {
        Vec3 srcRight = right().getUnitVec3();
        Vec3 srcUp = up().getUnitVec3();
        Vec3 srcNormal = direction().getUnitVec3();
        Vec3 dstRight = destination.up().getUnitVec3().cross(destination.direction().getOpposite().getUnitVec3());
        Vec3 dstUp = destination.up().getUnitVec3();
        Vec3 dstNormal = destination.direction().getOpposite().getUnitVec3();
        return dstRight.scale(vector.dot(srcRight))
                .add(dstUp.scale(vector.dot(srcUp)))
                .add(dstNormal.scale(vector.dot(srcNormal)));
    }

    private boolean isPointWithinPortal(Vec3 point) {
        Vec3 offset = point.subtract(position());
        double localX = offset.dot(right().getUnitVec3());
        double localY = offset.dot(up().getUnitVec3());
        return Math.abs(localX) <= WIDTH * 0.5D + 0.35D && Math.abs(localY) <= HEIGHT * 0.5D + 0.35D;
    }

    private boolean isTeleportPointWithinPortal(Vec3 point) {
        Vec3 offset = point.subtract(position());
        double localX = offset.dot(right().getUnitVec3());
        double localY = offset.dot(up().getUnitVec3());
        return Math.abs(localX) <= WIDTH * 0.5D + 0.03D && Math.abs(localY) <= HEIGHT * 0.5D + 0.03D;
    }

    public boolean isBlockBehindPortal(BlockPos pos) {
        Vec3 blockCenter = Vec3.atCenterOf(pos);
        Vec3 offset = blockCenter.subtract(position());
        double normalDistance = offset.dot(direction().getUnitVec3());
        double localX = offset.dot(right().getUnitVec3());
        double localY = offset.dot(up().getUnitVec3());
        return normalDistance <= 0.35D
                && normalDistance >= -1.15D
                && Math.abs(localX) <= WIDTH * 0.5D + 0.55D
                && Math.abs(localY) <= HEIGHT * 0.5D + 0.55D;
    }

    private boolean survives() {
        Optional<PortalTarget> current = target();
        if (current.isEmpty()) {
            return false;
        }

        Direction face = direction();
        Direction up = up();
        Direction right = right();
        Vec3 center = position().add(face.getUnitVec3().scale(-0.01D));
        BlockPos[] support = new BlockPos[] {
                BlockPos.containing(center.add(right.getUnitVec3().scale(-0.49D)).add(up.getUnitVec3().scale(-0.99D))),
                BlockPos.containing(center.add(right.getUnitVec3().scale(0.49D)).add(up.getUnitVec3().scale(-0.99D))),
                BlockPos.containing(center.add(right.getUnitVec3().scale(-0.49D)).add(up.getUnitVec3().scale(0.99D))),
                BlockPos.containing(center.add(right.getUnitVec3().scale(0.49D)).add(up.getUnitVec3().scale(0.99D)))
        };

        for (BlockPos pos : support) {
            net.minecraft.world.level.block.state.BlockState state = level().getBlockState(pos);
            if (!net.portalmod.fabric.portal.PortalPlacementService.isPortalable(level(), state)) {
                return false;
            }
        }

        return true;
    }

    public static boolean shouldSkipCollision(BlockGetter blockGetter, BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
        if (!(blockGetter instanceof Level level) || !(context instanceof net.minecraft.world.phys.shapes.EntityCollisionContext entityContext)) {
            return false;
        }

        Entity entity = entityContext.getEntity();
        if (entity == null || entity instanceof PortalEntity || blockGetter.getBlockState(pos).isAir()) {
            return false;
        }

        AABB travelBox = entity.getBoundingBox().expandTowards(entity.getDeltaMovement()).inflate(0.35D);
        AABB searchBox = travelBox.inflate(2.0D);
        return level.getEntities(PortalModEntities.PORTAL, searchBox, portal ->
                portal.shouldSkipCollisionFor(entity, pos, travelBox)
        ).stream().findAny().isPresent();
    }

    private boolean shouldSkipCollisionFor(Entity entity, BlockPos pos, AABB travelBox) {
        if (!isOpen() || !getBoundingBox().inflate(0.75D).intersects(travelBox) || !isBlockBehindPortal(pos)) {
            return false;
        }

        Vec3 normal = direction().getUnitVec3();
        boolean movingIntoPortal = entity.getDeltaMovement().dot(normal) <= 0.05D;
        boolean exitingPortal = entity.isOnPortalCooldown() && entity.getDeltaMovement().dot(normal) >= -0.15D;
        if (!movingIntoPortal && !exitingPortal) {
            return false;
        }

        AABB entityBox = entity.getBoundingBox();
        return isPointWithinPortal(entityBox.getCenter())
                || isPointWithinPortal(entity.getEyePosition())
                || isPointWithinPortal(new Vec3(entityBox.minX, entityBox.getCenter().y(), entityBox.minZ))
                || isPointWithinPortal(new Vec3(entityBox.maxX, entityBox.getCenter().y(), entityBox.maxZ));
    }

    private void recalculateBoundingBox() {
        Vec3 center = position();
        Vec3 normal = direction().getUnitVec3();
        Vec3 right = right().getUnitVec3();
        Vec3 up = up().getUnitVec3();
        AABB box = null;

        for (double sx : new double[]{-0.5D, 0.5D}) {
            for (double sy : new double[]{-1.0D, 1.0D}) {
                for (double sz : new double[]{-DEPTH * 0.5D, DEPTH * 0.5D}) {
                    Vec3 point = center.add(right.scale(sx)).add(up.scale(sy)).add(normal.scale(sz));
                    AABB pointBox = new AABB(point, point);
                    box = box == null ? pointBox : box.minmax(pointBox);
                }
            }
        }

        if (box != null) {
            setBoundingBox(box);
        }
    }

    private record Crossing(Vec3 point, double progress) {
    }

    private static Direction direction(PortalTarget target) {
        return direction(target, "north");
    }

    private static Direction direction(PortalTarget target, String fallback) {
        Direction direction = Direction.byName(target == null ? fallback : target.face());
        return direction == null ? Direction.NORTH : direction;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        gunId = new UUID(input.getLongOr("gun_most", 0L), input.getLongOr("gun_least", 0L));
        primary = input.getBooleanOr("primary", true);
        age = input.getIntOr("age", 0);
        Direction up = Direction.byName(input.getStringOr("up", "up"));
        String hue = input.getStringOr("hue", primary ? "blue" : "orange");
        target = new PortalTarget(
                input.getStringOr("dimension", level().dimension().identifier().toString()),
                input.getIntOr("x", blockPosition().getX()),
                input.getIntOr("y", blockPosition().getY()),
                input.getIntOr("z", blockPosition().getZ()),
                input.getStringOr("face", "north"),
                input.getDoubleOr("hit_x", getX()),
                input.getDoubleOr("hit_y", getY()),
                input.getDoubleOr("hit_z", getZ())
        );
        configure(gunId, primary, target, up == null ? Direction.UP : up, hue);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        UUID id = gunId();
        output.putLong("gun_most", id.getMostSignificantBits());
        output.putLong("gun_least", id.getLeastSignificantBits());
        output.putBoolean("primary", primary());
        output.putInt("age", age);

        Optional<PortalTarget> currentTarget = target();
        if (currentTarget.isPresent()) {
            PortalTarget target = currentTarget.get();
            output.putString("dimension", target.dimension());
            output.putInt("x", target.x());
            output.putInt("y", target.y());
            output.putInt("z", target.z());
            output.putString("face", target.face());
            output.putString("up", up().getSerializedName());
            output.putString("hue", hue());
            output.putDouble("hit_x", target.hitX());
            output.putDouble("hit_y", target.hitY());
            output.putDouble("hit_z", target.hitZ());
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource damageSource, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }
}
