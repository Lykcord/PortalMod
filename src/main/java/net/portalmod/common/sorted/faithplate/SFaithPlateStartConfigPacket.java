package net.portalmod.common.sorted.faithplate;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.portalmod.core.packet.AbstractPacket;
import net.portalmod.core.packet.PacketContext;
import net.portalmod.core.util.ClientExecution;

import java.util.function.Supplier;

public class SFaithPlateStartConfigPacket implements AbstractPacket<SFaithPlateStartConfigPacket> {
    protected BlockPos pos;

    public SFaithPlateStartConfigPacket() {}

    public SFaithPlateStartConfigPacket(BlockPos pos) {
        this.pos = pos;
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
    }

    @Override
    public SFaithPlateStartConfigPacket decode(FriendlyByteBuf buffer) {
        return new SFaithPlateStartConfigPacket(buffer.readBlockPos());
    }

    @Override
    public boolean handle(Supplier<PacketContext> context) {
        context.get().enqueueWork(() -> ClientExecution.run(() -> () -> FaithPlateClient.setScreen(pos)));
        return true;
    }
}
