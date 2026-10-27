package net.portalmod;

import com.google.gson.GsonBuilder;

import jdk.jfr.Recording;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.portalmod.common.sorted.antline.AntlineTileEntity;
import net.portalmod.common.sorted.antline.indicator.AntlineEncoderBlock;
import net.portalmod.common.sorted.antline.indicator.AntlineOutput;
import net.portalmod.common.sorted.button.StandingButtonBlock;
import net.portalmod.core.init.BlockInit;
import net.portalmod.core.init.ItemInit;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

public final class AntlineMemoryClientTest implements FabricClientGameTest {
    private record Sample(
            String phase,
            long tick,
            long heap,
            long committed,
            long resident,
            long packets,
            int updates,
            int activeWires,
            boolean encoderPowered,
            long clientAllocated,
            long serverAllocated) {}

    @Override
    public void runTest(ClientGameTestContext context) {
        if (!Boolean.getBoolean("portalmod.antlineMemory")) {
            return;
        }
        var output = Path.of(System.getProperty("portalmod.antlineMemoryOutput"));
        boolean encoderPower = "encoder".equals(System.getProperty("portalmod.antlineMemoryPower", "button"));
        boolean clock = "clock".equals(System.getProperty("portalmod.antlineMemoryPower", "button"));
        var samples = new ArrayList<Sample>();
        var properties = new Properties();
        properties.setProperty("server-ip", "127.0.0.1");
        properties.setProperty("gamemode", "creative");
        properties.setProperty("view-distance", "4");
        try (var socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            properties.setProperty("server-port", Integer.toString(socket.getLocalPort()));
        } catch (IOException error) {
            throw new AssertionError(error);
        }
        var encoder = new BlockPos(4, 100, 0);
        var button = new BlockPos(0, 100, 0);
        int previousFps = context.computeOnClient(client -> client.options.framerateLimit().get());
        try (var recording = new Recording();
                var server = context.worldBuilder().createServer(properties);
                var connection = server.connect()) {
            Files.createDirectories(output);
            recording.setMaxSize(64L * 1024 * 1024);
            recording.enable("jdk.ObjectAllocationSample").withStackTrace();
            recording.enable("jdk.ExecutionSample").withPeriod(Duration.ofMillis(10));
            recording.enable("jdk.GCHeapSummary");
            recording.setDestination(output.resolve("allocations.jfr"));
            recording.start();
            var watch =
                    server.computeOnServer(
                            instance -> {
                                var player = connection.getServerPlayer();
                                var level = player.level();
                                for (int x = -2; x <= 7; x++) {
                                    for (int z = -3; z <= 7; z++) {
                                        level.setBlock(
                                                new BlockPos(x, 99, z),
                                                BlockInit.LUNECAST.get().defaultBlockState(),
                                                2);
                                    }
                                }
                                var indicator = new BlockPos(2, 100, 0);
                                var decoder = indicator.east();
                                level.setBlockAndUpdate(
                                        indicator,
                                        BlockInit.ANTLINE_INDICATOR
                                                .get()
                                                .defaultBlockState()
                                                .setValue(
                                                        FaceAttachedHorizontalDirectionalBlock.FACE, AttachFace.FLOOR));
                                level.setBlockAndUpdate(
                                        decoder,
                                        BlockInit.ANTLINE_DECODER
                                                .get()
                                                .defaultBlockState()
                                                .setValue(
                                                        FaceAttachedHorizontalDirectionalBlock.FACE, AttachFace.FLOOR));
                                level.setBlockAndUpdate(
                                        encoder,
                                        BlockInit.ANTLINE_ENCODER
                                                .get()
                                                .defaultBlockState()
                                                .setValue(
                                                        FaceAttachedHorizontalDirectionalBlock.FACE, AttachFace.FLOOR));
                                var wires = new ArrayList<BlockPos>();
                                for (int x = 1; x <= 5; x++) {
                                    wires.add(new BlockPos(x, 100, 4));
                                }
                                for (int z = 0; z < 4; z++) {
                                    wires.add(new BlockPos(1, 100, z));
                                    wires.add(new BlockPos(5, 100, z));
                                }
                                var nodes = new HashSet<>(wires);
                                nodes.addAll(List.of(indicator, decoder, encoder, button, button.above()));
                                var result = AntlineFeedbackProbe.watch(level, nodes, 1_000_000);
                                var stack = new ItemStack(ItemInit.ANTLINE.get(), 64);
                                player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                                for (var pos : wires) {
                                    var support = pos.below();
                                    var hit =
                                            new BlockHitResult(
                                                    Vec3.atCenterOf(support).add(0, .5, 0),
                                                    Direction.UP,
                                                    support,
                                                    false);
                                    if (!((BlockItem) stack.getItem())
                                            .place(new BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack, hit))
                                            .consumesAction()) {
                                        throw new AssertionError("Could not place loop wire at " + pos);
                                    }
                                }
                                var buttonStack = new ItemStack(ItemInit.STANDING_BUTTON.get());
                                player.setItemInHand(InteractionHand.MAIN_HAND, buttonStack);
                                var support = button.below();
                                var hit =
                                        new BlockHitResult(
                                                Vec3.atCenterOf(support).add(0, .5, 0), Direction.UP, support, false);
                                if (!((BlockItem) buttonStack.getItem())
                                        .place(
                                                new BlockPlaceContext(
                                                        player, InteractionHand.MAIN_HAND, buttonStack, hit))
                                        .consumesAction()) {
                                    throw new AssertionError("Could not place the standing button beside the loop");
                                }
                                player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                                player.connection.teleport(3, 100, 8, 180, 22);
                                return result;
                            });
            try (watch) {
                connection.waitForClientboundPackets();
                context.waitFor(
                        client ->
                                client.player.getY() > 99
                                        && client.level.getBlockEntity(new BlockPos(1, 100, 0))
                                                instanceof AntlineTileEntity);
                context.runOnClient(client -> client.options.framerateLimit().set(120));
                connection.waitForClientboundPackets();
                context.waitTicks(400);
                for (int phase = 0; phase < 7; phase++) {
                    if (phase == 2) {
                        server.runOnServer(
                                instance -> {
                                    var level = connection.getServerPlayer().level();
                                    if (clock) {
                                        var indicator = new BlockPos(2, 100, 0);
                                        level.setBlockAndUpdate(
                                                indicator,
                                                level.getBlockState(indicator).setValue(AntlineOutput.REVERSED, true));
                                    } else if (encoderPower) {
                                        level.setBlockAndUpdate(
                                                encoder.north(), Blocks.REDSTONE_BLOCK.defaultBlockState());
                                    } else {
                                        var state = level.getBlockState(button.above());
                                        ((StandingButtonBlock) state.getBlock()).press(state, level, button.above());
                                    }
                                });
                        server.waitFor(
                                instance ->
                                        connection
                                                .getServerPlayer()
                                                .level()
                                                .getBlockState(encoder)
                                                .getValue(AntlineEncoderBlock.POWERED),
                                40);
                    }
                    if (phase == 6) {
                        server.runOnServer(
                                instance -> {
                                    var level = connection.getServerPlayer().level();
                                    if (clock) {
                                        var indicator = new BlockPos(2, 100, 0);
                                        level.setBlockAndUpdate(
                                                indicator,
                                                level.getBlockState(indicator).setValue(AntlineOutput.REVERSED, false));
                                    } else if (encoderPower) {
                                        level.setBlockAndUpdate(encoder.north(), Blocks.AIR.defaultBlockState());
                                    } else {
                                        level.destroyBlock(encoder, false);
                                    }
                                });
                    }
                    context.waitTicks(400);
                    System.gc();
                    var heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
                    long[] serverStats =
                            server.computeOnServer(
                                    instance -> {
                                        var level = connection.getServerPlayer().level();
                                        int activeWires = 0;
                                        for (int x = 1; x <= 5; x++) {
                                            for (int z = 0; z <= 4; z++) {
                                                var pos = new BlockPos(x, 100, z);
                                                if (level.getBlockEntity(pos) instanceof AntlineTileEntity wire
                                                        && wire.getSideMap().get(Direction.DOWN).isActive()) {
                                                    activeWires++;
                                                }
                                            }
                                        }
                                        var state = level.getBlockState(encoder);
                                        boolean powered =
                                                state.hasProperty(AntlineEncoderBlock.POWERED)
                                                        && state.getValue(AntlineEncoderBlock.POWERED);
                                        return new long[] {
                                            watch.packets(),
                                            watch.updates(),
                                            allocated(),
                                            activeWires,
                                            powered ? 1 : 0,
                                            level.getGameTime()
                                        };
                                    });
                    long clientAllocated = context.computeOnClient(client -> allocated());
                    var sample =
                            new Sample(
                                    phase < 2 ? "unpowered" : phase < 6 ? "powered" : "released",
                                    serverStats[5],
                                    heap.getUsed(),
                                    heap.getCommitted(),
                                    resident(),
                                    serverStats[0],
                                    (int) serverStats[1],
                                    (int) serverStats[3],
                                    serverStats[4] != 0,
                                    clientAllocated,
                                    serverStats[2]);
                    samples.add(sample);
                    Files.writeString(
                            output.resolve("samples.json"),
                            new GsonBuilder().setPrettyPrinting().create().toJson(samples));
                    System.out.println("Antline memory sample: " + sample);
                    if (phase == 5) {
                        context.takeScreenshot("antline-memory-circuit-powered");
                    }
                }
                context.takeScreenshot("antline-memory-circuit");
                recording.stop();
                var firstPowered = samples.get(2);
                var lastPowered = samples.get(5);
                if (!clock && (lastPowered.activeWires() != 13 || !lastPowered.encoderPowered())) {
                    throw new AssertionError(
                            "The test circuit must remain powered after the button releases: " + lastPowered);
                }
                if (clock) {
                    long changes = lastPowered.packets() - firstPowered.packets();
                    long ticks = lastPowered.tick() - firstPowered.tick();
                    if (changes < 13 || changes > (ticks + 2) * 13) {
                        throw new AssertionError(
                                "Clock must continue at a bounded tick rate: " + firstPowered + " -> " + lastPowered);
                    }
                    for (var sample : samples.subList(2, 6)) {
                        if (sample.activeWires() != (sample.encoderPowered() ? 13 : 0)) {
                            throw new AssertionError(
                                    "Clock left part of its connected network in a stale state: " + sample);
                        }
                    }
                } else if (lastPowered.packets() != firstPowered.packets()
                        || lastPowered.updates() != firstPowered.updates()) {
                    throw new AssertionError(
                            "Stable powered loop keeps producing work: " + firstPowered + " -> " + lastPowered);
                }
                if (lastPowered.heap() - firstPowered.heap() > 32L * 1024 * 1024) {
                    throw new AssertionError(
                            "Powered loop retained heap keeps growing after full GC: "
                                    + firstPowered
                                    + " -> "
                                    + lastPowered);
                }
                if (!clock && !encoderPower && samples.getLast().activeWires() != 0) {
                    throw new AssertionError("Removing the encoder must unpower every remaining wire");
                }
            }
        } catch (IOException error) {
            throw new AssertionError("Cannot save antline memory profile", error);
        } finally {
            context.runOnClient(client -> client.options.framerateLimit().set(previousFps));
        }
    }

    private static long allocated() {
        var bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        return bean.getThreadAllocatedBytes(Thread.currentThread().threadId());
    }

    private static long resident() throws IOException {
        var status = Path.of("/proc/self/status");
        if (!Files.exists(status)) {
            return -1;
        }
        for (var line : Files.readAllLines(status)) {
            if (line.startsWith("VmRSS:")) {
                return Long.parseLong(line.trim().split("\\s+")[1]) * 1024;
            }
        }
        return -1;
    }
}
