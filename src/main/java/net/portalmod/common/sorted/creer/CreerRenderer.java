package net.portalmod.common.sorted.creer;

import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.entity.CreeperRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.CreeperRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Creeper;

public class CreerRenderer extends CreeperRenderer {
    public CreerRenderer(EntityRendererProvider.Context context) {
        super(context);
        model = new CreerModel(0);
        layers.clear();
        addLayer(new CreerChargeLayer(this));
    }

    @Override
    public CreeperRenderState createRenderState() {
        return new CreerRenderState();
    }

    @Override
    public void extractRenderState(Creeper entity, CreeperRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        ((CreerRenderState) state).isCreer = isCreer(entity);
    }

    public static boolean isCreer(Entity entity) {
        return entity instanceof Creeper
                && entity.hasCustomName()
                && "creer".equalsIgnoreCase(ChatFormatting.stripFormatting(entity.getName().getString()));
    }
}
