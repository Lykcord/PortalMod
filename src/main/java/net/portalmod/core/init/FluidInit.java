package net.portalmod.core.init;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.portalmod.PortalMod;
import net.portalmod.common.sorted.goo.GooFluid;
import net.portalmod.core.util.Registry;
import net.portalmod.core.util.Registry.Entry;

public final class FluidInit {
    public static final Registry<Fluid> FLUIDS =
            new Registry<>(
                    (id, value) ->
                            net.minecraft.core.Registry.register(
                                    BuiltInRegistries.FLUID,
                                    Identifier.fromNamespaceAndPath(PortalMod.MODID, id),
                                    value));

    public static final Entry<FlowingFluid> GOO_FLUID = FLUIDS.register("goo_fluid", GooFluid.Source::new);
    public static final Entry<FlowingFluid> GOO_FLOWING = FLUIDS.register("goo_flowing", GooFluid.Flowing::new);

    private FluidInit() {}
}
