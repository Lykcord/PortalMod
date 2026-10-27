package net.portalmod.common.sorted.portal;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.portalmod.core.math.Vec3;

public class PortalEntityRenderer extends EntityRenderer<PortalEntity, EntityRenderState> {
    public static final float OFFSET = .0001f;

    public PortalEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }

    @Override
    protected boolean shouldShowName(PortalEntity portal, double distance) {
        return portal.hasCustomName();
    }

    @Override
    public void extractRenderState(PortalEntity portal, EntityRenderState state, float partialTick) {
        super.extractRenderState(portal, state, partialTick);
        if (state.nameTagAttachment != null) {
            var down = new Vec3(portal.getUpVector().getStep()).mul(-1).to3d();
            var forward = new Vec3(portal.getDirection().getStep()).mul(.2).to3d();
            state.nameTagAttachment = state.nameTagAttachment.add(down).add(forward);
        }
    }
}
