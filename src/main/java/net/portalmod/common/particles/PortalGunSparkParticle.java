package net.portalmod.common.particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.portalmod.core.init.ParticleInit;

import java.util.Random;

public class PortalGunSparkParticle extends SingleQuadParticle {

    public static final Random RANDOM = new Random();

    public int u;
    public int v;

    protected PortalGunSparkParticle(
            ClientLevel world,
            double x,
            double y,
            double z,
            double dx,
            double dy,
            double dz,
            TextureAtlasSprite sprite) {
        super(world, x, y, z, dx, dy, dz, sprite);
        this.lifetime = 2;
        this.gravity = 0;
        this.xd = dx;
        this.yd = dy;
        this.zd = dz;
        this.quadSize = quadSize * 0.65f;

        this.u = RANDOM.nextInt(4);
        this.v = RANDOM.nextInt(4);
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

        if (this.age == 1) {
            this.setAlpha(0.5f);
        }

        super.tick();
    }

    @Override
    protected float getU0() {
        return this.sprite.getU(this.u * .25F);
    }

    @Override
    protected float getU1() {
        return this.sprite.getU((this.u + 1) * .25F);
    }

    @Override
    protected float getV0() {
        return this.sprite.getV(this.v * .25F);
    }

    @Override
    protected float getV1() {
        return this.sprite.getV((this.v + 1) * .25F);
    }

    public static void createParticles(Level world, Player player, boolean offhand) {
        if (!(world instanceof ClientLevel client)) {
            return;
        }
        boolean isLefty = player.getMainArm() == HumanoidArm.LEFT;

        Vec3 viewVec = player.getViewVector(1);
        Vec3 rightVec = Vec3.directionFromRotation(0, player.yHeadRot + 90); // Screen right
        Vec3 downVec = viewVec.cross(rightVec); // Screen down

        Vec3 viewLocation =
                player.getEyePosition(1)
                        .add(viewVec.scale(0.8)) // Forward
                        .add(rightVec.scale(0.45).scale(offhand == isLefty ? 1 : -1)) // Right
                        .add(downVec.scale(0.3)); // Down

        int amount = RANDOM.nextInt(3) + 1; // 1 - 3
        double speed = 0.07;
        double spread = 0.2;

        for (int i = 0; i < amount; i++) {
            Vec3 offset = new Vec3(symmetricRandom(spread), symmetricRandom(spread), symmetricRandom(spread));
            Vec3 particlePos = viewLocation.add(offset);

            client.addParticle(
                    ParticleInit.PORTALGUN_SPARK.get(),
                    particlePos.x,
                    particlePos.y,
                    particlePos.z,
                    symmetricRandom(speed),
                    symmetricRandom(speed),
                    symmetricRandom(speed));
        }
    }

    public static double symmetricRandom(double i) {
        return (RANDOM.nextFloat() - 0.5) * 2 * i;
    }

    public static class Factory implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public Factory(SpriteSet sprites) {
            this.sprite = sprites;
        }

        @Override
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
            return new PortalGunSparkParticle(world, x, y, z, dx, dy, dz, this.sprite.get(random));
        }
    }
}
