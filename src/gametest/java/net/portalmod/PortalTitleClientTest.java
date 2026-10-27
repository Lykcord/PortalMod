package net.portalmod;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.TitleScreen;
import net.portalmod.core.config.PortalModConfigManager;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Objects;

import javax.imageio.ImageIO;

public final class PortalTitleClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        boolean previousMenu = PortalModConfigManager.MENU.get();
        try {
            context.runOnClient(client -> PortalModConfigManager.MENU.set(true));
            context.setScreen(TitleScreen::new);
            context.waitTicks(30);
            var image = ImageIO.read(context.takeScreenshot("portalmod-title-centered").toFile());
            var template =
                    ImageIO.read(
                            Objects.requireNonNull(
                                    getClass()
                                            .getResourceAsStream("/assets/portalmod/textures/gui/title/edition.png")));
            int width = context.computeOnClient(client -> client.gui.screen().width);
            double scale = (double) image.getWidth() / width;
            int bestOffset = 0;
            long bestScore = Long.MAX_VALUE;
            for (int offset = -25; offset <= 30; offset++) {
                long score = score(image, template, scale, width / 2 - 64 + offset, 67);
                if (score < bestScore) {
                    bestScore = score;
                    bestOffset = offset;
                }
            }
            if (Math.abs(bestOffset - 15) > 1) {
                throw new AssertionError("PortalMod title artwork is off center: texture offset=" + bestOffset);
            }
        } catch (IOException error) {
            throw new AssertionError("Cannot read the title screenshot", error);
        } finally {
            context.runOnClient(client -> PortalModConfigManager.MENU.set(previousMenu));
        }
    }

    private static long score(BufferedImage image, BufferedImage template, double scale, int left, int top) {
        long score = 0;
        for (int y = 0; y < 14; y++) {
            for (int x = 4; x < 94; x++) {
                int expected = template.getRGB(x, y);
                if ((expected >>> 24) < 250) {
                    continue;
                }
                int actual = image.getRGB((int) ((left + x + .5) * scale), (int) ((top + y + .5) * scale));
                for (int shift : new int[] {0, 8, 16}) {
                    int difference = (actual >> shift & 255) - (expected >> shift & 255);
                    score += (long) difference * difference;
                }
            }
        }
        return score;
    }
}
