package net.portalmod.common.sorted.cube;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.portalmod.common.entities.TestElementEntityRenderer;

public abstract class CubeRenderer extends TestElementEntityRenderer<Cube, CubeRenderState, CubeModel> {
    protected CubeRenderer(EntityRendererProvider.Context context) {
        super(context, new CubeModel(), .5F);
    }

    @Override
    public CubeRenderState createRenderState() {
        return new CubeRenderState();
    }

    @Override
    public void extractRenderState(Cube cube, CubeRenderState state, float partialTick) {
        super.extractRenderState(cube, state, partialTick);
        state.active = cube.isActive();
    }
}
