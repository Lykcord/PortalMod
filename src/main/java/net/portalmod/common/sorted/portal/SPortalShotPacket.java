package net.portalmod.common.sorted.portal;

import net.minecraft.network.FriendlyByteBuf;
import net.portalmod.core.packet.AbstractPacket;
import net.portalmod.core.packet.ClientPacketHandler;
import net.portalmod.core.packet.PacketContext;
import net.portalmod.core.util.ClientExecution;

import java.util.function.Supplier;

public class SPortalShotPacket implements AbstractPacket<SPortalShotPacket> {
    public int id;

    public SPortalShotPacket() {}

    public SPortalShotPacket(int id) {
        this.id = id;
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(this.id);
    }

    @Override
    public SPortalShotPacket decode(FriendlyByteBuf buffer) {
        return new SPortalShotPacket(buffer.readInt());
    }

    @Override
    public boolean handle(Supplier<PacketContext> context) {
        context.get()
                .enqueueWork(() -> ClientExecution.run(() -> () -> ClientPacketHandler.handleSPortalShotPacket(this)));
        return true;
    }
}
