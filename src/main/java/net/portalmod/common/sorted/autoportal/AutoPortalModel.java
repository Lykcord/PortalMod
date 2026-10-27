package net.portalmod.common.sorted.autoportal;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;

public final class AutoPortalModel {
    public final ModelPart wawas;
    public final ModelPart frame;

    public AutoPortalModel() {
        var root = createLayer().bakeRoot();
        wawas = root.getChild("lights");
        frame = root.getChild("frame");
    }

    public static LayerDefinition createLayer() {
        var mesh = new MeshDefinition();
        var lights = CubeListBuilder.create();
        int[] ys = {-14, -9, -4, 2, 7, 12};
        int[] vs = {1, 4, 7, 7, 10, 13};
        for (int i = 0; i < ys.length; i++) {
            lights.texOffs(17, vs[i]).mirror().addBox(7, ys[i], 0, 4, 2, 0);
            lights.texOffs(17, vs[i]).mirror(false).addBox(-11, ys[i], 0, 4, 2, 0);
        }
        mesh.getRoot().addOrReplaceChild("lights", lights, PartPose.offset(0, 24, 0));
        mesh.getRoot()
                .addOrReplaceChild(
                        "frame",
                        CubeListBuilder.create()
                                .texOffs(1, 1)
                                .addBox(-2, -32, -1.05F, 2, 32, 1)
                                .texOffs(8, 1)
                                .addBox(0, -32, -1.05F, 1, 32, 1)
                                .texOffs(1, 1)
                                .mirror()
                                .addBox(22, -32, -1.05F, 2, 32, 1)
                                .texOffs(8, 1)
                                .addBox(21, -32, -1.05F, 1, 32, 1),
                        PartPose.offset(-11, 40, 1.05F));
        return LayerDefinition.create(mesh, 64, 64);
    }
}
