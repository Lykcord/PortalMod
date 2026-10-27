package net.portalmod.common.sorted.cube;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;

public class CubeModel extends EntityModel<CubeRenderState> {
    public CubeModel() {
        super(createLayer().bakeRoot());
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot()
                .addOrReplaceChild(
                        "cube",
                        CubeListBuilder.create()
                                .texOffs(0, 26)
                                .addBox(-6, -12.5F, -6, 12, 12, 12)
                                .texOffs(0, 0)
                                .addBox(-6.5F, -13, -6.5F, 13, 13, 13),
                        PartPose.offset(0, 24, 0));
        return LayerDefinition.create(mesh, 64, 64);
    }
}
