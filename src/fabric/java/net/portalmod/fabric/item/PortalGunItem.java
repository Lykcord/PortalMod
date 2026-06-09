package net.portalmod.fabric.item;

import net.minecraft.core.BlockPos;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.portalmod.fabric.component.PortalGunState;
import net.portalmod.fabric.portal.PortalPlacementService;
import net.portalmod.fabric.registry.PortalModDataComponents;
import net.portalmod.fabric.registry.PortalModSounds;

import java.util.function.Consumer;

public final class PortalGunItem extends Item {
    public PortalGunItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        fire(level, player, hand, !player.isShiftKeyDown());

        return InteractionResult.SUCCESS;
    }

    public void fire(Level level, Player player, InteractionHand hand, boolean primary) {
        ItemStack stack = player.getItemInHand(hand);
        String side = primary ? PortalGunState.PRIMARY : PortalGunState.SECONDARY;

        if (!level.isClientSide()) {
            PortalPlacementService.tryPlaceFromGun(level, player, stack, primary);
        } else {
            stack.update(PortalModDataComponents.PORTAL_GUN_STATE, PortalGunState.DEFAULT, state -> state.withLastShot(side));
        }

        level.playSound(
                player,
                player.getX(),
                player.getY(),
                player.getZ(),
                PortalGunState.PRIMARY.equals(side) ? PortalModSounds.PORTALGUN_FIRE_PRIMARY : PortalModSounds.PORTALGUN_FIRE_SECONDARY,
                SoundSource.PLAYERS,
                0.8F,
                1.0F
        );
    }

    @Override
    public boolean canDestroyBlock(ItemStack stack, BlockState state, Level level, BlockPos pos, LivingEntity entity) {
        return false;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay tooltipDisplay,
            Consumer<Component> tooltip,
            TooltipFlag flag
    ) {
        PortalGunState state = stack.getOrDefault(PortalModDataComponents.PORTAL_GUN_STATE, PortalGunState.DEFAULT);

        tooltip.accept(Component.translatable("tooltip.portalmod.portalgun_1").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.portalmod.portalgun.colors").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.literal("Primary: #" + hexColor(state.primaryColor())).withStyle(ChatFormatting.BLUE));
        tooltip.accept(Component.literal("Secondary: #" + hexColor(state.secondaryColor())).withStyle(ChatFormatting.GOLD));

        if (state.singlePortal()) {
            tooltip.accept(Component.translatable("tooltip.portalmod.portalgun.single").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private static String hexColor(int color) {
        return String.format("%06X", color & 0xFFFFFF);
    }
}
