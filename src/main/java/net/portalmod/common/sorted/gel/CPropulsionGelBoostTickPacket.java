package net.portalmod.common.sorted.gel;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.portalmod.core.packet.AbstractPacket;
import net.portalmod.core.packet.PacketContext;

import java.util.function.Supplier;

public class CPropulsionGelBoostTickPacket implements AbstractPacket<CPropulsionGelBoostTickPacket> {
    private int ticks;

    public CPropulsionGelBoostTickPacket() {}

    public CPropulsionGelBoostTickPacket(int ticks) {
        this.ticks = ticks;
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(this.ticks);
    }

    @Override
    public CPropulsionGelBoostTickPacket decode(FriendlyByteBuf buffer) {
        return new CPropulsionGelBoostTickPacket(buffer.readInt());
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

                            ((IGelAffected) player).setPropulsionTicks(this.ticks);
                        });
        return true;
    }
}
