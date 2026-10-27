package net.portalmod.common.sorted.trigger;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.portalmod.core.packet.AbstractPacket;
import net.portalmod.core.packet.PacketContext;

import java.util.function.Supplier;

public class CTriggerAbortConfigPacket implements AbstractPacket<CTriggerAbortConfigPacket> {
    private BlockPos pos;

    public CTriggerAbortConfigPacket() {}

    public CTriggerAbortConfigPacket(BlockPos pos) {
        this.pos = pos;
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(this.pos);
    }

    @Override
    public CTriggerAbortConfigPacket decode(FriendlyByteBuf buffer) {
        return new CTriggerAbortConfigPacket(buffer.readBlockPos());
    }

    @Override
    public boolean handle(Supplier<PacketContext> context) {
        context.get()
                .enqueueWork(
                        () -> {
                            ServerPlayer sender = context.get().getSender();
                            if (sender != null) {
                                TriggerSelectionServer.endConfiguration(sender);
                            }
                        });
        return true;
    }
}
