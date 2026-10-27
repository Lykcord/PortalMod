package net.portalmod.client.render;

import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.GrassColor;
import net.minecraft.world.level.block.state.BlockState;
import net.portalmod.core.init.BlockInit;

import java.util.List;

public final class BlockColorHandler {
    public static void init() {
        BlockColorRegistry.register(
                List.of(
                        new BlockTintSource() {
                            @Override
                            public int color(BlockState state) {
                                return 0xFF000000 | GrassColor.get(.5D, 1D);
                            }

                            @Override
                            public int colorInWorld(BlockState state, BlockAndTintGetter world, BlockPos pos) {
                                return 0xFF000000 | BiomeColors.getAverageFoliageColor(world, pos);
                            }
                        }),
                BlockInit.ARBORED_BLACKPLATE.get(),
                BlockInit.ARBORED_BLACKPLATE_SLAB.get(),
                BlockInit.ARBORED_BLACKPLATE_STAIRS.get(),
                BlockInit.ARBORED_BLACKPLATE_PLATFORM.get());
    }
}
