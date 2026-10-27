package net.portalmod.common.sorted.antline;

import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.portalmod.PortalMod;
import net.portalmod.core.init.BlockInit;
import net.portalmod.core.math.Mat4;
import net.portalmod.core.math.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public final class AntlineBakedModel implements BlockStateModel {
    private static final List<String> TEXTURES =
            List.of("active_dot", "active_corner", "inactive_dot", "inactive_corner");
    private final Map<String, Material.Baked> materials;

    private AntlineBakedModel(ModelBaker baker) {
        materials = new HashMap<>();
        for (String texture : TEXTURES) {
            materials.put(
                    texture,
                    baker.materials()
                            .get(
                                    new Material(
                                            Identifier.fromNamespaceAndPath(PortalMod.MODID, "antline/" + texture)),
                                    () -> "portalmod:antline"));
        }
    }

    public static void init() {
        ModelLoadingPlugin.register(
                plugin ->
                        plugin.registerBlockStateResolver(
                                BlockInit.ANTLINE.get(),
                                context -> {
                                    BlockStateModel.Unbaked model =
                                            new BlockStateModel.Unbaked() {
                                                @Override
                                                public BlockStateModel bake(ModelBaker baker) {
                                                    return new AntlineBakedModel(baker);
                                                }

                                                @Override
                                                public void resolveDependencies(ResolvableModel.Resolver resolver) {}
                                            };
                                    for (BlockState state : context.block().getStateDefinition().getPossibleStates()) {
                                        context.setModel(state, model.asRoot());
                                    }
                                }));
    }

    @Override
    public void emitQuads(
            QuadEmitter emitter,
            BlockAndTintGetter world,
            BlockPos pos,
            BlockState state,
            RandomSource random,
            Predicate<Direction> cullTest) {
        if (!(world.getBlockEntityRenderData(pos) instanceof AntlineTileEntity.SideMap sides)) {
            return;
        }
        sides.forEach(
                (direction, side) -> {
                    if (side.isEmpty()) {
                        return;
                    }
                    String prefix = side.isActive() ? "active_" : "inactive_";
                    addQuad(
                            emitter,
                            direction,
                            Direction.UP,
                            materials.get(
                                    prefix
                                            + (side.getSideType() == AntlineTileEntity.Side.SideType.NORMAL
                                                    ? "dot"
                                                    : "corner")));
                    side.getConnections()
                            .forEach(
                                    (connection, connected) -> {
                                        if (connected) {
                                            addQuad(emitter, direction, connection, materials.get(prefix + "dot"));
                                        }
                                    });
                });
    }

    private static void addQuad(QuadEmitter emitter, Direction side, Direction offset, Material.Baked material) {
        Vec3 y = new Vec3(side == Direction.UP ? side : side.getOpposite());
        Vec3 z = new Vec3(side.getAxis().isHorizontal() ? Direction.DOWN : Direction.SOUTH);
        Vec3 x = y.clone().cross(z);
        Mat4 basis = new Mat4(x.x, y.x, z.x, 0, x.y, y.y, z.y, 0, x.z, y.z, z.z, 0, 0, 0, 0, 1);
        Vec3 translation =
                offset.getAxis().isVertical() ? new Vec3(0, 0, 0) : new Vec3(offset).transform(basis).mul(5 / 16f);
        double min = .001, max = .999;
        Vec3[] vertices =
                switch (side) {
                    case UP ->
                            new Vec3[] {
                                new Vec3(max, max, min),
                                new Vec3(max, max, max),
                                new Vec3(min, max, max),
                                new Vec3(min, max, min)
                            };
                    case DOWN ->
                            new Vec3[] {
                                new Vec3(min, min, min),
                                new Vec3(min, min, max),
                                new Vec3(max, min, max),
                                new Vec3(max, min, min)
                            };
                    case EAST ->
                            new Vec3[] {
                                new Vec3(max, max, min),
                                new Vec3(max, min, min),
                                new Vec3(max, min, max),
                                new Vec3(max, max, max)
                            };
                    case WEST ->
                            new Vec3[] {
                                new Vec3(min, max, max),
                                new Vec3(min, min, max),
                                new Vec3(min, min, min),
                                new Vec3(min, max, min)
                            };
                    case NORTH ->
                            new Vec3[] {
                                new Vec3(min, max, min),
                                new Vec3(min, min, min),
                                new Vec3(max, min, min),
                                new Vec3(max, max, min)
                            };
                    case SOUTH ->
                            new Vec3[] {
                                new Vec3(max, max, max),
                                new Vec3(max, min, max),
                                new Vec3(min, min, max),
                                new Vec3(min, max, max)
                            };
                };
        for (int reverse = 0; reverse < 2; reverse++) {
            Vec3 normal = vertices[2].clone().sub(vertices[1]).cross(vertices[0].clone().sub(vertices[1])).normalize();
            if (reverse == 1) {
                normal.negate();
            }
            emitter.nominalFace(normal.toDirection()).cullFace(null);
            for (int i = 0; i < 4; i++) {
                Vec3 vertex = vertices[reverse == 0 ? i : 3 - i].clone().add(translation);
                emitter.pos(i, vertex.to3f())
                        .color(i, -1)
                        .normal(i, normal.to3f())
                        .uv(i, i < 2 ? 0 : 1, i == 1 || i == 2 ? 1 : 0);
            }
            emitter.materialBake(material, MutableQuadView.BAKE_NORMALIZED).emit();
        }
    }

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> parts) {}

    @Override
    public Material.Baked particleMaterial() {
        return materials.get("inactive_dot");
    }

    @Override
    public int materialFlags() {
        return 0;
    }
}
