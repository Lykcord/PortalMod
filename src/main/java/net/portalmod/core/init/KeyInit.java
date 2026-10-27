package net.portalmod.core.init;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.portalmod.PortalMod;

public class KeyInit {

    private KeyInit() {}

    public static KeyMapping PORTALGUN_INTERACT;

    public static void init() {
        PORTALGUN_INTERACT =
                new KeyMapping(
                        "key." + PortalMod.MODID + ".portalgun_interact",
                        InputConstants.KEY_E,
                        KeyMapping.Category.register(Identifier.fromNamespaceAndPath(PortalMod.MODID, "main")));

        KeyMappingHelper.registerKeyMapping(PORTALGUN_INTERACT);
    }
}
