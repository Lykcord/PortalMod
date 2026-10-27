package net.portalmod.common.sorted.gel;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.portalmod.core.packet.AbstractPacket;
import net.portalmod.core.packet.PacketContext;

import java.util.function.Supplier;

public class CRepulsionGelBouncePacket implements AbstractPacket<CRepulsionGelBouncePacket> {
    private boolean bounced;

    public CRepulsionGelBouncePacket() {}

    public CRepulsionGelBouncePacket(boolean bounced) {
        this.bounced = bounced;
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(this.bounced);
    }

    @Override
    public CRepulsionGelBouncePacket decode(FriendlyByteBuf buffer) {
        return new CRepulsionGelBouncePacket(buffer.readBoolean());
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

                            ((IGelAffected) player).setBounced(this.bounced);
                        });
        return true;
    }
}
