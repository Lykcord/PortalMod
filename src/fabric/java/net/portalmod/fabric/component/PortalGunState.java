package net.portalmod.fabric.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;
import java.util.UUID;

public record PortalGunState(
        Optional<UUID> gunUuid,
        int primaryColor,
        int secondaryColor,
        int accentColor,
        boolean singlePortal,
        String skin,
        String lastShot,
        Optional<PortalTarget> primaryTarget,
        Optional<PortalTarget> secondaryTarget
) {
    public static final String PRIMARY = "primary";
    public static final String SECONDARY = "secondary";
    public static final String NONE = "none";
    public static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);

    public static final PortalGunState DEFAULT = new PortalGunState(
            Optional.empty(),
            0x3376F6,
            0xF6A633,
            0xFFFFFF,
            false,
            "default",
            NONE,
            Optional.empty(),
            Optional.empty()
    );

    public static final Codec<PortalGunState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUID_CODEC.optionalFieldOf("gun_uuid").forGetter(PortalGunState::gunUuid),
            Codec.INT.optionalFieldOf("primary_color", DEFAULT.primaryColor()).forGetter(PortalGunState::primaryColor),
            Codec.INT.optionalFieldOf("secondary_color", DEFAULT.secondaryColor()).forGetter(PortalGunState::secondaryColor),
            Codec.INT.optionalFieldOf("accent_color", DEFAULT.accentColor()).forGetter(PortalGunState::accentColor),
            Codec.BOOL.optionalFieldOf("single_portal", DEFAULT.singlePortal()).forGetter(PortalGunState::singlePortal),
            Codec.STRING.optionalFieldOf("skin", DEFAULT.skin()).forGetter(PortalGunState::skin),
            Codec.STRING.optionalFieldOf("last_shot", DEFAULT.lastShot()).forGetter(PortalGunState::lastShot),
            PortalTarget.CODEC.optionalFieldOf("primary_target").forGetter(PortalGunState::primaryTarget),
            PortalTarget.CODEC.optionalFieldOf("secondary_target").forGetter(PortalGunState::secondaryTarget)
    ).apply(instance, PortalGunState::new));

    public PortalGunState withLastShot(String side) {
        return new PortalGunState(gunUuid, primaryColor, secondaryColor, accentColor, singlePortal, skin, side, primaryTarget, secondaryTarget);
    }

    public PortalGunState withGunUuid(UUID gunUuid) {
        return new PortalGunState(Optional.of(gunUuid), primaryColor, secondaryColor, accentColor, singlePortal, skin, lastShot, primaryTarget, secondaryTarget);
    }

    public PortalGunState withPortalTarget(boolean primary, PortalTarget target) {
        return new PortalGunState(
                gunUuid,
                primaryColor,
                secondaryColor,
                accentColor,
                singlePortal,
                skin,
                primary ? PRIMARY : SECONDARY,
                primary ? Optional.of(target) : primaryTarget,
                primary ? secondaryTarget : Optional.of(target)
        );
    }
}
