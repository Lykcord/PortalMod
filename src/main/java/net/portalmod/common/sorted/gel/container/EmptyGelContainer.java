package net.portalmod.common.sorted.gel.container;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.portalmod.common.sorted.gel.AbstractGelBlock;
import net.portalmod.core.init.SoundInit;
import net.portalmod.core.util.ModUtil;

public class EmptyGelContainer extends Item {
    public EmptyGelContainer(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        BlockPos clickedPos = context.getClickedPos();
        BlockState clickedState = context.getLevel().getBlockState(clickedPos);
        boolean creative = player.getAbilities().instabuild;
        Direction gelSide = context.getClickedFace().getOpposite();

        BooleanProperty sideProperty = AbstractGelBlock.STATES.get(gelSide);

        // Can't pick up any gel
        if (!(clickedState.getBlock() instanceof AbstractGelBlock) || !clickedState.getValue(sideProperty)) {
            return InteractionResult.PASS;
        }

        context.getLevel().setBlockAndUpdate(clickedPos, AbstractGelBlock.removeSide(gelSide, clickedState));
        context.getLevel()
                .playSound(
                        player,
                        clickedPos,
                        SoundInit.GEL_COLLECT.get(),
                        SoundSource.BLOCKS,
                        1,
                        ModUtil.randomSlightSoundPitch());

        // Fill container with gel
        if (!creative) {
            ItemStack newContainer = new ItemStack(clickedState.getBlock().asItem());
            newContainer.applyComponents(context.getItemInHand().getComponentsPatch());
            GelContainer.setAmount(newContainer, 1);
            player.setItemInHand(
                    context.getHand(), ItemUtils.createFilledResult(context.getItemInHand(), player, newContainer));
        }

        return InteractionResult.SUCCESS;
    }
}
