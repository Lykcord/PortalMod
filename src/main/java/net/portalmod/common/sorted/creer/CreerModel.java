package net.portalmod.common.sorted.creer;

import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.monster.creeper.CreeperModel;
import net.minecraft.client.renderer.entity.state.CreeperRenderState;

public class CreerModel extends CreeperModel {
    public CreerModel(float inflation) {
        super(CreeperModel.createBodyLayer(new CubeDeformation(inflation)).bakeRoot());
    }

    @Override
    public void setupAnim(CreeperRenderState state) {
        super.setupAnim(state);
        boolean creer = state instanceof CreerRenderState custom && custom.isCreer;
        root().getChild("body").visible = !creer;
        if (creer) {
            root().getChild("head").y = 18;
        }
    }
}
