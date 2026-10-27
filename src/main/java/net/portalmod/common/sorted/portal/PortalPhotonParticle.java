package net.portalmod.common.sorted.portal;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.portalmod.common.particles.PortalPhotonOptions;
import net.portalmod.core.math.Mat4;
import net.portalmod.core.math.Vec3;

import java.awt.*;
import java.util.Random;

public class PortalPhotonParticle extends SingleQuadParticle {
    private final PortalEntity portal;
    private final double x0, y0, z0;
    private final double x1, y1, z1;
    private final float speed;
    private final float decay;
    private final float end;
    private final boolean smooth;
    private final boolean useGravity;

    protected PortalPhotonParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            double xd,
            double yd,
            double zd,
            PortalPhotonOptions data,
            SpriteSet sprite) {
        super(level, x, y, z, xd, yd, zd, sprite.first());

        this.portal = level.getEntity(data.portalId()) instanceof PortalEntity portal ? portal : null;

        this.x0 = x;
        this.y0 = y;
        this.z0 = z;

        this.useGravity = data.useGravity();

        if (this.useGravity) {
            this.x1 = 0;
            this.y1 = 0;
            this.z1 = 0;
            this.xd = xd;
            this.yd = yd;
            this.zd = zd;
            this.gravity = 0.08f;
        } else {
            this.x1 = xd;
            this.y1 = yd;
            this.z1 = zd;
            this.xd = 0;
            this.yd = 0;
            this.zd = 0;
            this.gravity = 0;
        }

        this.speed = data.speed();
        this.decay = data.decay();
        this.end = this.random.nextFloat() * 0.1f;
        this.smooth = data.smooth();
        this.calculateAlpha();

        this.lifetime = 20;

        this.setSpriteFromAge(sprite);
        this.quadSize = 1 / 32f - (float) this.random.nextGaussian() * 0.005f;

        this.rCol = data.r();
        this.gCol = data.g();
        this.bCol = data.b();
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime || this.portal != null && !this.portal.isAlive()) {
            this.remove();
        }

        this.calculateAlpha();
        this.calculatePos();
    }

    private void calculateAlpha() {
        this.alpha = Mth.clamp((float) Math.pow((this.lifetime - this.age) / decay, 2), 0, 0.8f);
        if (this.smooth) {
            this.alpha *= 1 - (float) Math.exp(-this.age / 7f);
        }

        if (this.portal != null) {
            this.alpha *= Mth.clamp((float) this.portal.getAge() / 30f * this.portal.getAge() / 30f, 0f, 1f);
        }
    }

    private void calculatePos() {
        if (this.useGravity) {
            this.yd -= 1.2 * this.gravity;
            this.move(this.xd, this.yd, this.zd);

            this.xd *= 0.85F;
            this.yd *= 0.9F;
            this.zd *= 0.85F;

            if (this.onGround) {
                this.yd *= -1;
            }

        } else {
            double f = 1 - Math.exp(-this.age / this.speed) - this.end;
            this.setPos(Mth.lerp(f, this.x0, this.x1), Mth.lerp(f, this.y0, this.y1), Mth.lerp(f, this.z0, this.z1));
        }
    }

    @Override
    protected int getLightCoords(float partialTick) {
        return LightCoordsUtil.FULL_BRIGHT;
    }

    public static void createLivingParticles(PortalEntity portal) {
        Random random = new Random();

        for (int i = 0; i < 360; i += 20) {
            boolean doesRender = random.nextInt(100) < 30;
            if (!doesRender) {
                continue;
            }

            int deltaAngle = 40 * (portal.getEnd() == PortalEnd.PRIMARY ? 1 : -1);
            float randomAngle = (random.nextFloat() - .5f) * 4;
            float startAngle = (i + randomAngle) * (float) Math.PI / 180;
            float endAngle = (i + deltaAngle + randomAngle) * (float) Math.PI / 180;

            float randomRadius = (random.nextFloat() - .5f) * 0.1f;
            float startRadius = 0.55f + randomRadius;
            float endRadius = 0.45f + randomRadius;

            createRadialParticle(
                    portal,
                    random,
                    startRadius,
                    endRadius,
                    startAngle,
                    endAngle,
                    random.nextFloat() * 2 + 15,
                    random.nextFloat() * 18 + 15,
                    true,
                    true);
        }
    }

    public static void createOpeningParticles(PortalEntity portal) {
        Random random = new Random();

        for (int i = 0; i < 360; i += 10) {
            for (int j = 0; j < 3; j++) {
                float randomAngle = (random.nextFloat() - .5f) * 4;
                float angle = (i + randomAngle) * (float) Math.PI / 180;

                float innerRadius = 0.2f;
                float outerRadius = 0.45f;
                float randomRadius = (random.nextFloat() - .5f) * 0.1f;
                float startRadius = 0;
                float endRadius = (outerRadius - innerRadius) * (j / 2f) + innerRadius + randomRadius;

                int decayBase = 0;
                switch (j) {
                    case 2:
                        decayBase = 5;
                        break;
                    case 1:
                        decayBase = 10;
                        break;
                    case 0:
                        decayBase = 22;
                        break;
                }

                createRadialParticle(
                        portal,
                        random,
                        startRadius,
                        endRadius,
                        angle,
                        angle,
                        random.nextFloat() + 2,
                        random.nextFloat() * 18 + decayBase,
                        false,
                        false);
            }
        }
    }

    public static void createClosingParticles(PortalEntity portal) {
        Random random = new Random();

        for (int i = 0; i < 360; i += 20) {
            float randomAngle = (random.nextFloat() - .5f) * 4;
            float angle = (i + randomAngle) * (float) Math.PI / 180;

            float randomRadius = (random.nextFloat() - .5f) * 0.1f;
            float startRadius = 0.45f + randomRadius;
            float endRadius = 0.1f + randomRadius;

            createRadialParticle(
                    portal,
                    random,
                    startRadius,
                    endRadius,
                    angle,
                    angle,
                    random.nextFloat() * 2 + 4,
                    random.nextFloat() * 18 + 30,
                    false,
                    false);
        }
    }

    public static void createFailParticles(Level level, Vec3 position, Vec3 normal, Vec3 upVector, String hue) {
        Random random = new Random();

        for (int i = 0; i < 360; i += 10) {
            float randomAngle = (random.nextFloat() - .5f) * 4;
            float angle = (i + randomAngle) * (float) Math.PI / 180;

            Color color = PortalColors.getColor(hue);
            Mat4 modelMatrix =
                    Mat4.identity()
                            .mul(
                                    new OrthonormalBasis(upVector.clone().cross(normal), upVector)
                                            .getChangeOfBasisFromCanonicalMatrix());

            if (color == null) {
                return;
            }

            float x0 = 0;
            float y0 = 0;
            float x1 = (float) Math.cos(angle);
            float y1 = (float) Math.sin(angle);

            Vec3 pos0 = new Vec3(x0, y0, 0).transform(modelMatrix).add(position);
            Vec3 pos1 = new Vec3(x1, y1, .7).transform(modelMatrix);

            float randomRadius = random.nextFloat() * 0.3f;
            pos1.x *= 0.1 + randomRadius;
            pos1.y *= 0.4 + randomRadius;
            pos1.z *= 0.1 + randomRadius;

            createParticle(
                    level,
                    null,
                    random,
                    pos0,
                    pos1,
                    color,
                    random.nextFloat() * 2 + 4,
                    random.nextFloat() * 18 + 15,
                    false,
                    true);
        }
    }

    private static void createRadialParticle(
            PortalEntity portal,
            Random random,
            float startRadius,
            float endRadius,
            float startAngle,
            float endAngle,
            float speed,
            float decay,
            boolean smooth,
            boolean linked) {
        Mat4 modelMatrix = portal.getSourceBasis().getChangeOfBasisFromCanonicalMatrix();
        Color color = PortalColors.getColor(portal);

        if (portal.level() == null || color == null) {
            return;
        }

        float x0 = (float) Math.cos(startAngle);
        float y0 = (float) Math.sin(startAngle);
        float x1, y1;

        if (startAngle == endAngle) {
            x1 = x0;
            y1 = y0;
        } else {
            x1 = (float) Math.cos(endAngle);
            y1 = (float) Math.sin(endAngle);
        }

        x0 *= startRadius;
        y0 *= startRadius * 2;

        x1 *= endRadius;
        y1 *= endRadius * 2;

        boolean outwards = endRadius > startRadius;

        Vec3 pos0 = new Vec3(x0, y0, outwards ? 0 : 0.1).transform(modelMatrix).add(portal.position());
        Vec3 pos1 = new Vec3(x1, y1, outwards ? 0.1 : 0).transform(modelMatrix).add(portal.position());

        createParticle(portal.level(), linked ? portal : null, random, pos0, pos1, color, speed, decay, smooth, false);
    }

    private static void createParticle(
            Level level,
            PortalEntity portal,
            Random random,
            Vec3 pos0,
            Vec3 pos1,
            Color color,
            float speed,
            float decay,
            boolean smooth,
            boolean useGravity) {
        if (!(level instanceof ClientLevel client)) {
            return;
        }
        float randomR = random.nextFloat() * 0.1f;
        float randomG = random.nextFloat() * 0.1f;
        float randomB = random.nextFloat() * 0.1f;

        client.addParticle(
                new PortalPhotonOptions(
                        portal == null ? -1 : portal.getId(),
                        Mth.clamp(color.getRed() / 255f + randomR, 0, 1),
                        Mth.clamp(color.getGreen() / 255f + randomG, 0, 1),
                        Mth.clamp(color.getBlue() / 255f + randomB, 0, 1),
                        speed,
                        decay,
                        smooth,
                        useGravity),
                pos0.x,
                pos0.y,
                pos0.z,
                pos1.x,
                pos1.y,
                pos1.z);
    }

    @Override
    protected float getU0() {
        return this.sprite.getU(0);
    }

    @Override
    protected float getV0() {
        return this.sprite.getV(0);
    }

    @Override
    protected float getU1() {
        return this.sprite.getU(.125F);
    }

    @Override
    protected float getV1() {
        return this.sprite.getV(.125F);
    }

    @Override
    protected Layer getLayer() {
        return Layer.TRANSLUCENT;
    }

    public static class Factory implements ParticleProvider<PortalPhotonOptions> {
        private final SpriteSet sprite;

        public Factory(SpriteSet sprite) {
            this.sprite = sprite;
        }

        public Particle createParticle(
                PortalPhotonOptions data,
                ClientLevel level,
                double x,
                double y,
                double z,
                double xd,
                double yd,
                double zd,
                RandomSource random) {
            return new PortalPhotonParticle(level, x, y, z, xd, yd, zd, data, sprite);
        }
    }
}
