package net.portalmod.core;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.portalmod.PortalMod;
import net.portalmod.common.sorted.portalgun.PortalGun;
import net.portalmod.core.init.ItemInit;

public final class PortalModTab {
    private PortalModTab() {}

    public static void register() {
        Registry.register(
                BuiltInRegistries.CREATIVE_MODE_TAB,
                Identifier.fromNamespaceAndPath(PortalMod.MODID, "main"),
                FabricCreativeModeTab.builder()
                        .title(Component.translatable("itemGroup.portalmod"))
                        .icon(() -> new ItemStack(ItemInit.RADIO.get()))
                        .displayItems(
                                (parameters, output) ->
                                        ItemInit.ITEMS
                                                .getRegistry()
                                                .values()
                                                .forEach(
                                                        entry -> {
                                                            var item = entry.get();
                                                            output.accept(item);
                                                            if (item instanceof PortalGun) {
                                                                output.accept(
                                                                        PortalGun.modifyColors(
                                                                                new ItemStack(item),
                                                                                "light_blue",
                                                                                "purple",
                                                                                "light_blue"));
                                                                output.accept(
                                                                        PortalGun.modifyColors(
                                                                                new ItemStack(item),
                                                                                "yellow",
                                                                                "red",
                                                                                "orange"));
                                                            }
                                                        }))
                        .build());
    }
}
