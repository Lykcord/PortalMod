package net.portalmod.common.sorted.portalgun;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.portalmod.core.init.RecipeInit;

public class PortalGunDuplicateRecipe extends CustomRecipe {
    @Override
    public boolean matches(CraftingInput inventory, Level world) {
        int guns = 0;

        for (int i = 0; i < inventory.size(); i++) {
            if (inventory.getItem(i).getItem() instanceof PortalGun) {
                guns++;
            } else if (!inventory.getItem(i).isEmpty()) {
                return false;
            }
        }

        return guns == 2;
    }

    @Override
    public ItemStack assemble(CraftingInput inventory) {
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack item = inventory.getItem(i);
            if (!item.isEmpty()) {
                return item.copyWithCount(1);
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput inventory) {
        NonNullList<ItemStack> nonnulllist = NonNullList.withSize(inventory.size(), ItemStack.EMPTY);

        for (int i = 0; i < inventory.size(); i++) {
            ItemStack item = inventory.getItem(i);
            if (!item.isEmpty()) {
                nonnulllist.set(i, item.copyWithCount(1));
                return nonnulllist;
            }
        }
        return nonnulllist;
    }

    @Override
    public RecipeSerializer<PortalGunDuplicateRecipe> getSerializer() {
        return RecipeInit.PORTAL_GUN_DUPLICATE.get();
    }
}
