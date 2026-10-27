package net.portalmod.common.sorted.longfallboots;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.portalmod.core.util.ModUtil;

import java.util.function.Consumer;

public class LongFallBoots extends Item {
    public LongFallBoots(ArmorMaterial armorMaterial, Properties properties) {
        super(properties.humanoidArmor(armorMaterial, ArmorType.BOOTS));
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> tooltip,
            TooltipFlag flag) {
        ModUtil.addTooltip("longfall_boots", tooltip);
    }
}
