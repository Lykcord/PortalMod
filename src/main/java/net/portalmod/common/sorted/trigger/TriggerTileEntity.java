package net.portalmod.common.sorted.trigger;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.portalmod.core.init.TileEntityTypeInit;

import java.util.List;

public class TriggerTileEntity extends BlockEntity {
    private BlockPos fieldStart;
    private BlockPos fieldEnd;

    private int entityCount = 0;

    private Player configuringPlayer;

    public TriggerTileEntity(BlockPos pos, BlockState state) {
        super(TileEntityTypeInit.TRIGGER.get(), pos, state);
    }

    public void tick() {
        if (this.level == null) {
            return;
        }

        BlockState state = this.level.getBlockState(this.worldPosition);

        if (!this.hasField()) {
            if (state.getValue(TriggerBlock.STATE) != TriggerState.NULL) {
                this.level.setBlockAndUpdate(this.worldPosition, state.setValue(TriggerBlock.STATE, TriggerState.NULL));
            }
            this.entityCount = 0;
            return;
        }

        AABB aabb = this.getField();
        aabb = aabb.move(this.worldPosition);

        List<LivingEntity> entities =
                level.getEntitiesOfClass(
                        LivingEntity.class,
                        aabb,
                        state.getValue(TriggerBlock.TYPE).getPredicate().and(entity -> !entity.isSpectator()));

        if (this.entityCount != entities.size()) {
            this.entityCount = entities.size();
            this.level.updateNeighborsAt(this.worldPosition, state.getBlock());
        }

        boolean shouldActivate = !this.isBeingConfigured() && !entities.isEmpty();

        if (!state.getValue(TriggerBlock.STATE).isActive(shouldActivate)) {
            this.level.setBlockAndUpdate(
                    this.worldPosition, state.setValue(TriggerBlock.STATE, TriggerState.fromActive(shouldActivate)));
        }
    }

    public void startConfiguration(ServerPlayer player) {
        this.configuringPlayer = player;
    }

    public void endConfiguration() {
        this.configuringPlayer = null;
    }

    public boolean isBeingConfigured() {
        return this.configuringPlayer != null;
    }

    public Player getConfiguringPlayer() {
        return this.configuringPlayer;
    }

    public void setField(BlockPos start, BlockPos end) {
        this.fieldStart = start;
        this.fieldEnd = end;
        this.setChanged();
    }

    public boolean hasField() {
        return this.fieldStart != null && this.fieldEnd != null;
    }

    public AABB getField() {
        if (!this.hasField()) {
            return null;
        }
        return new AABB(
                        fieldStart.getX(),
                        fieldStart.getY(),
                        fieldStart.getZ(),
                        fieldEnd.getX(),
                        fieldEnd.getY(),
                        fieldEnd.getZ())
                .expandTowards(1, 1, 1);
    }

    public TriggerType getTriggerType() {
        return this.getBlockState().getValue(TriggerBlock.TYPE);
    }

    @Override
    protected void saveAdditional(ValueOutput nbt) {
        super.saveAdditional(nbt);
        if (this.fieldStart != null && this.fieldEnd != null) {
            ValueOutput start = nbt.child("start");
            start.putInt("x", this.fieldStart.getX());
            start.putInt("y", this.fieldStart.getY());
            start.putInt("z", this.fieldStart.getZ());
            ValueOutput end = nbt.child("end");
            end.putInt("x", this.fieldEnd.getX());
            end.putInt("y", this.fieldEnd.getY());
            end.putInt("z", this.fieldEnd.getZ());
        }
    }

    @Override
    protected void loadAdditional(ValueInput nbt) {
        super.loadAdditional(nbt);
        this.fieldStart = null;
        this.fieldEnd = null;
        ValueInput start = nbt.childOrEmpty("start");
        ValueInput end = nbt.childOrEmpty("end");
        if (start.getInt("x").isEmpty()
                || start.getInt("y").isEmpty()
                || start.getInt("z").isEmpty()
                || end.getInt("x").isEmpty()
                || end.getInt("y").isEmpty()
                || end.getInt("z").isEmpty()) {
            return;
        }
        this.fieldStart = new BlockPos(start.getIntOr("x", 0), start.getIntOr("y", 0), start.getIntOr("z", 0));
        this.fieldEnd = new BlockPos(end.getIntOr("x", 0), end.getIntOr("y", 0), end.getIntOr("z", 0));
    }

    public void rotate(Rotation rotation) {
        if (!hasField()) {
            return;
        }
        this.fieldStart = this.fieldStart.rotate(rotation);
        this.fieldEnd = this.fieldEnd.rotate(rotation);
    }

    public void mirror(Mirror mirror) {
        if (!hasField()) {
            return;
        }
        int flipX = mirror == Mirror.FRONT_BACK ? -1 : 1;
        int flipZ = mirror == Mirror.LEFT_RIGHT ? -1 : 1;
        this.fieldStart =
                new BlockPos(this.fieldStart.getX() * flipX, this.fieldStart.getY(), this.fieldStart.getZ() * flipZ);
        this.fieldEnd = new BlockPos(this.fieldEnd.getX() * flipX, this.fieldEnd.getY(), this.fieldEnd.getZ() * flipZ);
    }

    // chunk update

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider lookup) {
        return saveWithoutMetadata(lookup);
    }

    // block update

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public AABB getRenderBoundingBox() {
        return new AABB(this.getBlockPos()).inflate(1E30);
    }

    public double getViewDistance() {
        return 256.0D;
    }

    public int getEntityCount() {
        return entityCount;
    }
}
