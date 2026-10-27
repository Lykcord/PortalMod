package net.portalmod;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.portalmod.common.sorted.portal.ClientPortalManager;
import net.portalmod.common.sorted.portal.PortalEnd;
import net.portalmod.common.sorted.portal.PortalEntity;
import net.portalmod.common.sorted.portal.PortalPlacer;
import net.portalmod.common.sorted.portalgun.PortalGun;
import net.portalmod.core.config.PortalModConfigManager;
import net.portalmod.core.init.BlockInit;
import net.portalmod.core.init.ItemInit;
import net.portalmod.core.math.Vec3;

import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

public final class PortalBackToBackClientTest implements FabricClientGameTest {
    private static boolean suppressParticles;

    static {
        ClientTickEvents.END_CLIENT_TICK.register(
                client -> {
                    if (suppressParticles) {
                        client.particleEngine.clearParticles();
                    }
                });
    }

    @Override
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> suppressParticles = true);
        boolean previousHighlights = PortalModConfigManager.HIGHLIGHTS.get();
        try (var world =
                context.worldBuilder()
                        .adjustSettings(
                                settings -> settings.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE))
                        .create()) {
            var gun =
                    world.getServer()
                            .computeOnServer(
                                    server -> {
                                        var player = world.getConnection().getServerPlayer();
                                        var level = player.level();
                                        for (int x = -5; x <= 5; x++) {
                                            for (int z = -6; z <= 6; z++) {
                                                level.setBlock(
                                                        new BlockPos(x, 99, z),
                                                        BlockInit.LUNECAST.get().defaultBlockState(),
                                                        2);
                                            }
                                            for (int y = 100; y <= 106; y++) {
                                                level.setBlock(
                                                        new BlockPos(x, y, -4),
                                                        Blocks.CONCRETE.green().defaultBlockState(),
                                                        2);
                                            }
                                        }
                                        for (int y = 100; y <= 103; y++) {
                                            level.setBlock(
                                                    new BlockPos(0, y, 0),
                                                    BlockInit.LUNECAST.get().defaultBlockState(),
                                                    2);
                                        }
                                        var stack = new ItemStack(ItemInit.PORTALGUN.get());
                                        PortalGun.addUUID(stack);
                                        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                                        var id = PortalGun.getUUID(stack).orElseThrow();
                                        var entry =
                                                PortalPlacer.placePortal(
                                                        level,
                                                        PortalEnd.PRIMARY,
                                                        "blue",
                                                        id,
                                                        new Vec3(.5, 101.625, 1),
                                                        Direction.SOUTH,
                                                        Direction.UP,
                                                        false,
                                                        null,
                                                        null);
                                        var exit =
                                                PortalPlacer.placePortal(
                                                        level,
                                                        PortalEnd.SECONDARY,
                                                        "orange",
                                                        id,
                                                        new Vec3(.5, 101.625, 0),
                                                        Direction.NORTH,
                                                        Direction.UP,
                                                        false,
                                                        null,
                                                        null);
                                        if (entry == null || exit == null) {
                                            throw new AssertionError(
                                                    "Cannot place portals on opposite faces of one wall");
                                        }
                                        player.connection.teleport(.5, 100, 4, 180, 0);
                                        return id;
                                    });
            world.getConnection().waitForClientboundPackets();
            context.waitFor(client -> ClientPortalManager.getInstance().has(gun, PortalEnd.SECONDARY));
            world.getConnection().waitForChunksRender();
            context.waitTicks(20);
            context.runOnClient(client -> PortalModConfigManager.HIGHLIGHTS.set(false));
            context.waitTicks(3);
            var without = ImageIO.read(context.takeScreenshot("portal-back-to-back-no-highlights").toFile());
            context.runOnClient(client -> PortalModConfigManager.HIGHLIGHTS.set(true));
            context.waitTicks(3);
            var with = ImageIO.read(context.takeScreenshot("portal-back-to-back-highlights").toFile());
            var bounds =
                    context.computeOnClient(
                            client -> {
                                PortalEntity entry = null;
                                for (var entity : client.level.entitiesForRendering()) {
                                    if (entity instanceof PortalEntity portal
                                            && portal.getGunUUID().equals(gun)
                                            && portal.getDirection() == Direction.SOUTH) {
                                        entry = portal;
                                        break;
                                    }
                                }
                                if (entry == null) {
                                    throw new AssertionError("Missing front portal");
                                }
                                var camera = client.gameRenderer.gameRenderState().levelRenderState.cameraRenderState;
                                var matrix = new Matrix4f(camera.projectionMatrix).mul(camera.viewRotationMatrix);
                                int left = with.getWidth(), top = with.getHeight(), right = 0, bottom = 0;
                                for (double x : new double[] {-.5, .5}) {
                                    for (double y : new double[] {-1, 1}) {
                                        var point =
                                                matrix.transform(
                                                        new Vector4f(
                                                                (float) (entry.getX() + x - camera.pos.x),
                                                                (float) (entry.getY() + y - camera.pos.y),
                                                                (float) (entry.getZ() - camera.pos.z),
                                                                1));
                                        int pixelX = (int) ((point.x / point.w * .5 + .5) * with.getWidth());
                                        int pixelY = (int) ((.5 - point.y / point.w * .5) * with.getHeight());
                                        left = Math.min(left, pixelX);
                                        right = Math.max(right, pixelX);
                                        top = Math.min(top, pixelY);
                                        bottom = Math.max(bottom, pixelY);
                                    }
                                }
                                return new int[] {left, top, right, bottom};
                            });
            var comparison = changedPixels(without, with, bounds);
            if (comparison[1] < 500 || comparison[0] > comparison[1] / 100) {
                throw new AssertionError(
                        "Back-face portal highlight leaked into the front opening: "
                                + comparison[0]
                                + "/"
                                + comparison[1]
                                + " pixels");
            }
            if (blueGreen(without, bounds, false) < blueGreen(without, bounds, true) + 20) {
                throw new AssertionError("Portal frame's bright cyan end must be at the bottom");
            }
            world.getServer()
                    .runOnServer(
                            server -> {
                                var level = world.getConnection().getServerPlayer().level();
                                for (int y = 100; y <= 103; y++) {
                                    level.setBlockAndUpdate(
                                            new BlockPos(0, y, 2), BlockInit.LUNECAST.get().defaultBlockState());
                                }
                            });
            world.getConnection().waitForClientboundPackets();
            context.waitTicks(5);
            context.runOnClient(client -> PortalModConfigManager.HIGHLIGHTS.set(false));
            context.waitTicks(3);
            var blockedWithout = ImageIO.read(context.takeScreenshot("portal-highlights-wall-off").toFile());
            context.runOnClient(client -> PortalModConfigManager.HIGHLIGHTS.set(true));
            context.waitTicks(3);
            var blockedWith = ImageIO.read(context.takeScreenshot("portal-highlights-wall-on").toFile());
            if (changedPixels(blockedWithout, blockedWith, bounds)[0] < 50) {
                throw new AssertionError("Occluded portal highlights must remain visible through ordinary walls");
            }
        } catch (IOException error) {
            throw new AssertionError("Cannot read back-to-back portal screenshots", error);
        } finally {
            context.runOnClient(
                    client -> {
                        suppressParticles = false;
                        PortalModConfigManager.HIGHLIGHTS.set(previousHighlights);
                    });
        }
    }

    private static int[] changedPixels(BufferedImage without, BufferedImage with, int[] bounds) {
        int changed = 0, sampled = 0;
        double centerX = (bounds[0] + bounds[2]) / 2.0;
        double centerY = (bounds[1] + bounds[3]) / 2.0;
        for (int y = Math.max(0, bounds[1]); y < Math.min(with.getHeight(), bounds[3]); y++) {
            for (int x = Math.max(0, bounds[0]); x < Math.min(with.getWidth(), bounds[2]); x++) {
                double dx = (x - centerX) / ((bounds[2] - bounds[0]) / 2.0);
                double dy = (y - centerY) / ((bounds[3] - bounds[1]) / 2.0);
                if (dx * dx + dy * dy > .7 || Math.abs(x - centerX) < 40 && Math.abs(y - centerY) < 40) {
                    continue;
                }
                sampled++;
                int a = without.getRGB(x, y), b = with.getRGB(x, y);
                int difference = 0;
                for (int shift : new int[] {0, 8, 16}) {
                    difference += Math.abs((a >> shift & 255) - (b >> shift & 255));
                }
                if (difference > 24) {
                    changed++;
                }
            }
        }
        return new int[] {changed, sampled};
    }

    private static double blueGreen(BufferedImage image, int[] bounds, boolean top) {
        long green = 0;
        int count = 0;
        int height = bounds[3] - bounds[1];
        int start = top ? bounds[1] : bounds[3] - height / 4;
        int end = top ? bounds[1] + height / 4 : bounds[3];
        for (int y = Math.max(0, start); y < Math.min(image.getHeight(), end); y++) {
            for (int x = Math.max(0, bounds[0] - 3); x < Math.min(image.getWidth(), bounds[2] + 3); x++) {
                int color = image.getRGB(x, y);
                int red = color >> 16 & 255, value = color >> 8 & 255, blue = color & 255;
                if (blue > red + 80 && blue > value * 1.1) {
                    green += value;
                    count++;
                }
            }
        }
        if (count < 10) {
            throw new AssertionError("Missing " + (top ? "top" : "bottom") + " portal border pixels");
        }
        return (double) green / count;
    }
}
