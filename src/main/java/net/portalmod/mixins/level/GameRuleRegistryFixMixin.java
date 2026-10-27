package net.portalmod.mixins.level;

import com.mojang.serialization.Dynamic;

import net.minecraft.util.datafix.fixes.GameRuleRegistryFix;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(GameRuleRegistryFix.class)
public class GameRuleRegistryFixMixin {
    @Inject(method = "lambda$makeRule$2", at = @At("RETURN"), cancellable = true)
    private static void pmUpgradeForgeRules(Dynamic<?> input, CallbackInfoReturnable<Dynamic<?>> callback) {
        Dynamic<?> rules = callback.getReturnValue();
        for (var alias :
                Map.of(
                                "portalSlowShot",
                                "portalmod:portal_slow_shot",
                                "usePortalableBlacklist",
                                "portalmod:use_portalable_blacklist",
                                "allowPortalOverwrite",
                                "portalmod:allow_portal_overwrite")
                        .entrySet()) {
            var old = rules.get(alias.getKey()).result();
            if (old.isEmpty()) {
                continue;
            }
            if (rules.get(alias.getValue()).result().isEmpty()) {
                boolean value =
                        old.get()
                                .asString()
                                .result()
                                .map(Boolean::parseBoolean)
                                .orElseGet(() -> old.get().asBoolean(false));
                rules = rules.set(alias.getValue(), rules.createBoolean(value));
            }
            rules = rules.remove(alias.getKey());
        }
        callback.setReturnValue(rules);
    }
}
