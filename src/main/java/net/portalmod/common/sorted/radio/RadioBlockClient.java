package net.portalmod.common.sorted.radio;

import net.minecraft.client.Minecraft;
import net.portalmod.core.init.SoundInit;

import java.util.HashMap;
import java.util.Map;

public class RadioBlockClient {
    protected static final Map<RadioBlockTileEntity, RadioSound> RADIOS = new HashMap<>();

    protected static void handlePacket(RadioBlockTileEntity be, RadioState state) {
        var manager = Minecraft.getInstance().getSoundManager();
        var previous = RADIOS.remove(be);
        if (previous != null) {
            previous.stopPlaying();
            manager.stop(previous);
        }
        if (!state.isPlaying()) {
            return;
        }
        var sound =
                new RadioSound(
                        be.getBlockPos(),
                        state == RadioState.ACTIVE ? SoundInit.RADIO_DINOSAUR1.get() : SoundInit.RADIO_LOOP.get(),
                        state == RadioState.ON);
        RADIOS.put(be, sound);
        manager.play(sound);
    }
}
