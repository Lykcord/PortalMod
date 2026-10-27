package net.portalmod.common.sorted.antline;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.Block;
import net.portalmod.core.packet.AbstractPacket;
import net.portalmod.core.packet.PacketContext;
import net.portalmod.core.util.ClientExecution;

import java.util.function.Supplier;

public class SAntlineUpdatePacket implements AbstractPacket<SAntlineUpdatePacket> {
    private BlockPos pos;
    private CompoundTag nbt;

    public SAntlineUpdatePacket() {}

    public SAntlineUpdatePacket(BlockPos pos, CompoundTag nbt) {
        this.pos = pos;
        this.nbt = nbt;
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos).writeNbt(nbt);
    }

    @Override
    public SAntlineUpdatePacket decode(FriendlyByteBuf buffer) {
        return new SAntlineUpdatePacket(buffer.readBlockPos(), buffer.readNbt());
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
                                                if (level == null
                                                        || nbt == null
                                                        || !(level.getBlockEntity(pos)
                                                                instanceof AntlineTileEntity blockEntity)) {
                                                    return;
                                                }
                                                blockEntity.load(nbt, false);
                                                var state = blockEntity.getBlockState();
                                                level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
                                            });
                        });
        return true;
    }
}
