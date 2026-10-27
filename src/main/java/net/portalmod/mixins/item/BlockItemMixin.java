package net.portalmod.mixins.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.portalmod.common.sorted.gel.container.GelContainer;
import net.portalmod.core.init.SoundInit;
import net.portalmod.core.util.ModUtil;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BlockItem.class)
public abstract class BlockItemMixin extends Item {
    public BlockItemMixin(Properties properties) {
        super(properties);
    }

    @Redirect(
            method = "place",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/item/ItemStack;consume(ILnet/minecraft/world/entity/LivingEntity;)V"))
    public void consumeGel(ItemStack instance, int amount, LivingEntity entity) {
        Item item = instance.getItem();
        if (item instanceof GelContainer) {
            if (entity == null || !entity.hasInfiniteMaterials()) {
                GelContainer.setAmount(instance, GelContainer.getAmount(instance) - amount);
            }
        } else {
            instance.consume(amount, entity);
        }
    }

    @Redirect(
            method = "place",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/SoundType;getPitch()F"))
    private float redirectPitch(SoundType instance) {
        if (instance.getPlaceSound() == SoundInit.GEL_PLACE.get()) {
            return ModUtil.randomSlightSoundPitch();
        }

        return instance.getPitch();
    }
}
