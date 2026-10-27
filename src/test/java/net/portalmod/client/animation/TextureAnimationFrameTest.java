package net.portalmod.client.animation;

import static org.junit.Assert.*;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;

public class TextureAnimationFrameTest {
    @Test
    public void framesAdvanceAtExactRateAndWrap() {
        assertEquals(0, TextureAnimationFrame.atTime(60, 8, 16).index());
        assertEquals(1, TextureAnimationFrame.atTime(60, 8, 17).index());
        assertEquals(4, TextureAnimationFrame.atTime(60, 8, 1000).index());
        assertEquals(0, TextureAnimationFrame.atTime(2000, 8, 4).index());
        assertEquals(4, TextureAnimationFrame.atTime(2000, 8, 2).index());
        assertEquals(6, TextureAnimationFrame.atTime(60, 8, -17).index());
    }

    @Test
    public void frameUvStaysInsideSelectedVerticalTile() {
        var frame = new TextureAnimationFrame(2, 4);
        assertEquals(.5F, frame.getV(0), 0);
        assertEquals(.75F, frame.getV(1), 0);
        assertEquals(.25F, frame.getU(.25F), 0);
        assertEquals(0, TextureAnimationFrame.atTime(0, 4, 1000).index());
        assertEquals(0, TextureAnimationFrame.atTime(-20, 4, 1000).index());
        assertEquals(1, TextureAnimationFrame.atTime(20, 0, 1000).count());
        assertEquals(0, TextureAnimationFrame.atTime(Integer.MAX_VALUE, 1, Long.MAX_VALUE).index());
        int expected =
                BigInteger.valueOf(Long.MAX_VALUE)
                        .multiply(BigInteger.valueOf(Integer.MAX_VALUE))
                        .divide(BigInteger.valueOf(1000))
                        .mod(BigInteger.valueOf(8))
                        .intValue();
        assertEquals(expected, TextureAnimationFrame.atTime(Integer.MAX_VALUE, 8, Long.MAX_VALUE).index());
    }
}
