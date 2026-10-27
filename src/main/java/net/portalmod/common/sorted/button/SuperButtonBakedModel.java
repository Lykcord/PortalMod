package net.portalmod.common.sorted.button;

import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.SingleVariant;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.portalmod.core.init.BlockInit;

public final class SuperButtonBakedModel {
    private SuperButtonBakedModel() {}

    public static void init() {
        ModelLoadingPlugin.register(
                plugin ->
                        plugin.registerBlockStateResolver(
                                BlockInit.SUPER_BUTTON.get(),
                                context -> {
                                    for (var state : context.block().getStateDefinition().getPossibleStates()) {
                                        var corner = state.getValue(SuperButtonBlock.CORNER);
                                        int variant =
                                                corner == QuadBlockCorner.DOWN_RIGHT
                                                                || corner == QuadBlockCorner.UP_LEFT
                                                        ? 1
                                                        : 2;
                                        var model =
                                                Identifier.fromNamespaceAndPath(
                                                        "portalmod",
                                                        "block/super_button_"
                                                                + state.getValue(SuperButtonBlock.MODE)
                                                                        .getSerializedName()
                                                                + "_"
                                                                + variant
                                                                + (state.getValue(SuperButtonBlock.PRESSED)
                                                                        ? "_pressed"
                                                                        : "_unpressed")
                                                                + (state.getValue(SuperButtonBlock.ACTIVE)
                                                                        ? "_on"
                                                                        : "_off"));
                                        context.setModel(
                                                state,
                                                new Unbaked(model, state.getValue(SuperButtonBlock.FACING), corner));
                                    }
                                }));
    }

    private record Unbaked(Identifier model, Direction facing, QuadBlockCorner corner)
            implements BlockStateModel.UnbakedRoot {
        @Override
        public BlockStateModel bake(BlockState state, ModelBaker baker) {
            return new SingleVariant(
                    SimpleModelWrapper.bake(baker, model, new SuperButtonModelTransform(facing, corner)));
        }

        @Override
        public Object visualEqualityGroup(BlockState state) {
            return this;
        }

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            resolver.markDependency(model);
        }
    }
}
