package net.portalmod.core.init;

import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;
import net.portalmod.PortalMod;
import net.portalmod.core.packet.SUpdateBooleanGameRulePacket;

import java.util.HashMap;
import java.util.Map;

public final class GameRuleInit {
    private static final Map<String, GameRule<Boolean>> SYNCED_RULES = new HashMap<>();
    private static final Map<GameRule<Boolean>, Boolean> CLIENT_VALUES = new HashMap<>();

    public static final GameRule<Boolean> PORTAL_SLOWSHOT = register("portal_slow_shot", false, true);
    public static final GameRule<Boolean> USE_PORTALABLE_BLACKLIST = register("use_portalable_blacklist", false, true);
    public static final GameRule<Boolean> ALLOW_PORTAL_OVERWRITE = register("allow_portal_overwrite", true, false);

    private GameRuleInit() {}

    public static void registerAll() {}

    private static GameRule<Boolean> register(String name, boolean defaultValue, boolean sync) {
        GameRule<Boolean> rule =
                GameRuleBuilder.forBoolean(defaultValue)
                        .category(GameRuleCategory.PLAYER)
                        .buildAndRegister(Identifier.fromNamespaceAndPath(PortalMod.MODID, name));
        if (sync) {
            SYNCED_RULES.put(rule.id(), rule);
            GameRuleEvents.changeCallback(rule)
                    .register(
                            (value, server) ->
                                    PacketInit.sendToAll(new SUpdateBooleanGameRulePacket(rule.id(), value)));
        }
        return rule;
    }

    public static boolean getBoolean(Level level, GameRule<Boolean> rule) {
        if (level instanceof ServerLevel serverLevel) {
            return serverLevel.getGameRules().get(rule);
        }
        return CLIENT_VALUES.getOrDefault(rule, rule.defaultValue());
    }

    public static void updateClientRule(String name, boolean value) {
        GameRule<Boolean> rule = SYNCED_RULES.get(name);
        if (rule != null) {
            CLIENT_VALUES.put(rule, value);
        }
    }

    public static void clearClientRules() {
        CLIENT_VALUES.clear();
    }

    public static void sendBooleanRule(ServerPlayer player, GameRule<Boolean> rule) {
        PacketInit.sendToPlayer(
                player, new SUpdateBooleanGameRulePacket(rule.id(), player.level().getGameRules().get(rule)));
    }
}
