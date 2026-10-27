package net.portalmod.common.sorted.portalgun;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.portalmod.core.init.RecipeInit;

public class PortalGunModifyRecipe extends CustomRecipe {
    private static int findGun(CraftingInput input) {
        for (int i = 0; i < input.size(); i++) {
            if (input.getItem(i).getItem() instanceof PortalGun) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        int gun = findGun(input);
        if (gun < 0 || input.ingredientCount() < 2) {
            return false;
        }
        boolean hasChain = false;
        int gunX = gun % input.width();
        int gunY = gun / input.width();
        // CraftingInput trims empty outer rows and columns; positions are relative to the gun.
        for (int i = 0; i < input.size(); i++) {
            ItemStack item = input.getItem(i);
            if (item.isEmpty() || i == gun) {
                continue;
            }
            int dx = i % input.width() - gunX;
            int dy = i / input.width() - gunY;
            boolean above = dx == 0 && dy == -1;
            boolean beside = Math.abs(dx) == 1 && dy == 0;
            if (!(above || beside)) {
                return false;
            }
            if (item.has(DataComponents.DYE)) {
                continue;
            }
            if (beside && item.is(Items.IRON_CHAIN) && !hasChain) {
                hasChain = true;
                continue;
            }
            return false;
        }
        return true;
    }

    private static ItemStack adjacent(CraftingInput input, int gun, int dx, int dy) {
        int x = gun % input.width() + dx;
        int y = gun / input.width() + dy;
        return x >= 0 && x < input.width() && y >= 0 && y < input.height() ? input.getItem(x, y) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        int gun = findGun(input);
        if (gun < 0) {
            return ItemStack.EMPTY;
        }
        ItemStack result = input.getItem(gun).copyWithCount(1);
        ItemStack accent = adjacent(input, gun, 0, -1);
        ItemStack left = adjacent(input, gun, -1, 0);
        ItemStack right = adjacent(input, gun, 1, 0);
        CustomData.update(
                DataComponents.CUSTOM_DATA,
                result,
                tag -> {
                    if (accent.has(DataComponents.DYE)) {
                        tag.putString("AccentColor", accent.get(DataComponents.DYE).getName());
                    }
                    if (left.has(DataComponents.DYE)) {
                        tag.putString("LeftColor", left.get(DataComponents.DYE).getName());
                    }
                    if (right.has(DataComponents.DYE)) {
                        tag.putString("RightColor", right.get(DataComponents.DYE).getName());
                    }
                    tag.putString(
                            "Locked",
                            left.is(Items.IRON_CHAIN) ? "Left" : right.is(Items.IRON_CHAIN) ? "Right" : "None");
                    tag.putInt("LastPortal", 0);
                });
        return result;
    }

    @Override
    public RecipeSerializer<PortalGunModifyRecipe> getSerializer() {
        return RecipeInit.PORTAL_GUN_MODIFY.get();
    }
}
