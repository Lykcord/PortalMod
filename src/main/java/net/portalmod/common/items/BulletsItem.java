package net.portalmod.common.items;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.portalmod.core.util.ModUtil;

import java.util.function.Consumer;

public class BulletsItem extends Item {

    public BulletsItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(
            ItemStack itemStack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> list,
            TooltipFlag flag) {
        ModUtil.addTooltip("bullets", list);
    }
}
