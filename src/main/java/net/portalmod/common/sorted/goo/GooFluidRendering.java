package net.portalmod.common.sorted.goo;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderingRegistry;
import net.minecraft.client.color.block.BlockTintSources;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.portalmod.PortalMod;
import net.portalmod.core.init.FluidInit;

@Environment(EnvType.CLIENT)
public final class GooFluidRendering {
    private GooFluidRendering() {}

    public static void init() {
        FluidRenderingRegistry.register(
                FluidInit.GOO_FLUID.get(),
                FluidInit.GOO_FLOWING.get(),
                new FluidModel.Unbaked(
                        new Material(Identifier.fromNamespaceAndPath(PortalMod.MODID, "block/goo_still")),
                        new Material(Identifier.fromNamespaceAndPath(PortalMod.MODID, "block/goo_flow")),
                        null,
                        BlockTintSources.constant(-1)));
    }
}
