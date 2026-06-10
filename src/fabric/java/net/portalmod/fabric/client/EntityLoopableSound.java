package net.portalmod.fabric.client;

import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

/**
 * Entity-attached looping sound, used for the portal gun's hold hum while carrying.
 */
public final class EntityLoopableSound extends EntityBoundSoundInstance {
    public EntityLoopableSound(Entity entity, SoundEvent sound, SoundSource source) {
        super(sound, source, 1.0F, 1.0F, entity, entity.level().getRandom().nextLong());
        this.looping = true;
    }
}
