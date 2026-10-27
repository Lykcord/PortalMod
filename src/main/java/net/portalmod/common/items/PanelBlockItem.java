package net.portalmod.common.items;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import net.portalmod.core.util.ModUtil;

import java.util.function.Consumer;

public class PanelBlockItem extends BlockItem {
    public PanelBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(
            ItemStack itemStack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> list,
            TooltipFlag flag) {
        ModUtil.addTooltip("panel", list);
    }
}
