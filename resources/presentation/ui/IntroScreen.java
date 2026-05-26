package resources.presentation.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * Main menu / title screen. Static farm background + wooden buttons.
 * Clicking New Game / Load Game hands off to a {@link LoadingScreen}
 * that runs while {@code GamePanel} initialises.
 */
public class IntroScreen extends JPanel {

    private static final int W = 64 * 20;
    private static final int H = 64 * 12;

    private static final String TITLE      = "Faroak";
    private static final String SAVE_PATH  = "storage.txt";
    private static final String ASSETS     = "resources/images/ui/MainMenu/";
    private static final String BOARDS_DIR = "resources/images/ui/UISprites/boards/";

    /** Indices of boards to use as button backgrounds (boards 6 and 7 have ropes). */
    private static final int[] BOARD_INDICES = { 0, 1, 2, 3, 4, 5 };

    private final JFrame frame;
    private BufferedImage background;

    private final IntroButton newGameBtn;
    private final IntroButton loadGameBtn;
    private final IntroButton hostGameBtn;
    private final IntroButton joinGameBtn;
    private final IntroButton quitBtn;

    private long startNanos = System.nanoTime();

    public IntroScreen(JFrame frame) {
        this.frame = frame;
        setPreferredSize(new Dimension(W, H));
        setBackground(Color.black);
        setDoubleBuffered(true);
        setFocusable(true);

        background = tryLoad(ASSETS + "map.png");

        // Load all available board PNGs and pick a random one per button.
        java.util.List<BufferedImage> boards = new java.util.ArrayList<>();
        for (int i : BOARD_INDICES) {
            BufferedImage b = tryLoad(BOARDS_DIR + "board" + i + ".png");
            if (b != null) boards.add(b);
        }
        java.util.Random rng = new java.util.Random();

        int bw = 300, bh = 60, gap = 10;
        int bx = (W - bw) / 2;
        int by = (int) (H * 0.45);
        newGameBtn   = new IntroButton("New Game",   bx, by,                  bw, bh, true,
                                       randomBoard(boards, rng));
        loadGameBtn  = new IntroButton("Load Game",  bx, by + (bh + gap),     bw, bh, saveExists(),
                                       randomBoard(boards, rng));
        hostGameBtn  = new IntroButton("Host Game",  bx, by + (bh + gap) * 2, bw, bh, true,
                                       randomBoard(boards, rng));
        joinGameBtn  = new IntroButton("Join Game",  bx, by + (bh + gap) * 3, bw, bh, true,
                                       randomBoard(boards, rng));
        quitBtn      = new IntroButton("Quit",       bx, by + (bh + gap) * 4, bw, bh, true,
                                       randomBoard(boards, rng));

        MouseAdapter mouse = new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e)   { updateHover(e.getX(), e.getY()); repaint(); }
            @Override public void mouseDragged(MouseEvent e) { updateHover(e.getX(), e.getY()); repaint(); }
            @Override public void mousePressed(MouseEvent e) {
                if (newGameBtn.contains(e.getX(), e.getY()) && newGameBtn.enabled)         startNewGame();
                else if (loadGameBtn.contains(e.getX(), e.getY()) && loadGameBtn.enabled)  loadGame();
                else if (hostGameBtn.contains(e.getX(), e.getY()) && hostGameBtn.enabled)  openLobby(LobbyScreen.Mode.HOST);
                else if (joinGameBtn.contains(e.getX(), e.getY()) && joinGameBtn.enabled)  openLobby(LobbyScreen.Mode.JOIN);
                else if (quitBtn.contains(e.getX(), e.getY()) && quitBtn.enabled)          System.exit(0);
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);

        // Light repaint loop so the hover pulse animates.
        new javax.swing.Timer(50, e -> repaint()).start();
    }

    private BufferedImage tryLoad(String path) {
        try {
            File f = new File(path);
            if (f.exists()) return ImageIO.read(f);
        } catch (IOException ignored) {}
        return null;
    }

    private static BufferedImage randomBoard(java.util.List<BufferedImage> boards,
                                             java.util.Random rng) {
        if (boards == null || boards.isEmpty()) return null;
        return boards.get(rng.nextInt(boards.size()));
    }

    private void updateHover(int mx, int my) {
        boolean any = false;
        any |= newGameBtn.updateHover(mx, my);
        any |= loadGameBtn.updateHover(mx, my);
        any |= hostGameBtn.updateHover(mx, my);
        any |= joinGameBtn.updateHover(mx, my);
        any |= quitBtn.updateHover(mx, my);
        setCursor(Cursor.getPredefinedCursor(any ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    private boolean saveExists() {
        File f = new File(SAVE_PATH);
        return f.exists() && f.length() > 0;
    }

    private void startNewGame() {
        LoadingScreen.show(frame, /*loadExisting=*/false);
    }

    private void loadGame() {
        LoadingScreen.show(frame, /*loadExisting=*/true);
    }

    private void openLobby(LobbyScreen.Mode mode) {
        LobbyScreen.show(frame, mode);
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

        if (background != null) {
            g2.drawImage(background, 0, 0, W, H, null);
            // soft vignette so buttons read against the busy background
            g2.setPaint(new java.awt.RadialGradientPaint(
                W / 2f, H / 2f, Math.max(W, H) * 0.7f,
                new float[] { 0f, 1f },
                new Color[] { new Color(0, 0, 0, 0), new Color(0, 0, 0, 140) }));
            g2.fillRect(0, 0, W, H);
        } else {
            g2.setPaint(new GradientPaint(0, 0, new Color(40, 60, 90),
                                          0, H, new Color(10, 20, 30)));
            g2.fillRect(0, 0, W, H);
        }

        drawTitle(g2);
        float t = (System.nanoTime() - startNanos) / 1_000_000_000f;
        newGameBtn.draw(g2, t);
        loadGameBtn.draw(g2, t);
        hostGameBtn.draw(g2, t);
        joinGameBtn.draw(g2, t);
        quitBtn.draw(g2, t);
        drawFootnote(g2);

        g2.dispose();
    }

    private void drawTitle(Graphics2D g2) {
        Font titleFont = new Font("Serif", Font.BOLD, 84);
        g2.setFont(titleFont);
        int tw = g2.getFontMetrics().stringWidth(TITLE);
        int tx = (W - tw) / 2;
        int ty = (int) (H * 0.28);

        for (int i = 6; i > 0; i--) {
            g2.setColor(new Color(255, 220, 160, 10));
            g2.drawString(TITLE, tx - i, ty);
            g2.drawString(TITLE, tx + i, ty);
            g2.drawString(TITLE, tx, ty - i);
            g2.drawString(TITLE, tx, ty + i);
        }
        g2.setColor(new Color(0, 0, 0, 200));
        g2.drawString(TITLE, tx + 3, ty + 3);
        g2.setColor(new Color(250, 235, 205));
        g2.drawString(TITLE, tx, ty);

        Font sub = new Font("SansSerif", Font.ITALIC, 18);
        g2.setFont(sub);
        String s = "a new adventure";
        int sw = g2.getFontMetrics().stringWidth(s);
        g2.setColor(new Color(0, 0, 0, 180));
        g2.drawString(s, (W - sw) / 2 + 1, ty + 31);
        g2.setColor(new Color(235, 220, 200, 220));
        g2.drawString(s, (W - sw) / 2, ty + 30);
    }

    private void drawFootnote(Graphics2D g2) {
        if (saveExists()) return;
        g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g2.setColor(new Color(220, 220, 220, 200));
        String s = "(no saved game found)";
        int sw = g2.getFontMetrics().stringWidth(s);
        g2.drawString(s, (W - sw) / 2, loadGameBtn.y + loadGameBtn.h + 18);
    }

    // ---------- wooden button (PNG-backed) ----------

    static final class IntroButton {
        final String label;
        final int x, y, w, h;
        boolean hover;
        boolean enabled;
        final BufferedImage board;

        IntroButton(String label, int x, int y, int w, int h, boolean enabled,
                    BufferedImage board) {
            this.label = label;
            this.x = x; this.y = y; this.w = w; this.h = h;
            this.enabled = enabled;
            this.board = board;
        }

        boolean contains(int mx, int my) {
            return mx >= x && mx <= x + w && my >= y && my <= y + h;
        }

        boolean updateHover(int mx, int my) {
            hover = enabled && contains(mx, my);
            return hover;
        }

        void draw(Graphics2D g2, float t) {
            // Drop shadow under the board
            g2.setColor(new Color(0, 0, 0, 130));
            g2.fillRoundRect(x + 4, y + 6, w, h, 14, 14);

            if (board != null) {
                g2.drawImage(board, x, y, w, h, null);
            } else {
                g2.setColor(new Color(110, 72, 40));
                g2.fillRoundRect(x, y, w, h, 12, 12);
            }

            // Disabled: dim/desaturate overlay (clipped to the board's approximate shape)
            if (!enabled) {
                g2.setColor(new Color(30, 30, 35, 160));
                g2.fillRoundRect(x, y, w, h, 12, 12);
            }

            // Hover: pulsing warm firelight glow
            if (hover) {
                float pulse = 0.5f + 0.5f * (float) Math.sin(t * 3.5);
                int glowA = 50 + (int) (pulse * 50);
                java.awt.Paint oldPaint = g2.getPaint();
                java.awt.RadialGradientPaint glow = new java.awt.RadialGradientPaint(
                    x + w / 2f, y + h / 2f, Math.max(w, h) * 0.7f,
                    new float[] { 0f, 1f },
                    new Color[] {
                        new Color(255, 170, 80, glowA),
                        new Color(255, 170, 80, 0)
                    });
                g2.setPaint(glow);
                g2.fillRoundRect(x, y, w, h, 12, 12);
                g2.setPaint(oldPaint);
            }

            // Label (engraved look: dark drop + warm highlight + main fill)
            Font f = new Font("Serif", Font.BOLD, 26);
            g2.setFont(f);
            int lw = g2.getFontMetrics().stringWidth(label);
            int lh = g2.getFontMetrics().getAscent();
            int tx = x + (w - lw) / 2;
            int ty = y + (h + lh) / 2 - 4;

            if (enabled) {
                g2.setColor(new Color(20, 10, 4, 220));
                g2.drawString(label, tx + 1, ty + 2);
                g2.setColor(new Color(255, 220, 160, 130));
                g2.drawString(label, tx, ty - 1);
                g2.setColor(hover ? new Color(255, 235, 190) : new Color(248, 220, 165));
                g2.drawString(label, tx, ty);
            } else {
                g2.setColor(new Color(0, 0, 0, 200));
                g2.drawString(label, tx + 1, ty + 1);
                g2.setColor(new Color(150, 145, 140));
                g2.drawString(label, tx, ty);
            }
        }
    }
}
