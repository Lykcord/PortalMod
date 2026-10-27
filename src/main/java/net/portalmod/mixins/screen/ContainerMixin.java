package net.portalmod.mixins.screen;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.portalmod.common.sorted.portalgun.PortalGun;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractContainerMenu.class)
public abstract class ContainerMixin {

    @Redirect(
            method = "doClick",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/inventory/Slot;safeClone(Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/item/ItemStack;"))
    public ItemStack pmResetGunUUIDOnCopy(Slot slot, Player player) {
        ItemStack itemStack = slot.safeClone(player);
        if (itemStack.getItem() instanceof PortalGun) {
            PortalGun.onDuplicate(itemStack);
        }
        return itemStack;
    }
}
