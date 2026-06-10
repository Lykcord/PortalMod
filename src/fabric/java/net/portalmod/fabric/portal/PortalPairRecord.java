package net.portalmod.fabric.portal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Optional;

public record PortalPairRecord(Optional<PortalRecord> primary, Optional<PortalRecord> secondary) {
    public static final PortalPairRecord EMPTY = new PortalPairRecord(Optional.empty(), Optional.empty());

    public static final Codec<PortalPairRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            PortalRecord.CODEC.optionalFieldOf("primary").forGetter(PortalPairRecord::primary),
            PortalRecord.CODEC.optionalFieldOf("secondary").forGetter(PortalPairRecord::secondary)
    ).apply(instance, PortalPairRecord::new));

    public static final StreamCodec<FriendlyByteBuf, PortalPairRecord> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(PortalRecord.STREAM_CODEC),
            PortalPairRecord::primary,
            ByteBufCodecs.optional(PortalRecord.STREAM_CODEC),
            PortalPairRecord::secondary,
            PortalPairRecord::new
    );

    public Optional<PortalRecord> end(boolean primaryEnd) {
        return primaryEnd ? primary : secondary;
    }

    public PortalPairRecord withEnd(boolean primaryEnd, Optional<PortalRecord> record) {
        return primaryEnd ? new PortalPairRecord(record, secondary) : new PortalPairRecord(primary, record);
    }

    public boolean isEmpty() {
        return primary.isEmpty() && secondary.isEmpty();
    }

    public boolean isComplete() {
        return primary.isPresent() && secondary.isPresent();
    }
}
