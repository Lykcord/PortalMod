package net.portalmod.common.sorted.sign;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.portalmod.core.util.ModUtil;

import java.util.function.Consumer;

public class ChamberSignItem extends Item {

    public ChamberSignItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        BlockPos blockpos = context.getClickedPos();
        Direction direction = context.getClickedFace();
        BlockPos blockpos1 = blockpos.relative(direction);
        Player playerentity = context.getPlayer();
        ItemStack itemstack = context.getItemInHand();

        if (playerentity != null && !this.mayPlace(playerentity, direction, itemstack, blockpos1)) {
            return InteractionResult.PASS;
        }

        Level world = context.getLevel();

        boolean clickedBottomHalf = (context.getClickLocation().y % 1) < 0.25;
        boolean clickedTopHalf = (context.getClickLocation().y % 1) > 0.75;

        BlockPos placePos = clickedBottomHalf ? blockpos1.below() : blockpos1;
        ChamberSignEntity chamberSign =
                new ChamberSignEntity(world, placePos, direction, !clickedBottomHalf && !clickedTopHalf);

        var entityData = itemstack.get(DataComponents.ENTITY_DATA);
        if (entityData != null) {
            EntityType.updateCustomEntityTag(world, playerentity, chamberSign, entityData);
        }

        if (chamberSign.survives()) {
            if (!world.isClientSide()) {
                chamberSign.playPlacementSound();
                world.addFreshEntity(chamberSign);
            }

            itemstack.consume(1, playerentity);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    protected boolean mayPlace(Player player, Direction direction, ItemStack itemStack, BlockPos pos) {
        return !direction.getAxis().isVertical() && player.mayUseItemAt(pos, direction, itemStack);
    }

    @Override
    public void appendHoverText(
            ItemStack itemStack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> list,
            TooltipFlag flag) {
        ModUtil.addTooltip("chamber_sign", list);
    }
}
