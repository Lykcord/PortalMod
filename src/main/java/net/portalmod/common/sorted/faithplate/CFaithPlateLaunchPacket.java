package net.portalmod.common.sorted.faithplate;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.portalmod.core.init.PacketInit;
import net.portalmod.core.packet.AbstractPacket;
import net.portalmod.core.packet.PacketContext;

import java.util.function.Supplier;

public class CFaithPlateLaunchPacket implements AbstractPacket<CFaithPlateLaunchPacket> {
    private BlockPos pos;

    public CFaithPlateLaunchPacket() {}

    public CFaithPlateLaunchPacket(BlockPos pos) {
        this.pos = pos;
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
    }

    @Override
    public CFaithPlateLaunchPacket decode(FriendlyByteBuf buffer) {
        return new CFaithPlateLaunchPacket(buffer.readBlockPos());
    }

    @Override
    public boolean handle(Supplier<PacketContext> context) {
        context.get()
                .enqueueWork(
                        () -> {
                            var player = context.get().getSender();
                            if (player == null) {
                                return;
                            }
                            Level level = player.level();
                            if (!level.hasChunkAt(pos)
                                    || !(level.getBlockEntity(pos) instanceof FaithPlateTileEntity)) {
                                return;
                            }
                            PacketInit.sendToTrackingAndSelf(player, new SFaithPlateLaunchPacket(pos));

                            //                    .distanceTo(context.get().getSender().getPosition(0)) > 3)

                            //            FaithPlateTER ter =
                            //            level.playSound(Minecraft.getInstance().player,
                            //                pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5,

                        });
        return true;
    }
}
