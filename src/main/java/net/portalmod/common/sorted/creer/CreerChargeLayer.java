package net.portalmod.common.sorted.creer;

import net.minecraft.client.model.monster.creeper.CreeperModel;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EnergySwirlLayer;
import net.minecraft.client.renderer.entity.state.CreeperRenderState;
import net.minecraft.resources.Identifier;

public class CreerChargeLayer extends EnergySwirlLayer<CreeperRenderState, CreeperModel> {
    private static final Identifier POWER_LOCATION = Identifier.parse("textures/entity/creeper/creeper_armor.png");
    private final CreerModel model = new CreerModel(2);

    public CreerChargeLayer(RenderLayerParent<CreeperRenderState, CreeperModel> parent) {
        super(parent);
    }

    @Override
    protected boolean isPowered(CreeperRenderState state) {
        return state.isPowered;
    }

    @Override
    protected float xOffset(float time) {
        return time * .01f;
    }

    @Override
    protected Identifier getTextureLocation() {
        return POWER_LOCATION;
    }

    @Override
    protected CreeperModel model() {
        return model;
    }
}
