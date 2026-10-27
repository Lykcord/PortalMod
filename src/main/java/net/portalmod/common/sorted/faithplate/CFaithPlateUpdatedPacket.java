package net.portalmod.common.sorted.faithplate;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.portalmod.core.packet.AbstractPacket;
import net.portalmod.core.packet.PacketContext;

import java.util.function.Supplier;

public class CFaithPlateUpdatedPacket implements AbstractPacket<CFaithPlateUpdatedPacket> {
    private BlockPos pos;
    private CompoundTag nbt;

    public CFaithPlateUpdatedPacket() {}

    public CFaithPlateUpdatedPacket(BlockPos pos, CompoundTag nbt) {
        this.pos = pos;
        this.nbt = nbt;
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos).writeNbt(nbt);
    }

    @Override
    public CFaithPlateUpdatedPacket decode(FriendlyByteBuf buffer) {
        return new CFaithPlateUpdatedPacket(buffer.readBlockPos(), buffer.readNbt());
    }

    @Override
    public boolean handle(Supplier<PacketContext> context) {
        context.get()
                .enqueueWork(
                        () -> {
                            var sender = context.get().getSender();
                            if (sender == null || nbt == null) {
                                return;
                            }
                            var level = sender.level();
                            if (!(level.getBlockEntity(pos) instanceof FaithPlateTileEntity blockEntity)) {
                                return;
                            }
                            blockEntity.loadCustomOnly(
                                    TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), nbt));
                            blockEntity.setChanged();
                            var state = blockEntity.getBlockState();
                            level.sendBlockUpdated(pos, state, state, 3);
                        });
        return true;
    }
}
