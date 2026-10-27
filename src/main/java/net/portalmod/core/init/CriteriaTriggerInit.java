package net.portalmod.core.init;

import net.minecraft.advancements.triggers.PlayerTrigger;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.portalmod.PortalMod;
import net.portalmod.core.util.Registry;

public class CriteriaTriggerInit {
    private CriteriaTriggerInit() {}

    public static final Registry<SimpleCriterionTrigger<?>> REGISTRY =
            new Registry<>(
                    (id, trigger) ->
                            net.minecraft.core.Registry.register(
                                    BuiltInRegistries.TRIGGER_TYPES,
                                    Identifier.fromNamespaceAndPath(PortalMod.MODID, id),
                                    trigger));

    public static final Registry.Entry<PlayerTrigger> SHOOT_MOON = registerCBT("shoot_moon");
    public static final Registry.Entry<PlayerTrigger> FAITH_PLATE_ELYTRA = registerCBT("faith_plate_elytra");
    public static final Registry.Entry<PlayerTrigger> TRIPLE_TELEPORT = registerCBT("triple_teleportation");
    public static final Registry.Entry<PlayerTrigger> PLACE_PORTALS = registerCBT("place_portals");
    public static final Registry.Entry<PlayerTrigger> PORTAL_SURFACE = registerCBT("portal_surface");
    public static final Registry.Entry<PlayerTrigger> BOUNCE_ON_GEL = registerCBT("bounce_on_gel");
    public static final Registry.Entry<PlayerTrigger> SHOOT_PORTAL_FAR = registerCBT("shoot_portal_far");
    public static final Registry.Entry<PlayerTrigger> GRAB_ENTITY = registerCBT("grab_entity");
    public static final Registry.Entry<PlayerTrigger> CUBE_ON_BUTTON = registerCBT("cube_on_button");
    public static final Registry.Entry<PlayerTrigger> SURVIVE_TURRET = registerCBT("survive_turret");
    public static final Registry.Entry<PlayerTrigger> TURRET_DEFENSE = registerCBT("turret_defense");

    private static Registry.Entry<PlayerTrigger> registerCBT(String name) {
        return REGISTRY.register(name, PlayerTrigger::new);
    }
}
