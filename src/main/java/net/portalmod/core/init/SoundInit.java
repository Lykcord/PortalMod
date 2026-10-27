package net.portalmod.core.init;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.portalmod.PortalMod;
import net.portalmod.core.util.Registry;
import net.portalmod.core.util.Registry.Entry;

public class SoundInit {
    public static final Registry<SoundEvent> SOUNDS =
            new Registry<>(
                    (id, value) ->
                            net.minecraft.core.Registry.register(
                                    BuiltInRegistries.SOUND_EVENT,
                                    Identifier.fromNamespaceAndPath(PortalMod.MODID, id),
                                    value));

    private SoundInit() {}

    // BLOCKS
    public static final Entry<SoundEvent> RADIO_LOOP = register("block.radio.loop");
    public static final Entry<SoundEvent> RADIO_DINOSAUR1 = register("block.radio.transmission");

    public static final Entry<SoundEvent> GEL_BREAK = register("block.gel.break");
    public static final Entry<SoundEvent> GEL_COLLECT = register("block.gel.collect");
    public static final Entry<SoundEvent> GEL_PLACE = register("block.gel.place");
    public static final Entry<SoundEvent> GEL_STEP = register("block.gel.step");
    public static final Entry<SoundEvent> REPULSION_GEL_BOUNCE = register("block.gel.bounce");

    public static final Entry<SoundEvent> CHAMBER_DOOR_OPEN = register("block.chamber_door.open");
    public static final Entry<SoundEvent> CHAMBER_DOOR_CLOSE = register("block.chamber_door.close");

    public static final Entry<SoundEvent> CHAMBER_LIGHTS_AMBIENT = register("block.chamber_lights.hum");
    public static final Entry<SoundEvent> CHAMBER_LIGHTS_FLICKER = register("block.chamber_lights.flicker");

    public static final Entry<SoundEvent> PUSH_DOOR_OPEN = register("block.push_door.open");
    public static final Entry<SoundEvent> PUSH_DOOR_CLOSE = register("block.push_door.close");

    public static final Entry<SoundEvent> CUBE_DROPPER_OPEN = register("block.cube_dropper.open");
    public static final Entry<SoundEvent> CUBE_DROPPER_CLOSE = register("block.cube_dropper.close");

    public static final Entry<SoundEvent> BUTTON_ACTIVATE = register("block.button.activate");
    public static final Entry<SoundEvent> BUTTON_DEACTIVATE = register("block.button.deactivate");
    public static final Entry<SoundEvent> SUPER_BUTTON_PRESS = register("block.super_button.press");
    public static final Entry<SoundEvent> SUPER_BUTTON_RELEASE = register("block.super_button.release");

    public static final Entry<SoundEvent> FAITHPLATE_LAUNCH = register("block.faithplate.launch");

    public static final Entry<SoundEvent> FIZZLER_ACTIVATE = register("block.fizzler_emitter.activate");
    public static final Entry<SoundEvent> FIZZLER_DEACTIVATE = register("block.fizzler_emitter.deactivate");

    public static final Entry<SoundEvent> ANTLINE_INDICATOR_ACTIVATE = register("block.antline_indicator.activate");
    public static final Entry<SoundEvent> ANTLINE_INDICATOR_DEACTIVATE = register("block.antline_indicator.deactivate");
    public static final Entry<SoundEvent> ANTLINE_TIMER_TICK = register("block.antline_timer.tick");

    public static final Entry<SoundEvent> CAKE_EAT_CANDLE = register("block.forest_cake.eat_candle");

    // ITEM
    public static final Entry<SoundEvent> BUCKET_FILL_GOO = register("item.bucket.fill_goo");
    public static final Entry<SoundEvent> BUCKET_EMPTY_GOO = register("item.bucket.empty_goo");
    public static final Entry<SoundEvent> PORTALGUN_FIRE_PRIMARY = register("item.portalgun.fire_primary");
    public static final Entry<SoundEvent> PORTALGUN_FIRE_SECONDARY = register("item.portalgun.fire_secondary");
    public static final Entry<SoundEvent> PORTALGUN_MISS = register("item.portalgun.miss");
    public static final Entry<SoundEvent> PORTALGUN_FIZZLE = register("item.portalgun.fizzle");
    public static final Entry<SoundEvent> PORTALGUN_LIFT = register("item.portalgun.lift");
    public static final Entry<SoundEvent> PORTALGUN_HOLD = register("item.portalgun.hold");
    public static final Entry<SoundEvent> PORTALGUN_DROP = register("item.portalgun.drop");

    public static final Entry<SoundEvent> WRENCH_USE = register("item.wrench.use");
    public static final Entry<SoundEvent> WRENCH_FAIL = register("item.wrench.fail");

    // DISC
    public static final Entry<SoundEvent> DISC_RAIN = register("disc.rain");

    // ENTITY
    public static final Entry<SoundEvent> PORTAL_OPEN = register("entity.portal.open");
    public static final Entry<SoundEvent> PORTAL_CLOSE = register("entity.portal.close");
    public static final Entry<SoundEvent> PORTAL_TELEPORT = register("entity.portal.teleport");
    public static final Entry<SoundEvent> GOO_DAMAGE = register("entity.player.hurt_goo");

    public static final Entry<SoundEvent> ENTITY_FIZZLE = register("entity.fizzle");

    public static final Entry<SoundEvent> CUBE_HIT = register("entity.cube.hit");
    public static final Entry<SoundEvent> CUBE_GABE = register("entity.cube.gabe");

    public static final Entry<SoundEvent> TURRET_OPEN = register("entity.turret.open");
    public static final Entry<SoundEvent> TURRET_CLOSE = register("entity.turret.close");
    public static final Entry<SoundEvent> TURRET_FIRE = register("entity.turret.fire");
    public static final Entry<SoundEvent> TURRET_FIRE_FAIL = register("entity.turret.fire_fail");
    public static final Entry<SoundEvent> TURRET_STOCK = register("entity.turret.stock");

    public static final Entry<SoundEvent> CHAMBER_SIGN_PLACE = register("entity.chamber_sign.place");

    private static Entry<SoundEvent> register(String id) {
        return SOUNDS.register(
                id, () -> SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(PortalMod.MODID, id)));
    }
}
