package net.portalmod.common.particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.portalmod.core.init.ParticleInit;

import java.util.Random;

public class TurretSparkParticle extends SingleQuadParticle {

    public int u;
    public int v;

    protected TurretSparkParticle(
            ClientLevel world,
            double x,
            double y,
            double z,
            double dx,
            double dy,
            double dz,
            TextureAtlasSprite sprite) {
        super(world, x, y, z, 0, 0, 0, sprite);
        this.lifetime = 3;
        this.gravity = 0;
    }

    @Override
    protected Layer getLayer() {
        return Layer.TRANSLUCENT;
    }

    @Override
    protected int getLightCoords(float partialTick) {
        return 15728880;
    }

    @Override
    public void tick() {
        if (this.age++ >= this.lifetime) {
            this.remove();
        }

        this.u = this.age % 2;
        this.v = (int) Math.floor((double) this.age / 2);
    }

    @Override
    protected float getU0() {
        return this.sprite.getU(this.u * .5F);
    }

    @Override
    protected float getU1() {
        return this.sprite.getU((this.u + 1) * .5F);
    }

    @Override
    protected float getV0() {
        return this.sprite.getV(this.v * .5F);
    }

    @Override
    protected float getV1() {
        return this.sprite.getV((this.v + 1) * .5F);
    }

    public static void createGlowParticles(Level world, LivingEntity entity, Vec3 direction) {
        if (!(world instanceof ClientLevel client)) {
            return;
        }
        double heightMid = entity.getBbHeight() * 0.5;
        Vec3 middle = entity.position().add(0, heightMid, 0);

        Vec3 front = middle.add(direction.normalize().multiply(0.25F, 0F, 0.25F));

        Vec3 side = new Vec3(-direction.normalize().z, 0, direction.normalize().x);

        long tickTime = world.getDefaultClockTime() + entity.getId();
        boolean leftSide = tickTime % 8 < 4;
        boolean upperSide = tickTime % 4 < 2;

        float verticalOffset = upperSide ? 0.12F : 0;
        side = side.multiply(leftSide ? -0.25F : 0.25F, 0, leftSide ? -0.25F : 0.25F);

        Vec3 particlePos = front.add(0F, 0.08F, 0F);

        client.addParticle(
                ParticleInit.TURRET_SPARK.get(),
                particlePos.x + side.x,
                particlePos.y + verticalOffset,
                particlePos.z + side.z,
                0,
                0,
                0);
    }

    public static double random(double i) {
        return (new Random().nextFloat() - 0.5) * 2 * i;
    }

    public static class Factory implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public Factory(SpriteSet sprites) {
            this.sprite = sprites;
        }

        public Particle createParticle(
                SimpleParticleType type,
                ClientLevel world,
                double x,
                double y,
                double z,
                double dx,
                double dy,
                double dz,
                RandomSource random) {
            TurretSparkParticle particle = new TurretSparkParticle(world, x, y, z, 0, 0, 0, this.sprite.first());
            particle.setSpriteFromAge(this.sprite);
            return particle;
        }
    }
}
