package net.portalmod.common.sorted.portalgun;

import static org.junit.Assert.*;

import com.mojang.blaze3d.vertex.PoseStack;

import org.junit.jupiter.api.Test;

import java.util.UUID;

public class PortalGunModelTest {
    private static class TimedModel extends PortalGunModel {
        long now;

        @Override
        protected long getMillis() {
            return now;
        }
    }

    @Test
    public void gunRetainsSeventeenBoxesIncludingPotatoAndThreeTintGroups() {
        var model = new TimedModel();
        int[] count = {0};
        model.root().visit(new PoseStack(), (pose, path, index, cube) -> count[0]++);
        assertEquals(17, count[0]);
        assertTrue(model.root().hasChild("gun"));
        assertTrue(model.root().hasChild("colour"));
        assertTrue(model.root().hasChild("stripes"));
        assertTrue(model.gun.getChild("gun_front").getChild("prong_top").getChild("potato_parent").hasChild("potato"));
    }

    @Test
    public void extractingPoseDoesNotMutateModelAndSnapshotSurvivesLaterAnimation() {
        var model = new TimedModel();
        var uuid = UUID.randomUUID();
        var front = model.gun.getChild("gun_front");
        model.startAnimation(uuid, "shoot");
        model.now = 100;
        var shot = model.extractAnimation(uuid, true);
        assertEquals(0, front.z, 0);
        assertEquals(-1.5708F, front.getChild("prong_right").xRot, .00001F);
        model.setupAnim(shot);
        assertEquals(3, front.z, .00001F);
        assertEquals(3, model.colour.getChild("colour_front").z, .00001F);
        assertEquals(-2.2689F, front.getChild("prong_left").xRot, .00001F);
        model.startAnimation(uuid, "drop");
        model.now = 500;
        model.setupAnim(model.extractAnimation(uuid, true));
        assertEquals(0, front.z, .00001F);
        model.setupAnim(shot);
        assertEquals(3, front.z, .00001F);
        model.setupAnim(model.extractAnimation(uuid, false));
        assertEquals(0, front.z, 0);
        assertEquals(-1.5708F, front.getChild("prong_left").xRot, .00001F);
    }

    @Test
    public void deferredTintLayersReplayTheirOwnPoseAfterAnotherItem() {
        var model = new TimedModel();
        var uuid = UUID.randomUUID();
        model.startAnimation(uuid, "shoot");
        model.now = 100;
        var shot = model.extractAnimation(uuid, true);
        var idle = model.extractAnimation(null, false);
        model.bodyLayer.setupAnim(shot);
        assertEquals(3, model.gun.getChild("gun_front").z, .00001F);
        model.colourLayer.setupAnim(idle);
        assertEquals(0, model.colour.getChild("colour_front").z, 0);
        model.colourLayer.setupAnim(shot);
        assertEquals(3, model.colour.getChild("colour_front").z, .00001F);
        model.stripeLayer.setupAnim(idle);
        assertEquals(0, model.gun.getChild("gun_front").z, 0);
    }
}
