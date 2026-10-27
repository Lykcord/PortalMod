package net.portalmod.client;

import static org.junit.Assert.*;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.SharedConstants;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.phys.Vec3;
import net.portalmod.common.sorted.autoportal.AutoPortalModel;
import net.portalmod.common.sorted.creer.CreerModel;
import net.portalmod.common.sorted.creer.CreerRenderState;
import net.portalmod.common.sorted.cube.CubeModel;
import net.portalmod.common.sorted.faithplate.FaithPlatePlateModel;
import net.portalmod.common.sorted.faithplate.FaithPlateRenderState;
import net.portalmod.common.sorted.faithplate.FaithPlateTargetGeometry;
import net.portalmod.common.sorted.longfallboots.LongFallBootsModel;
import net.portalmod.common.sorted.sign.ChamberSignModel;
import net.portalmod.common.sorted.sign.ChamberSignRenderState;
import net.portalmod.common.sorted.turret.TurretModel;
import net.portalmod.common.sorted.turret.TurretRenderState;
import net.portalmod.common.sorted.turret.TurretState;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class ModelGeometryTest {
    @Test
    public void creerRetainsShortHeadAndLegsAndRestoresNormalCreepersIncludingArmor() {
        for (float inflation : new float[] {0, 2}) {
            var model = new CreerModel(inflation);
            var state = new CreerRenderState();
            state.isCreer = true;
            model.setupAnim(state);
            assertFalse(model.root().getChild("body").visible);
            assertEquals(18, model.root().getChild("head").y, 0);
            assertEquals(18, model.root().getChild("right_hind_leg").y, 0);
            state.isCreer = false;
            model.setupAnim(state);
            assertTrue(model.root().getChild("body").visible);
            assertEquals(6, model.root().getChild("head").y, 0);
        }
    }

    @BeforeAll
    public static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    public void faithPlateRetainsGeometryAndResetsBetweenAnimatedAndIdlePlates() {
        var model = new FaithPlatePlateModel();
        assertEquals(6, cubes(model.root()).size());
        var bone = model.root().getChild("bone");
        var arm = bone.getChild("arm");
        assertEquals(29.5F, bone.y, 0);
        var state = new FaithPlateRenderState();
        state.animationMillis = 150;
        model.setupAnim(state);
        assertEquals(.1F, bone.xRot, .00001F);
        assertEquals(-.3F, arm.xRot, .00001F);
        assertEquals(-.7F, arm.getChild("plate").xRot, .00001F);
        assertEquals(-1.2F, arm.getChild("lock").xRot, .00001F);
        state.animationMillis = -1;
        model.setupAnim(state);
        assertEquals(-.2182F, bone.xRot, .00001F);
        assertEquals(.3491F, arm.xRot, .00001F);
        assertEquals(.2618F, arm.getChild("plate").xRot, .00001F);
        assertEquals(-.1309F, arm.getChild("lock").xRot, .00001F);
    }

    @Test
    public void faithPlateTargetsKeepOutwardWindingOnEveryFace() {
        for (var face : Direction.values()) {
            var quad = FaithPlateTargetGeometry.vertices(face, .01F);
            var normal = quad[2].subtract(quad[1]).cross(quad[0].subtract(quad[1])).normalize();
            assertEquals(1, normal.dot(Vec3.atLowerCornerOf(face.getStep())), .00001);
            for (var point : quad) {
                double coordinate = face.getAxis().choose(point.x, point.y, point.z);
                assertEquals(
                        face.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1.01 : -.01, coordinate, .00001);
            }
        }
    }

    @Test
    public void autoPortalKeepsTwelveLightsAndFourFramePieces() {
        var model = new AutoPortalModel();
        assertEquals(12, cubes(model.wawas).size());
        assertEquals(4, cubes(model.frame).size());
        assertEquals(24, model.wawas.y, 0);
        assertEquals(-11, model.frame.x, 0);
        assertEquals(40, model.frame.y, 0);
        assertEquals(1.05F, model.frame.z, 0);
        assertEquals(32, cubes(model.frame).getFirst().maxY - cubes(model.frame).getFirst().minY, 0);
    }

    @Test
    public void signSelectsDigitsProgressAndIconsWithoutRebuildingGeometry() {
        var model = new ChamberSignModel();
        var main = model.root().getChild("main");
        assertEquals(24, main.y, 0);
        var base = cubes(main).getFirst();
        assertEquals(24, base.maxX - base.minX, 0);
        assertEquals(48, base.maxY - base.minY, 0);
        var state = new ChamberSignRenderState();
        state.leftDigit = 9;
        state.rightDigit = 2;
        state.progress = 8;
        state.icons = 0b01010101;
        model.setupAnim(state);
        assertTrue(main.getChild("left_9").visible);
        assertFalse(main.getChild("left_0").visible);
        assertTrue(main.getChild("right_2").visible);
        assertTrue(main.getChild("progress_8").visible);
        assertFalse(main.getChild("progress_0").visible);
        assertTrue(main.getChild("icon_0_1").visible);
        assertTrue(main.getChild("icon_1_0").visible);
        state.leftDigit = 0;
        state.progress = -1;
        state.icons = 0;
        model.setupAnim(state);
        assertFalse(main.getChild("left_9").visible);
        assertTrue(main.getChild("left_0").visible);
        for (int i = 0; i <= 8; i++) {
            assertFalse(main.getChild("progress_" + i).visible);
        }
        assertFalse(main.getChild("icon_0_1").visible);
        assertTrue(main.getChild("icon_0_0").visible);
    }

    private static List<ModelPart.Cube> cubes(ModelPart root) {
        List<ModelPart.Cube> cubes = new ArrayList<>();
        root.visit(new PoseStack(), (pose, path, index, cube) -> cubes.add(cube));
        return cubes;
    }

    @Test
    public void cubeKeepsBothShellsAndOriginalDimensions() {
        ModelPart root = CubeModel.createLayer().bakeRoot();
        List<ModelPart.Cube> cubes = cubes(root);
        assertEquals(2, cubes.size());
        assertEquals(24, root.getChild("cube").y, 0);
        assertEquals(12, cubes.get(0).maxX - cubes.get(0).minX, 0);
        assertEquals(13, cubes.get(1).maxX - cubes.get(1).minX, 0);
        assertEquals(-13, cubes.get(1).minY, 0);
    }

    @Test
    public void bootsRetainBothSpringsAndHumanoidLegPaths() {
        ModelPart root = LongFallBootsModel.createLayer().bakeRoot();
        assertEquals(4, cubes(root).size());
        assertEquals(2, root.getChild("left_leg").x, 0);
        assertEquals(-2, root.getChild("right_leg").x, 0);
        assertEquals(12, root.getChild("left_leg").y, 0);
        assertEquals(2, cubes(root.getChild("left_leg")).size());
        assertEquals(2, cubes(root.getChild("right_leg")).size());
    }

    @Test
    public void turretRetainsGeometryAndResetsWingAnimation() {
        TurretModel model = new TurretModel();
        assertEquals(18, cubes(model.root()).size());
        assertEquals(14.5F, model.root().getChild("head").y, 0);
        assertEquals(.3927F, model.root().getChild("leg_back").xRot, .00001F);
        TurretRenderState state = new TurretRenderState();
        state.turretState = TurretState.SHOOTING;
        state.turretToTarget = new Vec3(0, 0, 1);
        state.recoil = .125F;
        model.setupAnim(state);
        assertEquals(2.5F, model.root().getChild("wing_left").x, 0);
        assertEquals(-2.5F, model.root().getChild("wing_right").x, 0);
        assertEquals(.125F, model.root().getChild("wing_left").xRot, .00001F);
        state.turretState = TurretState.RESTING;
        state.recoil = 0;
        model.setupAnim(state);
        assertEquals(.5F, model.root().getChild("wing_left").x, 0);
        assertEquals(0, model.root().getChild("wing_left").xRot, 0);
    }
}
