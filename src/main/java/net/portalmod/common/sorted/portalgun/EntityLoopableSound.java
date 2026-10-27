package net.portalmod.common.sorted.portalgun;

import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

public class EntityLoopableSound extends EntityBoundSoundInstance {
    public EntityLoopableSound(Entity entity, SoundEvent sound, SoundSource category) {
        super(sound, category, 1, 1, entity, entity.level().getRandom().nextLong());
        this.looping = true;
    }
}
