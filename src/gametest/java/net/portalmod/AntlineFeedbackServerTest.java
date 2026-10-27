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
import net.portalmod.common.sorted.button.StandingButtonBlock;
import net.portalmod.core.init.BlockInit;
import net.portalmod.core.init.ItemInit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public final class AntlineFeedbackServerTest {
    @GameTest(maxTicks = 100)
    public void indicatorDecoderEncoderLoopSettles(GameTestHelper test) {
        checkLoop(test, false);
    }

    @GameTest(maxTicks = 100)
    public void invertedIndicatorDecoderEncoderClockStaysBounded(GameTestHelper test) {
        checkLoop(test, true);
    }

    @GameTest(maxTicks = 40)
    public void sharedIndicatorKeepsPowerFromOtherNetwork(GameTestHelper test) {
        var level = test.getLevel();
        for (int x = 1; x <= 5; x++) {
            test.setBlock(new BlockPos(x, 1, 3), Blocks.STONE);
        }
        var indicator = test.absolutePos(new BlockPos(3, 2, 3));
        var leftInput = indicator.west(2);
        var rightInput = indicator.east(2);
        level.setBlockAndUpdate(indicator, BlockInit.ANTLINE_INDICATOR.get().defaultBlockState());
        for (var pos : Set.of(leftInput, rightInput)) {
            level.setBlockAndUpdate(pos, BlockInit.ANTLINE_ENCODER.get().defaultBlockState());
        }
        var player = test.makeMockPlayer(GameType.CREATIVE);
        var stack = new ItemStack(ItemInit.ANTLINE.get(), 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        for (var pos : Set.of(indicator.west(), indicator.east())) {
            var support = pos.below();
            var hit = new BlockHitResult(Vec3.atCenterOf(support).add(0, .5, 0), Direction.UP, support, false);
            test.assertTrue(
                    ((BlockItem) stack.getItem())
                            .place(new BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack, hit))
                            .consumesAction(),
                    "Both independent inputs must connect to the indicator");
        }
        level.setBlockAndUpdate(leftInput.north(), Blocks.REDSTONE_BLOCK.defaultBlockState());
        test.runAfterDelay(
                5,
                () -> {
                    test.assertTrue(
                            level.getBlockState(indicator).getValue(AntlineOutput.ACTIVATED),
                            "The first network must power the shared indicator");
                    try (var watch = AntlineFeedbackProbe.watch(level, Set.of(indicator))) {
                        ((AntlineBlock) BlockInit.ANTLINE.get())
                                .updateSignalNetwork(level, indicator.east(), Direction.DOWN);
                        test.assertTrue(
                                level.getBlockState(indicator).getValue(AntlineOutput.ACTIVATED),
                                "An inactive second network must not override the active input");
                        test.assertTrue(
                                watch.updates() == 0,
                                "Rechecking the inactive input must not momentarily toggle the shared indicator");
                    }
                    level.setBlockAndUpdate(leftInput.north(), Blocks.AIR.defaultBlockState());
                    test.runAfterDelay(
                            5,
                            () -> {
                                test.assertTrue(
                                        !level.getBlockState(indicator).getValue(AntlineOutput.ACTIVATED),
                                        "The indicator must turn off after its final input is removed");
                                test.succeed();
                            });
                });
    }

    private static void checkLoop(GameTestHelper test, boolean reversed) {
        var level = test.getLevel();
        var indicator = test.absolutePos(new BlockPos(2, 2, 2));
        var decoder = indicator.east();
        var encoder = decoder.east();
        var button = test.absolutePos(new BlockPos(0, 2, 2));
        var loopWires = new ArrayList<BlockPos>();
        for (int x = 1; x <= 5; x++) {
            loopWires.add(test.absolutePos(new BlockPos(x, 2, 6)));
        }
        for (int z = 2; z < 6; z++) {
            loopWires.add(test.absolutePos(new BlockPos(1, 2, z)));
            loopWires.add(test.absolutePos(new BlockPos(5, 2, z)));
        }
        var watched = new HashSet<>(loopWires);
        watched.addAll(Set.of(indicator, decoder, encoder, button));
        var watch = AntlineFeedbackProbe.watch(level, watched);
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
                            var player = test.makeMockPlayer(GameType.CREATIVE);
                            var stack = new ItemStack(ItemInit.ANTLINE.get(), 64);
                            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                            for (var pos : loopWires) {
                                var support = pos.below();
                                var hit =
                                        new BlockHitResult(
                                                Vec3.atCenterOf(support).add(0, .5, 0), Direction.UP, support, false);
                                var placement = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack, hit);
                                test.assertTrue(
                                        ((BlockItem) stack.getItem()).place(placement).consumesAction(),
                                        "Loop wire must be placed through the real item interaction");
                            }
                            var buttonStack = new ItemStack(ItemInit.STANDING_BUTTON.get());
                            player.setItemInHand(InteractionHand.MAIN_HAND, buttonStack);
                            var support = button.below();
                            var hit =
                                    new BlockHitResult(
                                            Vec3.atCenterOf(support).add(0, .5, 0), Direction.UP, support, false);
                            test.assertTrue(
                                    ((BlockItem) buttonStack.getItem())
                                            .place(
                                                    new BlockPlaceContext(
                                                            player, InteractionHand.MAIN_HAND, buttonStack, hit))
                                            .consumesAction(),
                                    "Standing button must connect to the third branch of the loop");
                            for (int repeat = 0; repeat < 4; repeat++) {
                                level.setBlockAndUpdate(encoder.north(), Blocks.REDSTONE_BLOCK.defaultBlockState());
                                level.setBlockAndUpdate(encoder.north(), Blocks.AIR.defaultBlockState());
                            }
                            level.setBlockAndUpdate(encoder.north(), Blocks.REDSTONE_BLOCK.defaultBlockState());
                        });
        test.assertTrue(
                updates > 0 && updates < 8192,
                "Closed circuit must settle without repeated device notifications: " + updates);
        var settledPackets = new long[1];
        test.runAfterDelay(10, () -> settledPackets[0] = watch.packets());
        test.runAfterDelay(
                15,
                () -> {
                    var state = level.getBlockState(button.above());
                    ((StandingButtonBlock) state.getBlock()).press(state, level, button.above());
                });
        test.runAfterDelay(
                50,
                () -> {
                    test.assertTrue(
                            level.getBlockState(encoder).getValue(AntlineEncoderBlock.POWERED),
                            "Encoder must respond to a held redstone input");
                    test.assertTrue(
                            watch.packets() == settledPackets[0],
                            "Changing another input must not flicker or resend an already-powered loop");
                    for (var wire : loopWires) {
                        test.assertTrue(
                                ((AntlineTileEntity) level.getBlockEntity(wire))
                                        .getSideMap()
                                        .get(Direction.DOWN)
                                        .isActive(),
                                "The active encoder must keep every branch powered");
                    }
                    level.setBlockAndUpdate(encoder.north(), Blocks.AIR.defaultBlockState());
                    var clockStart = new long[2];
                    test.runAfterDelay(
                            10,
                            () -> {
                                clockStart[0] = watch.packets();
                                clockStart[1] = level.getGameTime();
                            });
                    test.runAfterDelay(
                            30,
                            () -> {
                                try (watch) {
                                    long changes = watch.packets() - clockStart[0];
                                    long ticks = level.getGameTime() - clockStart[1];
                                    if (reversed) {
                                        test.assertTrue(
                                                changes > 0 && changes <= (ticks + 2) * loopWires.size(),
                                                "Inverted feedback must remain a running, tick-bounded clock: "
                                                        + changes);
                                    } else {
                                        test.assertTrue(
                                                changes == 0,
                                                "Non-inverting feedback must settle without packet churn");
                                    }
                                    test.assertTrue(
                                            watch.updates() > 0 && watch.updates() < 8192,
                                            "Powered and unpowered feedback must stay bounded across ticks: "
                                                    + watch.updates());
                                    level.destroyBlock(encoder, false);
                                    test.succeed();
                                }
                            });
                });
    }
}
