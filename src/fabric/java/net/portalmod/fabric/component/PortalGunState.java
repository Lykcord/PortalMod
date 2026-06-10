package net.portalmod.fabric.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.portalmod.fabric.portal.PortalColors;

import java.util.Optional;
import java.util.UUID;

public record PortalGunState(
        Optional<UUID> gunUuid,
        String primaryHue,
        String secondaryHue,
        String accentHue,
        boolean singlePortal,
        String skin,
        String lastShot,
        boolean holding,
        Optional<PortalTarget> primaryTarget,
        Optional<PortalTarget> secondaryTarget
) {
    public static final String PRIMARY = "primary";
    public static final String SECONDARY = "secondary";
    public static final String NONE = "none";
    public static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);

    public static final PortalGunState DEFAULT = new PortalGunState(
            Optional.empty(),
            "blue",
            "orange",
            NONE,
            false,
            "default",
            NONE,
            false,
            Optional.empty(),
            Optional.empty()
    );

    public static final Codec<PortalGunState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUID_CODEC.optionalFieldOf("gun_uuid").forGetter(PortalGunState::gunUuid),
            Codec.STRING.optionalFieldOf("primary_hue", DEFAULT.primaryHue()).forGetter(PortalGunState::primaryHue),
            Codec.STRING.optionalFieldOf("secondary_hue", DEFAULT.secondaryHue()).forGetter(PortalGunState::secondaryHue),
            Codec.STRING.optionalFieldOf("accent_hue", DEFAULT.accentHue()).forGetter(PortalGunState::accentHue),
            Codec.BOOL.optionalFieldOf("single_portal", DEFAULT.singlePortal()).forGetter(PortalGunState::singlePortal),
            Codec.STRING.optionalFieldOf("skin", DEFAULT.skin()).forGetter(PortalGunState::skin),
            Codec.STRING.optionalFieldOf("last_shot", DEFAULT.lastShot()).forGetter(PortalGunState::lastShot),
            Codec.BOOL.optionalFieldOf("holding", DEFAULT.holding()).forGetter(PortalGunState::holding),
            PortalTarget.CODEC.optionalFieldOf("primary_target").forGetter(PortalGunState::primaryTarget),
            PortalTarget.CODEC.optionalFieldOf("secondary_target").forGetter(PortalGunState::secondaryTarget)
    ).apply(instance, PortalGunState::new));

    /** Representative RGB of the primary portal hue. */
    public int primaryColor() {
        return PortalColors.colorOf(primaryHue);
    }

    /** Representative RGB of the secondary portal hue. */
    public int secondaryColor() {
        return PortalColors.colorOf(secondaryHue);
    }

    public PortalGunState withLastShot(String side) {
        return new PortalGunState(gunUuid, primaryHue, secondaryHue, accentHue, singlePortal, skin, side, holding, primaryTarget, secondaryTarget);
    }

    public PortalGunState withGunUuid(UUID gunUuid) {
        return new PortalGunState(Optional.of(gunUuid), primaryHue, secondaryHue, accentHue, singlePortal, skin, lastShot, holding, primaryTarget, secondaryTarget);
    }

    public PortalGunState withHolding(boolean holding) {
        return new PortalGunState(gunUuid, primaryHue, secondaryHue, accentHue, singlePortal, skin, lastShot, holding, primaryTarget, secondaryTarget);
    }

    public PortalGunState withHues(String primaryHue, String secondaryHue, String accentHue) {
        return new PortalGunState(gunUuid, primaryHue, secondaryHue, accentHue, singlePortal, skin, lastShot, holding, primaryTarget, secondaryTarget);
    }

    public PortalGunState withSkin(String skin) {
        return new PortalGunState(gunUuid, primaryHue, secondaryHue, accentHue, singlePortal, skin, lastShot, holding, primaryTarget, secondaryTarget);
    }

    public PortalGunState withPortalTarget(boolean primary, PortalTarget target) {
        return new PortalGunState(
                gunUuid,
                primaryHue,
                secondaryHue,
                accentHue,
                singlePortal,
                skin,
                primary ? PRIMARY : SECONDARY,
                holding,
                primary ? Optional.of(target) : primaryTarget,
                primary ? secondaryTarget : Optional.of(target)
        );
    }
}
