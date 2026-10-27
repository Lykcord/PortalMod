package net.portalmod.common.sorted.portal;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.portalmod.common.particles.PortalParticleOptions;

public class PortalParticle extends SingleQuadParticle {
    private final int u, v;

    protected PortalParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            double xd,
            double yd,
            double zd,
            PortalParticleOptions data,
            SpriteSet sprites) {
        super(level, x, y, z, xd, yd, zd, sprites.first());
        lifetime = (int) (Math.random() * 10) + 10;
        gravity = 1;
        setSpriteFromAge(sprites);
        quadSize /= 2;
        u = random.nextInt(4);
        v = random.nextInt(4);
        rCol = data.r();
        gCol = data.g();
        bCol = data.b();
        alpha = data.a();
    }

    @Override
    public void tick() {
        super.tick();
        alpha = Mth.clamp((float) Math.pow((lifetime - age) / 7F, 2), 0, 1);
        oRoll = roll;
        roll = -((float) Math.atan2(yd, xd) - (float) Math.PI / 2);
        if (age == 1) {
            oRoll = roll;
        }
    }

    public static void spawnBurst(ClientLevel level, Vec3 pos, Direction direction) {
        for (int i = 0; i < 40; i++) {
            level.addParticle(new PortalParticleOptions(0, 0, 1, 1, direction), pos.x, pos.y, pos.z, 0, 0, 0);
        }
    }

    @Override
    protected float getU0() {
        return sprite.getU(u * .25F);
    }

    @Override
    protected float getV0() {
        return sprite.getV(v * .25F);
    }

    @Override
    protected float getU1() {
        return sprite.getU((u + 1) * .25F);
    }

    @Override
    protected float getV1() {
        return sprite.getV((v + 1) * .25F);
    }

    @Override
    protected Layer getLayer() {
        return Layer.TRANSLUCENT;
    }

    public static class Factory implements ParticleProvider<PortalParticleOptions> {
        private final SpriteSet sprites;

        public Factory(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(
                PortalParticleOptions data,
                ClientLevel level,
                double x,
                double y,
                double z,
                double xd,
                double yd,
                double zd,
                RandomSource random) {
            return new PortalParticle(level, x, y, z, xd, yd, zd, data, sprites);
        }
    }
}
