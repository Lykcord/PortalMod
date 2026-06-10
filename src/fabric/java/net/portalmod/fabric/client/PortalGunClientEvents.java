package net.portalmod.fabric.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.portalmod.fabric.network.PortalGunEventPayload;
import net.portalmod.fabric.registry.PortalModSounds;

/**
 * Client-side portal gun feedback. Tracks the most recent gun events so HUD and item
 * animations (crosshair, lift/drop/shoot poses) can react to them, and runs the looping
 * hold hum while the local player carries a prop.
 */
public final class PortalGunClientEvents {
    private static PortalGunEventPayload.Event lastEvent;
    private static long lastEventMillis;
    private static SoundInstance holdLoop;

    private PortalGunClientEvents() {
    }

    public static void handle(PortalGunEventPayload payload) {
        lastEvent = payload.event();
        lastEventMillis = System.currentTimeMillis();

        switch (payload.event()) {
            case LIFT -> startHoldLoop();
            case DROP, FIZZLE -> stopHoldLoop();
            default -> {
            }
        }
    }

    public static PortalGunEventPayload.Event lastEvent() {
        return lastEvent;
    }

    public static long millisSinceLastEvent() {
        return lastEvent == null ? Long.MAX_VALUE : System.currentTimeMillis() - lastEventMillis;
    }

    private static void startHoldLoop() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || holdLoop != null) {
            return;
        }

        holdLoop = new EntityLoopableSound(player, PortalModSounds.PORTALGUN_HOLD, SoundSource.PLAYERS);
        minecraft.getSoundManager().play(holdLoop);
    }

    private static void stopHoldLoop() {
        if (holdLoop != null) {
            Minecraft.getInstance().getSoundManager().stop(holdLoop);
            holdLoop = null;
        }
    }
}
