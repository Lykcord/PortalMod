package net.portalmod.common.sorted.radio;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.portalmod.core.packet.AbstractPacket;
import net.portalmod.core.packet.PacketContext;
import net.portalmod.core.util.ClientExecution;

import java.util.function.Supplier;

public class SRadioUpdatePacket implements AbstractPacket<SRadioUpdatePacket> {
    private BlockPos pos;
    private RadioState state;

    public SRadioUpdatePacket() {}

    public SRadioUpdatePacket(BlockPos pos, RadioState state) {
        this.pos = pos;
        this.state = state;
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos).writeEnum(state);
    }

    @Override
    public SRadioUpdatePacket decode(FriendlyByteBuf buffer) {
        return new SRadioUpdatePacket(buffer.readBlockPos(), buffer.readEnum(RadioState.class));
    }

    @Override
    public boolean handle(Supplier<PacketContext> context) {
        context.get()
                .enqueueWork(
                        () -> {
                            ClientExecution.run(
                                    () ->
                                            () -> {
                                                var level = Minecraft.getInstance().level;
                                                if (level != null
                                                        && level.getBlockEntity(pos)
                                                                instanceof RadioBlockTileEntity blockEntity) {
                                                    blockEntity.handlePacket(state);
                                                }
                                            });
                        });
        return true;
    }
}
