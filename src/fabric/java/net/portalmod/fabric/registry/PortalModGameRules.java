package net.portalmod.fabric.registry;

import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;
import net.portalmod.fabric.PortalModFabric;

public final class PortalModGameRules {
    /** Portal shots travel as a slow projectile instead of placing instantly. */
    public static final GameRule<Boolean> PORTAL_SLOW_SHOT = register("portal_slow_shot", false);
    /** When true, any occluding block that is not blacklisted is portalable; when false only whitelisted blocks are. */
    public static final GameRule<Boolean> USE_PORTALABLE_BLACKLIST = register("use_portalable_blacklist", false);
    /** Whether shooting over a foreign gun's portal replaces it instead of failing the shot. */
    public static final GameRule<Boolean> ALLOW_PORTAL_OVERWRITE = register("allow_portal_overwrite", true);

    private PortalModGameRules() {
    }

    public static void register() {
        PortalModFabric.LOGGER.info("Registered PortalMod game rules.");
    }

    private static GameRule<Boolean> register(String name, boolean defaultValue) {
        return GameRuleBuilder.forBoolean(defaultValue)
                .category(GameRuleCategory.PLAYER)
                .buildAndRegister(Identifier.fromNamespaceAndPath(PortalModFabric.MOD_ID, name));
    }
}
