package net.portalmod.common.blocks;

import com.mojang.datafixers.util.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.portalmod.common.sorted.button.QuadBlockCorner;
import net.portalmod.core.util.ModUtil;

import org.jspecify.annotations.Nullable;

import java.util.*;

public class QuadBlock extends MultiBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public static final EnumProperty<QuadBlockCorner> CORNER = EnumProperty.create("corner", QuadBlockCorner.class);

    public QuadBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockPos getMainPosition(BlockState blockState, BlockPos pos) {
        QuadBlockCorner corner = blockState.getValue(CORNER);
        Direction facing = blockState.getValue(FACING);

        Direction horizontal =
                facing == Direction.UP
                        ? Direction.WEST
                        : facing == Direction.DOWN ? Direction.EAST : facing.getClockWise();
        Direction vertical = facing.getAxis() == Direction.Axis.Y ? Direction.NORTH : Direction.UP;

        if (!corner.isLeft()) {
            pos = pos.relative(horizontal);
        }
        if (!corner.isUp()) {
            pos = pos.relative(vertical);
        }
        return pos;
    }

    @Override
    public List<BlockPos> getConnectedPositions(BlockState mainState, BlockPos mainPos) {
        Direction facing = mainState.getValue(FACING);

        Direction horizontal =
                facing == Direction.UP
                        ? Direction.EAST
                        : facing == Direction.DOWN ? Direction.WEST : facing.getCounterClockWise();
        Direction vertical = facing.getAxis() == Direction.Axis.Y ? Direction.SOUTH : Direction.DOWN;

        return new ArrayList<>(
                Arrays.asList(
                        mainPos.relative(horizontal),
                        mainPos.relative(horizontal).relative(vertical),
                        mainPos.relative(vertical)));
    }

    @Override
    public Map<BlockPos, BlockState> getOtherParts(BlockState blockState, BlockPos pos) {
        QuadBlockCorner base = blockState.getValue(CORNER);
        Direction facing = blockState.getValue(FACING);

        Map<BlockPos, BlockState> map = new HashMap<>();

        for (QuadBlockCorner corner : QuadBlockCorner.values()) {
            if (corner == base) {
                continue;
            }
            map.put(getOtherBlock(pos, base, corner, facing), blockState.setValue(CORNER, corner));
        }

        return map;
    }

    @Override
    public boolean isSamePart(BlockState one, BlockState two) {
        return one.getValue(FACING) == two.getValue(FACING) && one.getValue(CORNER) == two.getValue(CORNER);
    }

    @Override
    public void addMainBlockProperties(Map<Property<?>, Comparable<?>> map) {
        map.put(CORNER, QuadBlockCorner.UP_LEFT);
    }

    public List<BlockPos> getAllBlocks(BlockPos pos, QuadBlockCorner base, Direction facing) {
        List<BlockPos> poses = new ArrayList<>();
        for (QuadBlockCorner corner : QuadBlockCorner.values()) {
            poses.add(getOtherBlock(pos, base, corner, facing));
        }
        return poses;
    }

    public BlockPos getOtherBlock(BlockPos pos, QuadBlockCorner base, QuadBlockCorner corner, Direction facing) {
        Pair<Direction, Direction> directions = placementDirectionsFromFacing(facing.getAxis());
        Direction a = directions.getFirst();
        Direction b = directions.getSecond();
        int x = corner.getX() - base.getX();
        int y = corner.getY() - base.getY();

        if (facing.getAxisDirection() == Direction.AxisDirection.NEGATIVE) {
            x *= -1;
        }

        BlockPos newPos = pos.immutable();
        if (x != 0) {
            newPos = newPos.relative(x < 0 ? a.getOpposite() : a);
        }
        if (y != 0) {
            newPos = newPos.relative(y < 0 ? b.getOpposite() : b);
        }

        return newPos;
    }

    public Pair<Direction, Direction> placementDirectionsFromFacing(Direction.Axis axis) {
        if (axis == Direction.Axis.X) {
            return Pair.of(Direction.NORTH, Direction.UP);
        }
        if (axis == Direction.Axis.Z) {
            return Pair.of(Direction.EAST, Direction.UP);
        }
        return Pair.of(Direction.EAST, Direction.NORTH);
    }

    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction direction = context.getClickedFace();
        Direction.Axis axis = direction.getAxis();
        boolean isPositive = direction.getAxisDirection() == Direction.AxisDirection.POSITIVE;

        // Determine directions of the corners for this placement face
        Direction upDirection = axis == Direction.Axis.Y ? Direction.NORTH : Direction.UP;
        Direction leftDirection =
                axis == Direction.Axis.Y ? (isPositive ? Direction.WEST : Direction.EAST) : direction.getClockWise();

        boolean prefersUp = clickedOnPositiveHalf(context, upDirection.getOpposite());
        boolean prefersLeft = clickedOnPositiveHalf(context, leftDirection.getOpposite());

        boolean[] flipUp = {false, false, true, true};
        boolean[] flipLeft = {false, true, false, true};

        for (int i = 0; i < 4; i++) {
            QuadBlockCorner corner = QuadBlockCorner.getCorner(prefersUp ^ flipUp[i], prefersLeft ^ flipLeft[i]);

            if (this.isCornerPlaceable(context, corner)) {
                return this.defaultBlockState().setValue(FACING, direction).setValue(CORNER, corner);
            }
        }

        return null;
    }

    public boolean isCornerPlaceable(BlockPlaceContext context, QuadBlockCorner corner) {
        return this.getAllBlocks(context.getClickedPos(), corner, context.getClickedFace()).stream()
                .allMatch(pos -> ModUtil.canPlaceAt(context, pos));
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        Direction facing = state.getValue(FACING);

        if (facing.getAxis() == Direction.Axis.Y) {
            int times = ModUtil.getRotationAmount(rotation);
            if (facing == Direction.DOWN) {
                times = 4 - times;
            }
            return state.setValue(CORNER, state.getValue(CORNER).rotate(times));
        }

        return state.setValue(FACING, rotation.rotate(facing));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        Direction facing = state.getValue(FACING);

        if (facing.getAxis() == Direction.Axis.Y) {
            switch (mirror) {
                case FRONT_BACK:
                    return state.setValue(CORNER, state.getValue(CORNER).mirrorLeftRight());
                case LEFT_RIGHT:
                    return state.setValue(CORNER, state.getValue(CORNER).mirrorUpDown());
            }
            return state;
        }

        if (mirror == Mirror.NONE) {
            return state;
        }

        BlockState sideFlipped = state.setValue(CORNER, state.getValue(CORNER).mirrorLeftRight());

        if (mirror.mirror(facing) == facing) {
            return sideFlipped;
        }

        return sideFlipped.setValue(FACING, facing.getOpposite());
    }
}
