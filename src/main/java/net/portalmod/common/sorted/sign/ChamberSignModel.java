package net.portalmod.common.sorted.sign;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;

public class ChamberSignModel extends EntityModel<ChamberSignRenderState> {
    public ChamberSignModel() {
        super(createLayer().bakeRoot());
    }

    public static LayerDefinition createLayer() {
        var mesh = new MeshDefinition();
        var main =
                mesh.getRoot()
                        .addOrReplaceChild(
                                "main",
                                CubeListBuilder.create().texOffs(1, 1).addBox(-12, -24, -1, 24, 48, 2),
                                PartPose.offset(0, 24, 0));
        for (int i = 0; i <= 8; i++) {
            main.addOrReplaceChild(
                    "progress_" + i,
                    CubeListBuilder.create().texOffs(8, 51).addBox(-9, 4, -1.01F, 1 + i * 2, 2, 1),
                    PartPose.ZERO);
        }
        for (int i = 0; i < 10; i++) {
            int u = 53 + i % 5 * 9;
            int v = i > 4 ? 20 : 0;
            main.addOrReplaceChild(
                    "left_" + i,
                    CubeListBuilder.create().texOffs(u, v).addBox(-9, -17, -1.01F, 8, 18, 1),
                    PartPose.ZERO);
            main.addOrReplaceChild(
                    "right_" + i,
                    CubeListBuilder.create().texOffs(u, v).addBox(-1, -17, -1.01F, 8, 18, 1),
                    PartPose.ZERO);
        }
        int[] xs = {-8, -4, 1, 5};
        for (int i = 0; i < 8; i++) {
            for (int enabled = 0; enabled < 2; enabled++) {
                main.addOrReplaceChild(
                        "icon_" + i + "_" + enabled,
                        CubeListBuilder.create()
                                .texOffs(1, enabled == 1 ? 51 : 55)
                                .addBox(xs[i % 4], i < 4 ? 11 : 15, -1.01F, 3, 3, 1),
                        PartPose.ZERO);
            }
        }
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void setupAnim(ChamberSignRenderState state) {
        super.setupAnim(state);
        var main = root().getChild("main");
        for (int i = 0; i <= 8; i++) {
            main.getChild("progress_" + i).visible = i == Math.min(state.progress, 8);
        }
        for (int i = 0; i < 10; i++) {
            main.getChild("left_" + i).visible = i == Math.floorMod(state.leftDigit, 10);
            main.getChild("right_" + i).visible = i == Math.floorMod(state.rightDigit, 10);
        }
        for (int i = 0; i < 8; i++) {
            boolean enabled = (state.icons & 1 << i) != 0;
            main.getChild("icon_" + i + "_1").visible = enabled;
            main.getChild("icon_" + i + "_0").visible = !enabled;
        }
    }
}
