package net.portalmod.common.sorted.faithplate;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.portalmod.core.init.SoundInit;
import net.portalmod.core.packet.PacketContext;
import net.portalmod.core.util.ModUtil;

import java.util.function.Supplier;

public class FaithPlateClient {
    protected static void handleLaunch(SFaithPlateLaunchPacket packet, Supplier<PacketContext> context) {
        Level level = Minecraft.getInstance().level;
        if (level == null || !(level.getBlockEntity(packet.pos) instanceof FaithPlateTileEntity be)) {
            return;
        }

        // todo animate only if successful

        BlockEntityRenderer<?, ?> renderer = Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(be);
        if (renderer instanceof FaithPlateTER ter) {
            ter.startLaunch(be);
        }
        level.playSound(
                Minecraft.getInstance().player,
                packet.pos.getX() + .5,
                packet.pos.getY() + .5,
                packet.pos.getZ() + .5,
                SoundInit.FAITHPLATE_LAUNCH.get(),
                SoundSource.BLOCKS,
                1,
                ModUtil.randomSoundPitch());
    }

    protected static void setScreen(BlockPos pos) {
        Minecraft.getInstance().gui.setScreen(new FaithPlateConfigScreen(pos));
    }
}
