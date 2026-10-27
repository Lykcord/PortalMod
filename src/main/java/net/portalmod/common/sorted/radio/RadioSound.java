package net.portalmod.common.sorted.radio;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

public class RadioSound extends AbstractTickableSoundInstance {
    public RadioSound(BlockPos pos, SoundEvent sound, boolean looping) {
        super(sound, SoundSource.RECORDS, RandomSource.create());
        x = pos.getX() + .5;
        y = pos.getY() + .5;
        z = pos.getZ() + .5;
        this.looping = looping;
        this.volume = .5f;
    }

    @Override
    public void tick() {}

    public void stopPlaying() {
        // Keep stop effective when the native audio buffer finishes loading asynchronously.
        stop();
    }
}
