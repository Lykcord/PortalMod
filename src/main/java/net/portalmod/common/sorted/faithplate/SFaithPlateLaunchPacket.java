package net.portalmod.common.sorted.faithplate;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.portalmod.core.packet.AbstractPacket;
import net.portalmod.core.packet.PacketContext;
import net.portalmod.core.util.ClientExecution;

import java.util.function.Supplier;

public class SFaithPlateLaunchPacket implements AbstractPacket<SFaithPlateLaunchPacket> {
    protected BlockPos pos;

    public SFaithPlateLaunchPacket() {}

    public SFaithPlateLaunchPacket(BlockPos pos) {
        this.pos = pos;
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
    }

    @Override
    public SFaithPlateLaunchPacket decode(FriendlyByteBuf buffer) {
        return new SFaithPlateLaunchPacket(buffer.readBlockPos());
    }

    @Override
    public boolean handle(Supplier<PacketContext> context) {
        context.get().enqueueWork(() -> ClientExecution.run(() -> () -> FaithPlateClient.handleLaunch(this, context)));
        return true;
    }
}
