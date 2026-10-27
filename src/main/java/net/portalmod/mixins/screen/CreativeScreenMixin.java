package net.portalmod.mixins.screen;

import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.ItemStack;
import net.portalmod.common.sorted.portalgun.PortalGun;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeScreenMixin {

    @Redirect(
            method = "slotClicked",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/item/ItemStack;copyWithCount(I)Lnet/minecraft/world/item/ItemStack;",
                            ordinal = 1))
    public ItemStack pmResetGunUUIDOnCopy(ItemStack instance, int count) {
        ItemStack itemStack = instance.copyWithCount(count);
        if (itemStack.getItem() instanceof PortalGun) {
            PortalGun.onDuplicate(itemStack);
        }
        return itemStack;
    }
}
