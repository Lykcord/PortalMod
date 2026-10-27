package net.portalmod.common.sorted.gel.container;

import static org.junit.Assert.*;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.portalmod.TestItems;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class GelContainerDataTest {
    @BeforeAll
    public static void bootstrap() {
        TestItems.bootstrap();
    }

    @Test
    public void amountIsBoundedAndPreservesOtherCustomData() {
        ItemStack stack = new ItemStack(Items.BUCKET);
        assertEquals(16, GelContainer.getAmount(stack));
        assertFalse(stack.has(DataComponents.CUSTOM_DATA));
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString("custom", "keep"));
        GelContainer.setAmount(stack, -1);
        assertEquals(0, GelContainer.getAmount(stack));
        GelContainer.decreaseAmount(stack);
        assertEquals(0, GelContainer.getAmount(stack));
        GelContainer.increaseAmount(stack);
        assertEquals(1, GelContainer.getAmount(stack));
        GelContainer.setAmount(stack, Integer.MAX_VALUE);
        assertEquals(16, GelContainer.getAmount(stack));
        GelContainer.increaseAmount(stack);
        assertEquals(16, GelContainer.getAmount(stack));
        assertEquals("keep", stack.get(DataComponents.CUSTOM_DATA).copyTag().getStringOr("custom", ""));
    }
}
