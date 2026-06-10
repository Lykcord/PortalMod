package net.portalmod.fabric.item;

import net.minecraft.core.BlockPos;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.portalmod.fabric.component.PortalGunState;
import net.portalmod.fabric.network.PortalGunEventPayload;
import net.portalmod.fabric.portal.PortalGunGrab;
import net.portalmod.fabric.portal.PortalPlacementService;
import net.portalmod.fabric.registry.PortalModDataComponents;
import net.portalmod.fabric.registry.PortalModSounds;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public final class PortalGunItem extends Item {
    public PortalGunItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        // No firing while carrying a prop; the interact key drops it, left click throws it.
        if (PortalGunGrab.isHolding(player)) {
            return InteractionResult.CONSUME;
        }

        // Right click fires the secondary (orange) portal; sneaking inverts.
        fire(level, player, hand, player.isShiftKeyDown());

        return InteractionResult.SUCCESS;
    }

    public void fire(Level level, Player player, InteractionHand hand, boolean primary) {
        ItemStack stack = player.getItemInHand(hand);
        ensureGunIdentity(stack, false);
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
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        ensureGunIdentity(stack, false);

        if (entity instanceof Player player) {
            updateHolding(stack, player);
        }
    }

    /**
     * The gun tracks whether it is carrying something on its own (via the component flag)
     * instead of having every grab path trigger the pick/drop feedback separately.
     */
    public static void updateHolding(ItemStack stack, Player player) {
        boolean isInMainHand = player.getItemInHand(InteractionHand.MAIN_HAND) == stack;
        boolean isInOffHand = player.getItemInHand(InteractionHand.OFF_HAND) == stack;

        // Only one gun carries at a time; main hand wins.
        boolean isInHand = isInMainHand
                || (isInOffHand && !(player.getMainHandItem().getItem() instanceof PortalGunItem));

        boolean wasHolding = stack.getOrDefault(PortalModDataComponents.PORTAL_GUN_STATE, PortalGunState.DEFAULT).holding();
        boolean hasPassenger = PortalGunGrab.isHolding(player);
        boolean isHolding = isInHand && hasPassenger;

        if (isHolding && !wasHolding) {
            onPickCube(player, stack);
        }

        if (!isHolding && wasHolding) {
            if (hasPassenger) {
                // The gun left the hand while still carrying: release the prop too.
                PortalGunGrab.dropHeldEntities(player, false, false, stack);
            } else {
                onDropCube(player, stack);
            }
        }
    }

    public static void onPickCube(Player player, ItemStack gun) {
        setHolding(gun, true);

        // Sounds and the hold-loop trigger come from the server only, so client-side
        // prediction (instant dismount) never doubles the feedback.
        if (!player.level().isClientSide()) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    PortalModSounds.PORTALGUN_LIFT, SoundSource.PLAYERS, 1.0F, randomSoundPitch(player));
        }

        if (player instanceof ServerPlayer serverPlayer) {
            ServerPlayNetworking.send(serverPlayer,
                    new PortalGunEventPayload(PortalGunEventPayload.Event.LIFT, player.position()));
        }
    }

    public static void onDropCube(Player player, ItemStack gun) {
        setHolding(gun, false);

        if (!player.level().isClientSide()) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    PortalModSounds.PORTALGUN_DROP, SoundSource.PLAYERS, 1.0F, randomSoundPitch(player));
        }

        if (player instanceof ServerPlayer serverPlayer) {
            ServerPlayNetworking.send(serverPlayer,
                    new PortalGunEventPayload(PortalGunEventPayload.Event.DROP, player.position()));
        }
    }

    private static void setHolding(ItemStack stack, boolean holding) {
        stack.update(PortalModDataComponents.PORTAL_GUN_STATE, PortalGunState.DEFAULT,
                state -> state.withHolding(holding));
    }

    private static float randomSoundPitch(Player player) {
        return 0.9F + player.getRandom().nextFloat() * 0.2F;
    }

    @Override
    public void onCraftedBy(ItemStack stack, Player player) {
        ensureGunIdentity(stack, true);
    }

    @Override
    public void onCraftedPostProcess(ItemStack stack, Level level) {
        ensureGunIdentity(stack, true);
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
        tooltip.accept(Component.translatable("tooltip.portalmod.colors." + state.primaryHue())
                .withStyle(style -> style.withColor(state.primaryColor())));
        tooltip.accept(Component.translatable("tooltip.portalmod.colors." + state.secondaryHue())
                .withStyle(style -> style.withColor(state.secondaryColor())));

        if (state.singlePortal()) {
            tooltip.accept(Component.translatable("tooltip.portalmod.portalgun.single").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private static void ensureGunIdentity(ItemStack stack, boolean forceNew) {
        PortalGunState state = stack.getOrDefault(PortalModDataComponents.PORTAL_GUN_STATE, PortalGunState.DEFAULT);
        if (!forceNew && state.gunUuid().isPresent()) {
            return;
        }

        stack.set(PortalModDataComponents.PORTAL_GUN_STATE, new PortalGunState(
                Optional.of(UUID.randomUUID()),
                state.primaryHue(),
                state.secondaryHue(),
                state.accentHue(),
                state.singlePortal(),
                state.skin(),
                PortalGunState.NONE,
                false,
                Optional.empty(),
                Optional.empty()
        ));
    }
}
