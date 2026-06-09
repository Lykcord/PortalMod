package net.portalmod.fabric.block;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class FrameBlock extends Block {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public static final BooleanProperty BEAM = BooleanProperty.create("beam");
    private static final VoxelShape Z_FRAME = Shapes.or(
            box(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 2.0D),
            box(0.0D, 14.0D, 0.0D, 16.0D, 16.0D, 2.0D),
            box(0.0D, 0.0D, 0.0D, 2.0D, 16.0D, 2.0D),
            box(14.0D, 0.0D, 0.0D, 16.0D, 16.0D, 2.0D)
    );
    private static final VoxelShape X_FRAME = Shapes.or(
            box(0.0D, 0.0D, 0.0D, 2.0D, 2.0D, 16.0D),
            box(0.0D, 14.0D, 0.0D, 2.0D, 16.0D, 16.0D),
            box(0.0D, 0.0D, 0.0D, 2.0D, 16.0D, 2.0D),
            box(0.0D, 0.0D, 14.0D, 2.0D, 16.0D, 16.0D)
    );
    private static final VoxelShape Y_FRAME = Shapes.or(
            box(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 2.0D),
            box(0.0D, 0.0D, 14.0D, 16.0D, 2.0D, 16.0D),
            box(0.0D, 0.0D, 0.0D, 2.0D, 2.0D, 16.0D),
            box(14.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D)
    );

    public FrameBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.UP)
                .setValue(BEAM, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, BEAM);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Player player = context.getPlayer();
        Direction direction = context.getNearestLookingDirection();
        return defaultBlockState().setValue(FACING, player != null && player.isShiftKeyDown() ? direction.getOpposite() : direction);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, net.minecraft.core.BlockPos pos, CollisionContext context) {
        return shapeFor(state.getValue(FACING));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, net.minecraft.core.BlockPos pos, CollisionContext context) {
        return shapeFor(state.getValue(FACING));
    }

    private static VoxelShape shapeFor(Direction direction) {
        return switch (direction.getAxis()) {
            case X -> X_FRAME;
            case Y -> Y_FRAME;
            case Z -> Z_FRAME;
        };
    }
}
