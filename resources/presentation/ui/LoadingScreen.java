package resources.presentation.ui;

import resources.app.GamePanel;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Loading screen shown between the {@link IntroScreen} main menu and the
 * actual {@link GamePanel}. Plays the layered parallax scene
 * (sky / clouds / mountains / swaying trees / swaying grass) while
 * {@code GamePanel} is constructed on a background thread.
 *
 * Use {@link #show(JFrame, boolean)} to swap it into the frame and start
 * the load. It guarantees a minimum display time so the screen never
 * flashes by, then swaps in the constructed {@code GamePanel}.
 */
public class LoadingScreen extends JPanel {

    private static final int W = 64 * 20;
    private static final int H = 64 * 12;

    private static final String ASSETS = "resources/images/ui/MainMenu/";

    /** Minimum time the loading screen stays visible before swapping to the game. */
    private static final long MIN_DISPLAY_MS = 1500;

    private final Timer animationTimer;

    private BufferedImage sky;
    private BufferedImage clouds;
    private BufferedImage mountains;
    private final BufferedImage[] treeFrames = new BufferedImage[3];
    private final BufferedImage[] grassFrames = new BufferedImage[2];

    private double skyOffset      = 0;
    private double cloudOffset    = 0;
    private double mountainOffset = 0;

    private final long startNanos = System.nanoTime();
    private float tSeconds = 0f;

    private static final double SKY_SPEED      =  -4;
    private static final double CLOUD_SPEED    = -18;
    private static final double MOUNTAIN_SPEED =  -1;
    private static final double TREE_FRAME_DT  = 0.55;
    private static final double GRASS_FRAME_DT = 0.35;

    /** Entry point: swap the frame to the loading screen and kick off the game-init thread. */
    public static void show(JFrame frame, boolean loadExisting) {
        LoadingScreen screen = new LoadingScreen();
        frame.getContentPane().removeAll();
        frame.getContentPane().add(screen);
        frame.revalidate();
        frame.repaint();
        screen.requestFocusInWindow();

        long startMs = System.currentTimeMillis();

        // Build GamePanel on a background thread so the EDT can paint the
        // loading animation while the world generates.
        new Thread(() -> {
            GamePanel gamePanel;
            try {
                gamePanel = new GamePanel(frame, /*newGame=*/!loadExisting);
            } catch (Throwable t) {
                t.printStackTrace();
                return;
            }

            long elapsed = System.currentTimeMillis() - startMs;
            long remaining = MIN_DISPLAY_MS - elapsed;
            if (remaining > 0) {
                try { Thread.sleep(remaining); } catch (InterruptedException ignored) {}
            }

            SwingUtilities.invokeLater(() -> {
                screen.animationTimer.stop();
                frame.getContentPane().removeAll();
                frame.getContentPane().add(gamePanel);
                frame.setSize(gamePanel.screenWidth, gamePanel.screenHeight);
                frame.revalidate();
                frame.repaint();
                gamePanel.requestFocusInWindow();
                gamePanel.startGameThread();
            });
        }, "game-init").start();
    }

    private LoadingScreen() {
        setPreferredSize(new Dimension(W, H));
        setBackground(Color.black);
        setDoubleBuffered(true);
        setFocusable(true);

        loadAssets();

        animationTimer = new Timer(16, e -> {
            tSeconds = (System.nanoTime() - startNanos) / 1_000_000_000f;
            tick();
            repaint();
        });
        animationTimer.start();
    }

    private BufferedImage tryLoad(String name) {
        try {
            File f = new File(ASSETS + name);
            if (f.exists()) return ImageIO.read(f);
        } catch (IOException ignored) {}
        return null;
    }

    private void loadAssets() {
        sky        = tryLoad("sky.png");
        clouds     = autoCrop(keyOutWhite(tryLoad("clouds.png")));
        mountains  = autoCrop(keyOutWhite(tryLoad("mountains.png")));
        treeFrames[0]  = autoCrop(keyOutWhite(tryLoad("trees_frame1.png")));
        treeFrames[1]  = autoCrop(keyOutWhite(tryLoad("trees_frame2.png")));
        treeFrames[2]  = autoCrop(keyOutWhite(tryLoad("trees_frame3.png")));
        grassFrames[0] = autoCrop(keyOutWhite(tryLoad("grass_frame1.png")));
        grassFrames[1] = autoCrop(keyOutWhite(tryLoad("grass_frame2.png")));
    }

    private static BufferedImage keyOutWhite(BufferedImage src) {
        if (src == null) return null;
        int w = src.getWidth(), h = src.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = src.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >>  8) & 0xFF;
                int b =  rgb        & 0xFF;
                int minC = Math.min(r, Math.min(g, b));
                int alpha;
                if (minC >= 235)      alpha = 0;
                else if (minC >= 200) alpha = (235 - minC) * 255 / 35;
                else                  alpha = 255;
                out.setRGB(x, y, (alpha << 24) | (rgb & 0x00FFFFFF));
            }
        }
        return out;
    }

    private static BufferedImage autoCrop(BufferedImage src) {
        if (src == null) return null;
        int w = src.getWidth(), h = src.getHeight();
        int minX = w, minY = h, maxX = -1, maxY = -1;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int a = (src.getRGB(x, y) >> 24) & 0xFF;
                if (a > 8) {
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
            }
        }
        if (maxX < 0) return src;
        return src.getSubimage(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    private void tick() {
        double dt = 0.016;
        skyOffset      += SKY_SPEED      * dt;
        cloudOffset    += CLOUD_SPEED    * dt;
        mountainOffset += MOUNTAIN_SPEED * dt;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                            RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        drawBackgroundLayers(g2);
        drawLoadingText(g2);

        g2.dispose();
    }

    private void drawBackgroundLayers(Graphics2D g2) {
        if (sky != null) {
            drawScrollingLayer(g2, sky, skyOffset, 0, H);
        } else {
            g2.setPaint(new GradientPaint(0, 0, new Color(15, 20, 50),
                                          0, H, new Color(80, 50, 90)));
            g2.fillRect(0, 0, W, H);
        }

        int grassBottom = H;
        int grassH = (int) (H * 0.18);
        int grassTop = grassBottom - grassH;

        int treeBottom = grassTop + (int) (grassH * 0.25);
        int treeH = (int) (H * 0.32);
        int treeTop = treeBottom - treeH;

        int mountainBottom = treeTop + (int) (treeH * 0.30);
        int mountainH = (int) (H * 0.28);
        int mountainTop = mountainBottom - mountainH;

        int cloudTop = (int) (H * 0.02);
        int cloudH   = (int) (H * 0.22);

        if (clouds != null) {
            drawScrollingLayer(g2, clouds, cloudOffset, cloudTop, cloudH);
        }
        if (mountains != null) {
            drawScrollingLayer(g2, mountains, mountainOffset, mountainTop, mountainH);
        }

        BufferedImage tree = pickFrame(treeFrames, tSeconds, TREE_FRAME_DT);
        if (tree != null) {
            g2.drawImage(tree, 0, treeTop, W, treeH, null);
        }

        BufferedImage grass = pickFrame(grassFrames, tSeconds, GRASS_FRAME_DT);
        if (grass != null) {
            g2.drawImage(grass, 0, grassTop, W, grassH, null);
        }
    }

    private void drawScrollingLayer(Graphics2D g2, BufferedImage img, double offset,
                                    int yTop, int targetH) {
        int srcW = img.getWidth();
        int srcH = img.getHeight();
        int drawW = (int) Math.round(srcW * (double) targetH / srcH);
        if (drawW <= 0) return;
        double mod = offset % drawW;
        if (mod > 0) mod -= drawW;
        int x = (int) Math.round(mod);
        while (x < W) {
            g2.drawImage(img, x, yTop, drawW, targetH, null);
            x += drawW;
        }
    }

    private BufferedImage pickFrame(BufferedImage[] frames, float time, double frameDt) {
        int count = 0;
        for (BufferedImage b : frames) if (b != null) count++;
        if (count == 0) return null;
        int step = ((int) (time / frameDt)) % count;
        if (step < 0) step += count;
        int i = 0;
        for (BufferedImage b : frames) {
            if (b == null) continue;
            if (i == step) return b;
            i++;
        }
        return frames[0];
    }

    private void drawLoadingText(Graphics2D g2) {
        // Animated ellipsis to show life
        int dots = ((int) (tSeconds * 2)) % 4;
        StringBuilder sb = new StringBuilder("Loading");
        for (int i = 0; i < dots; i++) sb.append('.');

        Font f = new Font("Serif", Font.BOLD, 48);
        g2.setFont(f);
        String s = sb.toString();
        int sw = g2.getFontMetrics().stringWidth(s);
        int sx = (W - sw) / 2;
        int sy = (int) (H * 0.4);

        g2.setColor(new Color(0, 0, 0, 200));
        g2.drawString(s, sx + 3, sy + 3);
        g2.setColor(new Color(250, 235, 205));
        g2.drawString(s, sx, sy);
    }
}
