package net.portalmod.common.sorted.gel.container;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.portalmod.common.sorted.gel.AbstractGelBlock;
import net.portalmod.core.init.ItemInit;
import net.portalmod.core.init.SoundInit;
import net.portalmod.core.util.ModUtil;

import java.util.function.Consumer;

public class GelContainer extends BlockItem {
    private final int color;
    private static final int maxAmount = 16;

    public GelContainer(Block block, Properties properties, int color) {
        super(block, properties);
        this.color = color;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        ItemStack itemStack = context.getItemInHand();
        BlockPos clickedPos = context.getClickedPos();
        BlockState clickedState = context.getLevel().getBlockState(clickedPos);
        boolean creative = player.getAbilities().instabuild;
        Direction gelSide = context.getClickedFace().getOpposite();

        int gelAmount = getAmount(itemStack);

        BooleanProperty sideProperty = AbstractGelBlock.STATES.get(gelSide);
        boolean pickUpGel =
                clickedState.is(this.getBlock())
                        && clickedState.getValue(sideProperty)
                        && (gelAmount < maxAmount || creative);

        InteractionResult result;

        if (pickUpGel) {
            // Pick up block
            context.getLevel().setBlockAndUpdate(clickedPos, AbstractGelBlock.removeSide(gelSide, clickedState));
            context.getLevel()
                    .playSound(
                            player,
                            clickedPos,
                            SoundInit.GEL_COLLECT.get(),
                            SoundSource.BLOCKS,
                            1,
                            ModUtil.randomSlightSoundPitch());

            if (!creative) {
                increaseAmount(itemStack);
            }

            result = InteractionResult.SUCCESS;
        } else {
            // Place block
            result = super.useOn(context);
        }

        // Replace with empty container if empty
        if (!creative && getAmount(itemStack) <= 0) {
            player.setItemInHand(context.getHand(), createEmptyContainer(itemStack));
        }

        return result;
    }

    public static ItemStack createEmptyContainer(ItemStack stack) {
        ItemStack test = new ItemStack(ItemInit.CONTAINER.get());
        test.applyComponents(stack.getComponentsPatch());
        return test;
    }

    public static void increaseAmount(ItemStack stack) {
        int amount = getAmount(stack);
        if (amount < maxAmount) {
            setAmount(stack, amount + 1);
        }
    }

    public static void decreaseAmount(ItemStack stack) {
        int amount = getAmount(stack);
        if (amount > 0) {
            setAmount(stack, amount - 1);
        }
    }

    public static int getAmount(ItemStack stack) {
        CompoundTag nbt = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (nbt.contains("amount")) {
            return Math.clamp(nbt.getIntOr("amount", 0), 0, maxAmount);
        }
        return maxAmount;
    }

    public static void setAmount(ItemStack stack, int amount) {
        CustomData.update(
                DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt("amount", Math.clamp(amount, 0, maxAmount)));
    }

    @Override
    protected SoundEvent getPlaceSound(BlockState state) {
        return SoundInit.GEL_PLACE.get();
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getAmount(stack) < maxAmount;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13F * getAmount(stack) / maxAmount);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return color == 0 ? super.getBarColor(stack) : color;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> tooltip,
            TooltipFlag flag) {
        ModUtil.addTooltip(BuiltInRegistries.BLOCK.getKey(getBlock()).getPath(), tooltip);
    }
}
