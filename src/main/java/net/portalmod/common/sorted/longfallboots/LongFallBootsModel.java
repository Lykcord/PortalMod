package net.portalmod.common.sorted.longfallboots;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;

public final class LongFallBootsModel extends Model<HumanoidRenderState> {
    public LongFallBootsModel(ModelPart root) {
        super(root, RenderTypes::armorCutoutNoCull);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("left_leg", shoe(false), PartPose.offset(2, 12, 0));
        mesh.getRoot().addOrReplaceChild("right_leg", shoe(true), PartPose.offset(-2, 12, 0));
        return LayerDefinition.create(mesh, 32, 16);
    }

    private static CubeListBuilder shoe(boolean mirror) {
        CubeDeformation inflation = new CubeDeformation(0.75F);
        return CubeListBuilder.create()
                .mirror(mirror)
                .texOffs(0, 0)
                .addBox(-2, 0, -2, 4, 12, 4, inflation)
                .texOffs(16, 7)
                .addBox(-2, 4, 3.5F, 4, 7, 2, inflation);
    }
}
