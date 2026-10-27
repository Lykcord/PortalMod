package net.portalmod.common.sorted.portal;

import io.netty.handler.codec.DecoderException;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.portalmod.core.packet.AbstractPacket;
import net.portalmod.core.packet.PacketContext;

import java.util.function.Supplier;

public class CThroughPortalProofPacket implements AbstractPacket<CThroughPortalProofPacket> {
    private int[] portalChain;

    public CThroughPortalProofPacket() {}

    public CThroughPortalProofPacket(int[] portalChain) {
        this.portalChain = portalChain;
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        int length = this.portalChain.length;
        buffer.writeInt(length);

        for (int i = 0; i < length; i++) {
            buffer.writeInt(this.portalChain[i]);
        }
    }

    @Override
    public CThroughPortalProofPacket decode(FriendlyByteBuf buffer) {
        int length = buffer.readInt();
        if (length < 0 || length > 100) {
            throw new DecoderException("Invalid portal chain length: " + length);
        }
        int[] portalChain = new int[length];

        for (int i = 0; i < length; i++) {
            portalChain[i] = buffer.readInt();
        }

        return new CThroughPortalProofPacket(portalChain);
    }

    @Override
    public boolean handle(Supplier<PacketContext> context) {
        context.get()
                .enqueueWork(
                        () -> {
                            ServerPlayer player = context.get().getSender();
                            if (player == null) {
                                return;
                            }

                            MinecraftServer server = player.level().getServer();
                            if (server == null) {
                                return;
                            }

                            PortalServerProofManager.getInstance()
                                    .setProof(player, server.getTickCount(), this.portalChain);
                        });
        return true;
    }
}
