package net.portalmod.common.sorted.trigger;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.portalmod.core.packet.AbstractPacket;
import net.portalmod.core.packet.ClientPacketHandler;
import net.portalmod.core.packet.PacketContext;
import net.portalmod.core.util.ClientExecution;

import java.util.function.Supplier;

public class STriggerStartConfigPacket implements AbstractPacket<STriggerStartConfigPacket> {
    public BlockPos pos;

    public STriggerStartConfigPacket() {}

    public STriggerStartConfigPacket(BlockPos pos) {
        this.pos = pos;
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
    }

    @Override
    public STriggerStartConfigPacket decode(FriendlyByteBuf buffer) {
        return new STriggerStartConfigPacket(buffer.readBlockPos());
    }

    @Override
    public boolean handle(Supplier<PacketContext> context) {
        context.get()
                .enqueueWork(
                        () ->
                                ClientExecution.run(
                                        () -> () -> ClientPacketHandler.handleSTriggerStartConfigPacket(this)));
        return true;
    }
}
