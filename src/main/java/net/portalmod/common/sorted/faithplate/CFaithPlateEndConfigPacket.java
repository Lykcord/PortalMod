package net.portalmod.common.sorted.faithplate;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.portalmod.core.packet.AbstractPacket;
import net.portalmod.core.packet.PacketContext;

import java.util.function.Supplier;

public class CFaithPlateEndConfigPacket implements AbstractPacket<CFaithPlateEndConfigPacket> {
    private BlockPos pos;

    public CFaithPlateEndConfigPacket() {}

    public CFaithPlateEndConfigPacket(BlockPos pos) {
        this.pos = pos;
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
    }

    @Override
    public CFaithPlateEndConfigPacket decode(FriendlyByteBuf buffer) {
        return new CFaithPlateEndConfigPacket(buffer.readBlockPos());
    }

    @Override
    public boolean handle(Supplier<PacketContext> context) {
        context.get()
                .enqueueWork(
                        () -> {
                            ServerPlayer sender = context.get().getSender();
                            if (sender == null) {
                                return;
                            }

                            FaithPlateTileEntity blockEntity =
                                    (FaithPlateTileEntity) sender.level().getBlockEntity(pos);
                            if (blockEntity == null) {
                                return;
                            }

                            blockEntity.endConfiguration();
                        });
        return true;
    }
}
