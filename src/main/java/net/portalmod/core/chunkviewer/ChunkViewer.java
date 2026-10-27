package net.portalmod.core.chunkviewer;

import net.minecraft.world.level.ChunkPos;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

public class ChunkViewer {
    private static final ChunkViewer INSTANCE = new ChunkViewer();

    private final JFrame window;
    protected final List<ChunkPos> chunks = new ArrayList<>();

    public ChunkViewer() {
        System.setProperty("java.awt.headless", "false");
        window = new JFrame("Chunk Viewer");
        window.setContentPane(new ChunkViewPane());
        window.getContentPane().setPreferredSize(new Dimension(300, 300));
        window.pack();
        window.setResizable(false);
    }

    private class ChunkViewPane extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            for (ChunkPos pos : chunks.toArray(new ChunkPos[0])) {
                g.fillRect(pos.x() + 150, pos.z() + 150, 1, 1);
            }
        }
    }

    public void setVisible(boolean visible) {
        window.setVisible(visible);
    }

    public void refresh() {
        window.repaint();
    }

    public List<ChunkPos> getChunkList() {
        return chunks;
    }

    public static ChunkViewer getInstance() {
        return INSTANCE;
    }
}
