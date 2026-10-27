package net.portalmod.common.sorted.portalgun;

import static org.junit.Assert.*;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.portalmod.TestItems;
import net.portalmod.core.init.ItemInit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

public class PortalGunRecipeTest {
    @BeforeAll
    public static void bootstrap() {
        TestItems.bootstrap();
    }

    private static ItemStack gun() {
        return PortalGun.modifyColors(new ItemStack(ItemInit.PORTALGUN.get()), "blue", "orange", "none");
    }

    @Test
    public void modifiersUsePositionsRelativeToGunInTrimmedGrid() {
        var recipe = new PortalGunModifyRecipe();
        var input =
                CraftingInput.of(
                        2,
                        2,
                        List.of(
                                ItemStack.EMPTY,
                                new ItemStack(Items.DYE.white()),
                                new ItemStack(Items.IRON_CHAIN),
                                gun()));
        assertTrue(recipe.matches(input, null));
        var data = recipe.assemble(input).get(DataComponents.CUSTOM_DATA).copyTag();
        assertEquals("white", data.getStringOr("AccentColor", ""));
        assertEquals("Left", data.getStringOr("Locked", ""));
        assertEquals("orange", data.getStringOr("RightColor", ""));
    }

    @Test
    public void chainCannotBeUsedAsAccentOrOnBothSides() {
        var recipe = new PortalGunModifyRecipe();
        assertFalse(recipe.matches(CraftingInput.of(1, 2, List.of(new ItemStack(Items.IRON_CHAIN), gun())), null));
        assertFalse(
                recipe.matches(
                        CraftingInput.of(
                                3, 1, List.of(new ItemStack(Items.IRON_CHAIN), gun(), new ItemStack(Items.IRON_CHAIN))),
                        null));
        assertFalse(recipe.matches(CraftingInput.of(1, 2, List.of(gun(), new ItemStack(Items.DYE.red()))), null));
    }

    @Test
    public void duplicateCopiesFirstGunAndLeavesOriginal() {
        var recipe = new PortalGunDuplicateRecipe();
        ItemStack original = gun();
        PortalGun.addUUID(original);
        var input = CraftingInput.of(2, 1, List.of(original, gun()));
        assertTrue(recipe.matches(input, null));
        ItemStack output = recipe.assemble(input);
        assertNotSame(original, output);
        assertTrue(ItemStack.isSameItemSameComponents(original, output));
        assertTrue(ItemStack.isSameItemSameComponents(original, recipe.getRemainingItems(input).get(0)));
        assertTrue(recipe.getRemainingItems(input).get(1).isEmpty());
        assertFalse(recipe.matches(CraftingInput.of(3, 1, List.of(original, gun(), new ItemStack(Items.DIRT))), null));
    }
}
