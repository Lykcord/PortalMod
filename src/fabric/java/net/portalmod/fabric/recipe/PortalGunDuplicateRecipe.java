package net.portalmod.fabric.recipe;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.portalmod.fabric.item.PortalGunItem;

/**
 * Two portal guns anywhere in the grid produce a copy of the first one. The crafted
 * result rolls a fresh gun UUID via {@link PortalGunItem#onCraftedBy}, so the copy
 * controls its own portal pair; the original stays in the grid as a remainder.
 */
public class PortalGunDuplicateRecipe extends CustomRecipe {
    public static final PortalGunDuplicateRecipe INSTANCE = new PortalGunDuplicateRecipe();
    public static final MapCodec<PortalGunDuplicateRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, PortalGunDuplicateRecipe> STREAM_CODEC = StreamCodec.unit(INSTANCE);
    public static final RecipeSerializer<PortalGunDuplicateRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (input.ingredientCount() != 2) {
            return false;
        }

        int guns = 0;
        for (int i = 0; i < input.size(); i++) {
            if (input.getItem(i).getItem() instanceof PortalGunItem) {
                guns++;
            }
        }

        return guns == 2;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty()) {
                return stack.copy();
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty()) {
                remaining.set(i, stack.copy());
                return remaining;
            }
        }
        return remaining;
    }

    @Override
    public RecipeSerializer<PortalGunDuplicateRecipe> getSerializer() {
        return SERIALIZER;
    }
}
