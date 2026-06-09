package net.portalmod.fabric.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.portalmod.fabric.PortalModFabric;

import java.util.LinkedHashMap;
import java.util.Map;

public final class PortalModSounds {
    private static final Map<String, SoundEvent> SOUNDS = new LinkedHashMap<>();

    public static final SoundEvent RADIO_LOOP = register("block.radio.loop");
    public static final SoundEvent RADIO_DINOSAUR1 = register("block.radio.transmission");

    public static final SoundEvent GEL_BREAK = register("block.gel.break");
    public static final SoundEvent GEL_COLLECT = register("block.gel.collect");
    public static final SoundEvent GEL_PLACE = register("block.gel.place");
    public static final SoundEvent GEL_STEP = register("block.gel.step");
    public static final SoundEvent REPULSION_GEL_BOUNCE = register("block.gel.bounce");

    public static final SoundEvent CHAMBER_DOOR_OPEN = register("block.chamber_door.open");
    public static final SoundEvent CHAMBER_DOOR_CLOSE = register("block.chamber_door.close");
    public static final SoundEvent CHAMBER_LIGHTS_AMBIENT = register("block.chamber_lights.hum");
    public static final SoundEvent CHAMBER_LIGHTS_FLICKER = register("block.chamber_lights.flicker");
    public static final SoundEvent PUSH_DOOR_OPEN = register("block.push_door.open");
    public static final SoundEvent PUSH_DOOR_CLOSE = register("block.push_door.close");
    public static final SoundEvent CUBE_DROPPER_OPEN = register("block.cube_dropper.open");
    public static final SoundEvent CUBE_DROPPER_CLOSE = register("block.cube_dropper.close");

    public static final SoundEvent BUTTON_ACTIVATE = register("block.button.activate");
    public static final SoundEvent BUTTON_DEACTIVATE = register("block.button.deactivate");
    public static final SoundEvent SUPER_BUTTON_PRESS = register("block.super_button.press");
    public static final SoundEvent SUPER_BUTTON_RELEASE = register("block.super_button.release");
    public static final SoundEvent FAITHPLATE_LAUNCH = register("block.faithplate.launch");
    public static final SoundEvent FIZZLER_ACTIVATE = register("block.fizzler_emitter.activate");
    public static final SoundEvent FIZZLER_DEACTIVATE = register("block.fizzler_emitter.deactivate");
    public static final SoundEvent ANTLINE_INDICATOR_ACTIVATE = register("block.antline_indicator.activate");
    public static final SoundEvent ANTLINE_INDICATOR_DEACTIVATE = register("block.antline_indicator.deactivate");
    public static final SoundEvent ANTLINE_TIMER_TICK = register("block.antline_timer.tick");
    public static final SoundEvent CAKE_EAT_CANDLE = register("block.forest_cake.eat_candle");

    public static final SoundEvent PORTALGUN_FIRE_PRIMARY = register("item.portalgun.fire_primary");
    public static final SoundEvent PORTALGUN_FIRE_SECONDARY = register("item.portalgun.fire_secondary");
    public static final SoundEvent PORTALGUN_MISS = register("item.portalgun.miss");
    public static final SoundEvent PORTALGUN_FIZZLE = register("item.portalgun.fizzle");
    public static final SoundEvent PORTALGUN_LIFT = register("item.portalgun.lift");
    public static final SoundEvent PORTALGUN_HOLD = register("item.portalgun.hold");
    public static final SoundEvent PORTALGUN_DROP = register("item.portalgun.drop");
    public static final SoundEvent WRENCH_USE = register("item.wrench.use");
    public static final SoundEvent WRENCH_FAIL = register("item.wrench.fail");

    public static final SoundEvent DISC_RAIN = register("disc.rain");

    public static final SoundEvent PORTAL_OPEN = register("entity.portal.open");
    public static final SoundEvent PORTAL_CLOSE = register("entity.portal.close");
    public static final SoundEvent PORTAL_TELEPORT = register("entity.portal.teleport");
    public static final SoundEvent GOO_DAMAGE = register("entity.player.hurt_goo");
    public static final SoundEvent ENTITY_FIZZLE = register("entity.fizzle");
    public static final SoundEvent CUBE_HIT = register("entity.cube.hit");
    public static final SoundEvent CUBE_GABE = register("entity.cube.gabe");
    public static final SoundEvent TURRET_OPEN = register("entity.turret.open");
    public static final SoundEvent TURRET_CLOSE = register("entity.turret.close");
    public static final SoundEvent TURRET_FIRE = register("entity.turret.fire");
    public static final SoundEvent TURRET_FIRE_FAIL = register("entity.turret.fire_fail");
    public static final SoundEvent TURRET_STOCK = register("entity.turret.stock");
    public static final SoundEvent CHAMBER_SIGN_PLACE = register("entity.chamber_sign.place");

    private PortalModSounds() {
    }

    public static void register() {
        PortalModFabric.LOGGER.info("Registered {} PortalMod sound events.", SOUNDS.size());
    }

    private static SoundEvent register(String name) {
        Identifier id = Identifier.fromNamespaceAndPath(PortalModFabric.MOD_ID, name);
        SoundEvent event = SoundEvent.createVariableRangeEvent(id);
        SOUNDS.put(name, event);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, event);
    }
}
