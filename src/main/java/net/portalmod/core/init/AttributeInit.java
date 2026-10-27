package net.portalmod.core.init;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.portalmod.PortalMod;
import net.portalmod.core.util.Registry;
import net.portalmod.core.util.Registry.Entry;

public class AttributeInit {
    public static final Registry<Holder<Attribute>> ATTRIBUTES = new Registry<>();

    private AttributeInit() {}

    public static final Entry<Holder<Attribute>> BUTTON_REACH = register("button_reach", 2, 0, 2048);
    public static final Entry<Holder<Attribute>> GRAB_REACH = register("grab_reach", 2, 0, 2048);

    private static Entry<Holder<Attribute>> register(String name, double base, double min, double max) {
        return ATTRIBUTES.register(
                "generic." + name,
                () ->
                        net.minecraft.core.Registry.registerForHolder(
                                BuiltInRegistries.ATTRIBUTE,
                                Identifier.fromNamespaceAndPath(PortalMod.MODID, "generic." + name),
                                new RangedAttribute("attribute.name.generic.portalmod." + name, base, min, max)
                                        .setSyncable(true)));
    }
}
