package net.portalmod.common.items;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.DispenserBlock;
import net.portalmod.core.init.ItemInit;
import net.portalmod.core.util.ModUtil;
import net.portalmod.core.util.Registry.Entry;

import java.util.function.Consumer;

public class ModSpawnEggItem extends SpawnEggItem {
    public final String tooltipName;

    public ModSpawnEggItem(Entry<? extends EntityType<?>> entity, Properties properties, String tooltipName) {
        super(properties.spawnEgg(entity.get()));
        this.tooltipName = tooltipName;
    }

    public static void register() {
        ItemInit.ITEMS
                .getRegistry()
                .values()
                .forEach(
                        entry -> {
                            Item item = entry.get();
                            if (item instanceof ModSpawnEggItem) {
                                DispenserBlock.registerBehavior(
                                        item, DispenserBlock.DISPENSER_REGISTRY.get(Items.PIG_SPAWN_EGG));
                            }
                        });
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> tooltip,
            TooltipFlag flag) {
        if (tooltipName != null) {
            ModUtil.addTooltip(tooltipName, tooltip);
        }
    }
}
