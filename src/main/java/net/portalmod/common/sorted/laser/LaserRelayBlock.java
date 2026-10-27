package net.portalmod.common.sorted.laser;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.portalmod.core.math.VoxelShapeBuilder;

public class LaserRelayBlock extends Block {
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    private static final VoxelShape SHAPE =
            new VoxelShapeBuilder().add(0, 0, 0, 16, 1.9, 16).add(3, 0, 3, 13, 14, 13).build();

    public LaserRelayBlock(Properties properties) {
        super(properties.pushReaction(PushReaction.POPPED));
        this.registerDefaultState(stateDefinition.any().setValue(ACTIVE, false));
    }

    //        SHAPE = new VoxelShapeBuilder()
    //                .add(0, 0, 0, 16, 1.9, 16)
    //                .add(3, 0, 3, 13, 14, 13)

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter blockReader, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
