package net.portalmod.mixins.entity;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.portalmod.common.sorted.portal.IClientTeleportable;
import net.portalmod.common.sorted.portal.PortalMovementAccounting;
import net.portalmod.common.sorted.portalgun.PortalGun;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
    @Redirect(
            method = "checkMovementStatistics",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;causeFoodExhaustion(F)V"))
    private void pmSuppressPortalMovementExhaustion(ServerPlayer player, float exhaustion) {
        if (!PortalMovementAccounting.shouldSuppressMovementExhaustion(
                ((IClientTeleportable) player).getJustPortaled(), exhaustion)) {
            player.causeFoodExhaustion(exhaustion);
        }
    }

    @Inject(
            method =
                    "drop(Lnet/minecraft/world/item/ItemStack;ZLnet/minecraft/util/Prediction;)Lnet/minecraft/world/entity/item/ItemEntity;",
            at = @At("HEAD"))
    private void pmDropPortalGun(
            ItemStack stack,
            boolean throwRandomly,
            Prediction prediction,
            CallbackInfoReturnable<ItemEntity> callback) {
        if (stack.getItem() instanceof PortalGun) {
            PortalGun.dropCube((ServerPlayer) (Object) this, stack);
        }
    }
}
