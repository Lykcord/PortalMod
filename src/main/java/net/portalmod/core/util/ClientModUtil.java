package net.portalmod.core.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;
import net.portalmod.PortalMod;

public class ClientModUtil {
    public static void sendClientChat(Object... text) {
        ClientLevel clientWorld = Minecraft.getInstance().level;
        if (clientWorld == null) {
            PortalMod.LOGGER.error("Tried to send a client chat message while not in a client environment");
            return;
        }

        Minecraft.getInstance().execute(() -> ModUtil.sendChat(clientWorld, text));
    }

    public static Player getLocalPlayer() {
        return Minecraft.getInstance().player;
    }
}
