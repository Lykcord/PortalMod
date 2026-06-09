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
import net.minecraft.world.phys.Vec3;
import net.portalmod.fabric.component.PortalTarget;
import net.portalmod.fabric.registry.PortalModEntities;

import java.util.Optional;
import java.util.UUID;

public final class PortalEntity extends Entity {
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
        if (!(level() instanceof ServerLevel serverLevel)) {
            return true;
        }
        return findPair(serverLevel) != null;
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

        recalculateBoundingBox();

        if (level().isClientSide() || !(level() instanceof ServerLevel serverLevel) || isRemoved() || isOnPortalCooldown()) {
            return;
        }

        Optional<PortalTarget> current = target();
        if (current.isEmpty()) {
            return;
        }

        PortalEntity pair = findPair(serverLevel);
        if (pair == null || pair.target().isEmpty()) {
            return;
        }

        Direction face = direction();
        Vec3 center = position();
        Vec3 normal = face.getUnitVec3();
        AABB area = getBoundingBox().inflate(0.35D);

        for (Entity entity : serverLevel.getEntities(this, area, entity -> canTeleport(entity))) {
            Vec3 oldCenter = new Vec3((entity.getBoundingBox().minX + entity.getBoundingBox().maxX) * 0.5D - entity.getDeltaMovement().x(),
                    (entity.getBoundingBox().minY + entity.getBoundingBox().maxY) * 0.5D - entity.getDeltaMovement().y(),
                    (entity.getBoundingBox().minZ + entity.getBoundingBox().maxZ) * 0.5D - entity.getDeltaMovement().z());
            Vec3 centerNow = entity.getBoundingBox().getCenter();
            double oldPlane = oldCenter.subtract(center).dot(normal);
            double newPlane = centerNow.subtract(center).dot(normal);

            if (oldPlane > -0.25D && newPlane <= 0.08D && entity.getDeltaMovement().dot(normal) <= 0.0D && isPointWithinPortal(centerNow)) {
                teleportEntity(entity, this, pair);
            }
        }
    }

    private PortalEntity findPair(ServerLevel level) {
        UUID id = gunId();
        boolean otherSide = !primary();
        return level.getEntities(PortalModEntities.PORTAL, portal ->
                !portal.isRemoved() && portal.gunId().equals(id) && portal.primary() == otherSide).stream().findFirst().orElse(null);
    }

    private static boolean canTeleport(Entity entity) {
        return !(entity instanceof PortalEntity) && entity.canInteractWithLevel() && !entity.isOnPortalCooldown();
    }

    private static void teleportEntity(Entity entity, PortalEntity source, PortalEntity destination) {
        PortalTarget target = destination.target().orElse(null);
        if (target == null || !(destination.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        Vec3 centerDelta = entity.getBoundingBox().getCenter().subtract(entity.position());
        Vec3 teleportedCenter = source.teleportPoint(entity.getBoundingBox().getCenter(), destination);
        Vec3 destinationPos = teleportedCenter.subtract(centerDelta).add(destination.direction().getUnitVec3().scale(0.08D));
        Vec3 velocity = source.teleportVector(entity.getDeltaMovement(), destination);
        Vec3 look = source.teleportVector(entity.getLookAngle(), destination).normalize();
        float yaw = (float) (Math.atan2(look.z(), look.x()) * 180.0D / Math.PI) - 90.0F;
        float pitch = (float) (-(Math.atan2(look.y(), Math.sqrt(look.x() * look.x() + look.z() * look.z())) * 180.0D / Math.PI));

        entity.teleportTo(serverLevel, destinationPos.x(), destinationPos.y(), destinationPos.z(), java.util.Set.of(), yaw, pitch, true);
        entity.setDeltaMovement(velocity);
        entity.setPortalCooldown(12);
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

    public static boolean shouldSkipCollision(BlockGetter blockGetter, BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
        if (!(blockGetter instanceof Level level) || !(context instanceof net.minecraft.world.phys.shapes.EntityCollisionContext entityContext)) {
            return false;
        }

        Entity entity = entityContext.getEntity();
        if (entity == null || entity instanceof PortalEntity || blockGetter.getBlockState(pos).isAir()) {
            return false;
        }

        AABB travelBox = entity.getBoundingBox().expandTowards(entity.getDeltaMovement()).inflate(0.25D);
        AABB searchBox = travelBox.inflate(2.0D);
        return level.getEntities(PortalModEntities.PORTAL, searchBox, portal ->
                portal.isOpen()
                        && portal.getBoundingBox().inflate(0.5D).intersects(travelBox)
                        && portal.isPointWithinPortal(entity.getBoundingBox().getCenter())
                        && portal.isBlockBehindPortal(pos)
                        && entity.getDeltaMovement().dot(portal.direction().getUnitVec3()) <= 0.05D
        ).stream().findAny().isPresent();
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
