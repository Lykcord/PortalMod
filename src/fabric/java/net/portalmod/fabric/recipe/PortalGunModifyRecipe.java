package net.portalmod.fabric.recipe;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.portalmod.fabric.component.PortalGunState;
import net.portalmod.fabric.item.PortalGunItem;
import net.portalmod.fabric.registry.PortalModDataComponents;
import org.jspecify.annotations.Nullable;

/**
 * Recolors a portal gun in the crafting grid. Layout (translation-invariant, matching the
 * Forge 3x3 layout): gun in the middle, an optional dye above it for the accent color, an
 * optional dye left/right of it for the primary/secondary portal color. A chain in the
 * left or right slot locks that side, turning the gun into a single-portal gun.
 */
public class PortalGunModifyRecipe extends CustomRecipe {
    public static final PortalGunModifyRecipe INSTANCE = new PortalGunModifyRecipe();
    public static final MapCodec<PortalGunModifyRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, PortalGunModifyRecipe> STREAM_CODEC = StreamCodec.unit(INSTANCE);
    public static final RecipeSerializer<PortalGunModifyRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return layout(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        Layout layout = layout(input);
        if (layout == null) {
            return ItemStack.EMPTY;
        }

        ItemStack newGun = layout.gun().copy();
        PortalGunState state = newGun.getOrDefault(PortalModDataComponents.PORTAL_GUN_STATE, PortalGunState.DEFAULT);

        String primaryHue = hueOf(layout.left(), state.primaryHue());
        String secondaryHue = hueOf(layout.right(), state.secondaryHue());
        String accentHue = hueOf(layout.accent(), state.accentHue());

        boolean singlePortal = layout.left().is(Items.IRON_CHAIN) || layout.right().is(Items.IRON_CHAIN);

        state = state.withHues(primaryHue, secondaryHue, accentHue);
        state = new PortalGunState(
                state.gunUuid(),
                state.primaryHue(),
                state.secondaryHue(),
                state.accentHue(),
                singlePortal,
                state.skin(),
                PortalGunState.NONE,
                false,
                state.primaryTarget(),
                state.secondaryTarget()
        );
        newGun.set(PortalModDataComponents.PORTAL_GUN_STATE, state);
        return newGun;
    }

    /**
     * Resolves the grid layout relative to the gun, or null when it does not match.
     */
    @Nullable
    private static Layout layout(CraftingInput input) {
        int gunX = -1;
        int gunY = -1;

        for (int y = 0; y < input.height(); y++) {
            for (int x = 0; x < input.width(); x++) {
                if (input.getItem(x, y).getItem() instanceof PortalGunItem) {
                    if (gunX >= 0) {
                        return null;
                    }
                    gunX = x;
                    gunY = y;
                }
            }
        }

        if (gunX < 0) {
            return null;
        }

        ItemStack accent = itemAt(input, gunX, gunY - 1);
        ItemStack left = itemAt(input, gunX - 1, gunY);
        ItemStack right = itemAt(input, gunX + 1, gunY);

        boolean hasModifier = false;
        boolean hasChain = false;

        for (int y = 0; y < input.height(); y++) {
            for (int x = 0; x < input.width(); x++) {
                ItemStack stack = input.getItem(x, y);
                if (stack.isEmpty() || (x == gunX && y == gunY)) {
                    continue;
                }

                boolean isAccentSlot = x == gunX && y == gunY - 1;
                boolean isSideSlot = y == gunY && Math.abs(x - gunX) == 1;
                if (!isAccentSlot && !isSideSlot) {
                    return null;
                }

                hasModifier = true;

                if (stack.getItem() instanceof DyeItem) {
                    continue;
                }
                if (isSideSlot && stack.is(Items.IRON_CHAIN) && !hasChain) {
                    hasChain = true;
                    continue;
                }

                return null;
            }
        }

        return hasModifier ? new Layout(input.getItem(gunX, gunY), accent, left, right) : null;
    }

    private static String hueOf(ItemStack stack, String fallback) {
        if (!(stack.getItem() instanceof DyeItem)) {
            return fallback;
        }

        DyeColor color = stack.get(DataComponents.DYE);
        return color == null ? fallback : color.getName();
    }

    private static ItemStack itemAt(CraftingInput input, int x, int y) {
        if (x < 0 || y < 0 || x >= input.width() || y >= input.height()) {
            return ItemStack.EMPTY;
        }
        return input.getItem(x, y);
    }

    @Override
    public RecipeSerializer<PortalGunModifyRecipe> getSerializer() {
        return SERIALIZER;
    }

    private record Layout(ItemStack gun, ItemStack accent, ItemStack left, ItemStack right) {
    }
}
