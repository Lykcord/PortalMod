package net.portalmod.common.sorted.antline;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.portalmod.core.init.BlockInit;
import net.portalmod.core.init.TileEntityTypeInit;
import net.portalmod.core.util.ModUtil;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

public class AntlineTileEntity extends BlockEntity {
    private final SideMap sideMap = new SideMap();

    public SideMap getSideMap() {
        return sideMap;
    }

    public AntlineTileEntity(BlockPos pos, BlockState state) {
        super(TileEntityTypeInit.ANTLINE.get(), pos, state);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (sideMap.isBlank()) {
            sideMap.makeDefault();
        }
        output.store(
                "AntlineSides", CompoundTag.CODEC, sideMap.get(new CompoundTag()).getCompoundOrEmpty("AntlineSides"));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        CompoundTag nbt = new CompoundTag();
        nbt.put("AntlineSides", input.read("AntlineSides", CompoundTag.CODEC).orElseGet(CompoundTag::new));
        sideMap.set(nbt);
    }

    public void load(CompoundTag nbt, boolean complete) {
        if (complete) {
            sideMap.set(nbt);
        } else {
            sideMap.merge(nbt);
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level.isClientSide()) {
            return;
        }

        SideMap sidemap = this.getSideMap();
        int count = sidemap.getSideCount();

        for (int i = 0; i < count - 1; i++) {
            AntlineBlock.dropResources(getBlock().defaultBlockState(), level, this.getBlockPos());
        }

        super.preRemoveSideEffects(pos, state);
    }

    private static Block getBlock() {
        return BlockInit.ANTLINE.get();
    }

    @Override
    public SideMap getRenderData() {
        SideMap snapshot = new SideMap();
        sideMap.forEach((direction, side) -> snapshot.put(direction, new Side(side.sideDir, side.value)));
        return snapshot;
    }

    public void rotate(Rotation rotation) {
        SideMap initial = (SideMap) this.sideMap.clone();

        // Horizontal faces
        for (Direction direction : Direction.values()) {
            Side side = initial.get(direction);
            side.setSideDirection(rotation.rotate(side.toDirection()));
            this.sideMap.put(rotation.rotate(direction), side);
        }

        // Up/Down
        this.sideMap.get(Direction.UP).rotate(ModUtil.getRotationAmount(rotation));
        this.sideMap.get(Direction.DOWN).rotate(ModUtil.getRotationAmount(rotation));
    }

    public void mirror(Mirror mirror) {
        if (mirror == Mirror.NONE) {
            return;
        }

        SideMap initial = (SideMap) this.sideMap.clone();

        for (Direction direction : Direction.values()) {
            Side side = initial.get(direction);
            // Turn side the other way
            side.setSideDirection(mirror.mirror(side.toDirection()));

            // Flip connections
            boolean onWall = direction.getAxis() != Direction.Axis.Y;
            side.mirror(onWall || (mirror == Mirror.FRONT_BACK));

            this.sideMap.put(mirror.mirror(direction), side);
        }
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

    public static class SideMap extends HashMap<Direction, Side> {

        public void makeDefault() {
            for (Direction direction : Direction.values()) {
                put(direction, direction == Direction.DOWN ? Side.dot(direction) : Side.emptySide(direction));
            }
        }

        public SideMap() {
            for (Direction direction : Direction.values()) {
                this.put(direction, Side.emptySide(direction));
            }
        }

        public boolean isBlank() {
            for (Direction direction : Direction.values()) {
                if (!get(direction).isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        public boolean hasSide(Direction direction) {
            return !get(direction).isEmpty();
        }

        public void set(CompoundTag nbt) {
            CompoundTag data = nbt.getCompoundOrEmpty("AntlineSides");
            for (Direction direction : Direction.values()) {
                this.put(direction, new Side(direction, data.getByteOr(direction.getName(), (byte) 0)));
            }
        }

        public CompoundTag get(CompoundTag nbt) {
            CompoundTag data = new CompoundTag();
            for (Direction direction : Direction.values()) {
                data.putByte(direction.getName(), this.get(direction).value);
            }
            nbt.put("AntlineSides", data);
            return nbt;
        }

        public void merge(CompoundTag nbt) {
            for (String s : nbt.keySet()) {
                this.put(
                        Direction.valueOf(s.toUpperCase()),
                        new Side(Direction.valueOf(s.toUpperCase()), nbt.getByteOr(s, (byte) 0)));
            }
        }

        public int getSideCount() {
            return (int) values().stream().filter(v -> v.value != 0xF).count();
        }

        public void removeSide(Direction direction) {
            this.put(direction, Side.emptySide(direction));
        }
    }

    // Values:
    //  0000 - dot
    //  0100 - relative east connected
    //  1111 - empty
    // 0???? - inactive
    // 1???? - active

    public static class Side {
        private byte value;
        private Direction sideDir;

        public Side(Direction sideDir, byte value) {
            this.sideDir = sideDir;
            this.value = value;
        }

        public static Side emptySide(Direction direction) {
            return new Side(direction, (byte) 0xF);
        }

        public static Side dot(Direction direction) {
            return new Side(direction, (byte) 0);
        }

        public void setActualValue(byte value) {
            this.value = value;
        }

        public void setValue(byte value) {
            this.value = (byte) ((this.value & 0b00010000) | (value & 0x0F));
        }

        public void setSideDirection(Direction direction) {
            this.sideDir = direction;
        }

        public boolean isEmpty() {
            return getValue() == 0xF;
        }

        public boolean isConnectable() {
            return countConnections() <= 2;
        }

        public boolean isConnectableWith(Direction direction) {
            return isConnectable() || hasConnection(direction);
        }

        public Set<Direction> absoluteConnections() {
            return Arrays.stream(Direction.values()).filter(this::hasConnection).collect(Collectors.toSet());
        }

        public static byte valueByDirection(Direction direction) {
            switch (direction) {
                case NORTH:
                    return 0b1000; // 1000 north
                case EAST:
                    return 0b0100; // 0100 east
                case SOUTH:
                    return 0b0010; // 0010 south
                case WEST:
                    return 0b0001; // 0001 west
            }
            return 0;
        }

        private Direction toRelative(Direction direction) {
            if (sideDir.getAxis() != Direction.Axis.Y) { // On a wall
                switch (direction) {
                    case UP:
                        return Direction.NORTH;
                    case DOWN:
                        return Direction.SOUTH;

                    default:
                        { // Else
                            switch (sideDir) {
                                case NORTH:
                                    return direction;
                                case WEST:
                                    return direction.getClockWise();
                                case SOUTH:
                                    return direction.getOpposite();
                                case EAST:
                                    return direction.getCounterClockWise();
                            }
                        }
                }
            }
            return direction;
        }

        public boolean hasConnection(Direction direction) {
            return Integer.bitCount(getValue() & valueByDirection(toRelative(direction))) != 0 && getValue() != 0xF;
        }

        public HashMap<Direction, Boolean> getConnections() {
            HashMap<Direction, Boolean> connections = new HashMap<>();
            for (Direction direction : Direction.values()) {
                if (direction.getAxis() == Direction.Axis.Y) {
                    continue;
                }
                connections.put(toRelative(direction), hasConnection(direction));
            }
            return connections;
        }

        public void addConnection(Direction direction) {
            int newValue = this.value | valueByDirection(toRelative(direction));
            this.setValue((byte) newValue);
        }

        public void removeConnection(Direction direction) {
            int newValue = this.value & ~valueByDirection(toRelative(direction));
            this.setValue((byte) newValue);
        }

        public enum SideType {
            NORMAL,
            NONE,
            CORNER
        }

        public SideType getSideType() {
            if (getValue() == 0xF) {
                return SideType.NONE;
            } // Empty side
            if ((getValue() == 0b0101) || (getValue() == 0b1010)) {
                return SideType.NORMAL;
            } // Straight lines
            return SideType.CORNER; // Intersection side
        }

        public byte getValue() {
            return (byte) (value & 0xF);
        }

        public byte getActualValue() {
            return value;
        }

        public boolean isActive() {
            return (value & 0b10000) != 0;
        }

        public void setActive(boolean active) { // Sets the 5th LSB
            this.setActualValue((byte) (active ? (value | 0x10) : (value & 0x0F)));
        }

        public int countConnections() { // Count how many connections a side has
            if (getValue() == 0xF) {
                return 0;
            }
            return Integer.bitCount(getValue());
        }

        public Direction toDirection() {
            return this.sideDir;
        }

        public void rotate(int times) {
            times = Math.floorMod(times, 4);
            byte value = this.getValue();
            this.setValue((byte) (value >> times | value << 4 - times));
        }

        public void mirror(boolean flipEastWest) {
            if (flipEastWest) {
                int east = this.getValue() & valueByDirection(Direction.EAST);
                int west = this.getValue() & valueByDirection(Direction.WEST);
                this.setValue((byte) (this.getValue() & ~east & ~west | east >> 2 | west << 2));
            } else {
                int north = this.getValue() & valueByDirection(Direction.NORTH);
                int south = this.getValue() & valueByDirection(Direction.SOUTH);
                this.setValue((byte) (this.getValue() & ~north & ~south | north >> 2 | south << 2));
            }
        }
    }
}
