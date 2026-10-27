package net.portalmod.core.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.Locale;
import java.util.function.Supplier;

public interface AbstractPacket<T extends AbstractPacket<T>> extends CustomPacketPayload {
    void encode(FriendlyByteBuf buffer);

    T decode(FriendlyByteBuf buffer);

    boolean handle(Supplier<PacketContext> context);

    @Override
    default Type<T> type() {
        return new Type<>(
                Identifier.fromNamespaceAndPath("portalmod", getClass().getSimpleName().toLowerCase(Locale.ROOT)));
    }
}
