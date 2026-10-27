package net.portalmod;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.portalmod.common.sorted.antline.AntlineBlock;
import net.portalmod.common.sorted.antline.AntlineTileEntity;
import net.portalmod.common.sorted.antline.indicator.AntlineEncoderBlock;
import net.portalmod.common.sorted.antline.indicator.AntlineOutput;
import net.portalmod.core.init.BlockInit;
import net.portalmod.core.init.ItemInit;

import java.util.ArrayList;
import java.util.Set;

public final class AntlineFeedbackServerTest {
    @GameTest(maxTicks = 100)
    public void indicatorDecoderEncoderLoopSettles(GameTestHelper test) {
        checkLoop(test, false);
    }

    @GameTest(maxTicks = 100)
    public void reversedIndicatorDecoderEncoderLoopSettles(GameTestHelper test) {
        checkLoop(test, true);
    }

    private static void checkLoop(GameTestHelper test, boolean reversed) {
        var level = test.getLevel();
        var indicator = test.absolutePos(new BlockPos(2, 2, 2));
        var decoder = indicator.east();
        var encoder = decoder.east();
        var watch = AntlineFeedbackProbe.watch(level, Set.of(indicator, decoder, encoder));
        int updates =
                AntlineFeedbackProbe.count(
                        () -> {
                            for (int x = 0; x <= 6; x++) {
                                for (int z = 0; z <= 6; z++) {
                                    test.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
                                }
                            }
                            level.setBlockAndUpdate(
                                    indicator,
                                    BlockInit.ANTLINE_INDICATOR
                                            .get()
                                            .defaultBlockState()
                                            .setValue(FaceAttachedHorizontalDirectionalBlock.FACE, AttachFace.FLOOR)
                                            .setValue(AntlineOutput.REVERSED, reversed));
                            level.setBlockAndUpdate(
                                    decoder,
                                    BlockInit.ANTLINE_DECODER
                                            .get()
                                            .defaultBlockState()
                                            .setValue(FaceAttachedHorizontalDirectionalBlock.FACE, AttachFace.FLOOR));
                            level.setBlockAndUpdate(
                                    encoder,
                                    BlockInit.ANTLINE_ENCODER
                                            .get()
                                            .defaultBlockState()
                                            .setValue(FaceAttachedHorizontalDirectionalBlock.FACE, AttachFace.FLOOR));
                            var wires = new ArrayList<BlockPos>();
                            for (int x = 1; x <= 5; x++) {
                                wires.add(test.absolutePos(new BlockPos(x, 2, 6)));
                            }
                            for (int z = 2; z < 6; z++) {
                                wires.add(test.absolutePos(new BlockPos(1, 2, z)));
                                wires.add(test.absolutePos(new BlockPos(5, 2, z)));
                            }
                            var player = test.makeMockPlayer(GameType.CREATIVE);
                            var stack = new ItemStack(ItemInit.ANTLINE.get(), 64);
                            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                            for (var pos : wires) {
                                var support = pos.below();
                                var hit =
                                        new BlockHitResult(
                                                Vec3.atCenterOf(support).add(0, .5, 0), Direction.UP, support, false);
                                var placement = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack, hit);
                                test.assertTrue(
                                        ((BlockItem) stack.getItem()).place(placement).consumesAction(),
                                        "Loop wire must be placed through the real item interaction");
                            }
                            var block = (AntlineBlock) BlockInit.ANTLINE.get();
                            for (var pos : wires) {
                                var side =
                                        ((AntlineTileEntity) level.getBlockEntity(pos))
                                                .getSideMap()
                                                .get(Direction.DOWN);
                                block.sideUpdate(level, side, pos, true, false, null);
                            }
                            for (int repeat = 0; repeat < 4; repeat++) {
                                level.setBlockAndUpdate(encoder.north(), Blocks.REDSTONE_BLOCK.defaultBlockState());
                                level.setBlockAndUpdate(encoder.north(), Blocks.AIR.defaultBlockState());
                                block.recursiveSignalChain(
                                        level,
                                        ((AntlineTileEntity) level.getBlockEntity(wires.getFirst()))
                                                .getSideMap()
                                                .get(Direction.DOWN),
                                        wires.getFirst(),
                                        null,
                                        false,
                                        0);
                            }
                            level.setBlockAndUpdate(encoder.north(), Blocks.REDSTONE_BLOCK.defaultBlockState());
                        });
        test.assertTrue(
                updates > 0 && updates < 8192,
                "Closed circuit must settle without repeated device notifications: " + updates);
        test.runAfterDelay(
                30,
                () -> {
                    test.assertTrue(
                            level.getBlockState(encoder).getValue(AntlineEncoderBlock.POWERED),
                            "Encoder must respond to a held redstone input");
                    level.setBlockAndUpdate(encoder.north(), Blocks.AIR.defaultBlockState());
                    test.runAfterDelay(
                            30,
                            () -> {
                                try (watch) {
                                    test.assertTrue(
                                            watch.updates() > 0 && watch.updates() < 8192,
                                            "Powered and unpowered feedback must stay bounded across ticks: "
                                                    + watch.updates());
                                    test.succeed();
                                }
                            });
                });
    }
}
