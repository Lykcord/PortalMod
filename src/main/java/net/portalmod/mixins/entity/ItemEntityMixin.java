package net.portalmod.mixins.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.portalmod.common.entities.Fizzleable;
import net.portalmod.common.sorted.portalgun.PortalGun;
import net.portalmod.core.event.CommonEvents;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin extends Entity implements Fizzleable {
    @Shadow
    public abstract ItemStack getItem();

    public ItemEntityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Unique private ItemStack pmStackBeforePickup = ItemStack.EMPTY;

    @Inject(method = "playerTouch", at = @At("HEAD"))
    private void pmBeforePickup(Player player, CallbackInfo ci) {
        pmStackBeforePickup = this.getItem().copy();
    }

    @Inject(method = "playerTouch", at = @At("RETURN"))
    private void pmAfterPickup(Player player, CallbackInfo ci) {
        if (this.getItem().getCount() < pmStackBeforePickup.getCount()) {
            CommonEvents.onPlayerPickUpItem(player, (ItemEntity) (Object) this, pmStackBeforePickup);
        }
        pmStackBeforePickup = ItemStack.EMPTY;
    }

    @Override
    public boolean shouldCheckForFizzlers() {
        return Fizzleable.isFizzleableItem(this.getItem());
    }

    @Override
    public void onTouchingFizzler() {
        PortalGun.fizzleGunItem(this.getItem());
    }
}
