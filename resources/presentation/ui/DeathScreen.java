package resources.presentation.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.RadialGradientPaint;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.imageio.ImageIO;

import resources.app.GamePanel;

/**
 * Full-screen "You Died" overlay shown when the local player dies. Visually
 * matches the {@link EscapeMenu} wooden-board buttons so the two menus read as
 * one consistent aesthetic, but it is a dedicated, decoupled widget: it can't
 * be dismissed with Resume and offers only two ways forward:
 *
 *   - Respawn    → revive at spawn (via {@link resources.domain.player.PlayerLifecycle},
 *                  or the server-authoritative respawn command online)
 *   - Main Menu  → leave the game and return to the title screen
 *
 * Like the escape menu, it covers the whole panel while visible so the parent
 * {@link Container}'s contains-based mouse dispatch routes clicks here; when
 * hidden it consumes nothing and falls through to the world.
 */
public final class DeathScreen extends Component {

    private static final String BOARDS_DIR = "resources/images/ui/UISprites/boards/";
    private static final int[] BOARD_INDICES = { 0, 1, 2, 3, 4, 5 };
    private static final int SIGN_BOARD_INDEX = 6;

    // Heavier blood-red wash than the pause menu's neutral dim, so death reads
    // as a distinct, grimmer state.
    private static final Color PANEL_BG = new Color(60, 0, 0, 170);

    // ---- title / sign palette + fonts ----
    private static final Color TITLE_SHADOW_DARK = new Color(40, 8, 8, 220);
    private static final Color TITLE_LIGHT       = new Color(255, 220, 210);
    private static final Color TITLE_SHADOW_BLACK = new Color(0, 0, 0, 200);
    private static final Font  SIGN_TITLE_FONT     = new Font("Serif", Font.BOLD, 44);
    private static final Font  FALLBACK_TITLE_FONT = new Font("Serif", Font.BOLD, 56);

    // ---- wooden button palette + font (mirrors EscapeMenu) ----
    private static final Color BTN_SHADOW       = new Color(0, 0, 0, 130);
    private static final Color WOOD_LIGHT       = new Color(149, 102, 60);
    private static final Color WOOD_DARK        = new Color(70, 44, 22);
    private static final Color GLOW_RGB         = new Color(255, 170, 80);
    private static final Color GLOW_TRANSPARENT = new Color(255, 170, 80, 0);
    private static final Color LABEL_SHADOW     = new Color(20, 10, 4, 220);
    private static final Color LABEL_HIGHLIGHT  = new Color(255, 220, 160, 130);
    private static final Color LABEL_HOVER      = new Color(255, 235, 190);
    private static final Color LABEL_NORMAL     = new Color(248, 220, 165);
    private static final Font  BUTTON_FONT      = new Font("Serif", Font.BOLD, 26);

    private static final int BTN_W = 320;
    private static final int BTN_H = 70;
    private static final int BTN_GAP = 14;
    private static final int SIGN_W = 460;

    private final GamePanel panel;
    private final long startNanos = System.nanoTime();
    private final List<WoodenButton> buttons = new ArrayList<>();
    private final BufferedImage signBoard;

    public DeathScreen(GamePanel panel) {
        super(panel);
        this.panel = panel;
        Random rng = new Random(0xDEADBEEFL);
        List<BufferedImage> boards = loadBoards();
        buttons.add(new WoodenButton("Respawn",   randomBoard(boards, rng), this::actionRespawn));
        buttons.add(new WoodenButton("Main Menu", randomBoard(boards, rng), this::actionQuit));
        signBoard = tryLoad(BOARDS_DIR + "board" + SIGN_BOARD_INDEX + ".png");
    }

    /**
     * Cover the whole panel while visible so the parent {@link Container}'s
     * contains-based mouse dispatch routes events here; fall through when hidden.
     */
    @Override
    public boolean contains(java.awt.Point p) {
        if (!visible) return false;
        return p.x >= 0 && p.x < panel.width && p.y >= 0 && p.y < panel.height;
    }

    // ---- entry points ----

    public void show() {
        visible = true;
        enable();
    }

    public void hide() {
        visible = false;
        disable();
    }

    public boolean isOpen() { return visible; }

    /**
     * Test/probe hook: lay out the buttons (normally done during draw) and
     * return the screen-space centre of the button with {@code label}, or null
     * if there is no such button. Lets a headless probe simulate a real click.
     */
    public java.awt.Point buttonCenter(String label) {
        layoutButtons();
        for (WoodenButton b : buttons) {
            if (b.label.equals(label)) return new java.awt.Point(b.x + b.w / 2, b.y + b.h / 2);
        }
        return null;
    }

    // ---- draw ----

    @Override
    public void draw(Graphics2D g2) {
        if (!visible) return;

        layoutButtons();

        g2.setColor(PANEL_BG);
        g2.fillRect(0, 0, panel.width, panel.height);

        drawHangingSign(g2);

        float t = (System.nanoTime() - startNanos) / 1_000_000_000f;
        for (WoodenButton b : buttons) b.draw(g2, t);
    }

    private void drawHangingSign(Graphics2D g2) {
        String title = "You Died";

        if (signBoard != null) {
            int w = SIGN_W;
            int h = (int) (signBoard.getHeight() * (SIGN_W / (float) signBoard.getWidth()));
            int sx = (panel.width - w) / 2;
            int sy = 0;
            g2.drawImage(signBoard, sx, sy, w, h, null);

            Font f = SIGN_TITLE_FONT;
            g2.setFont(f);
            int tw = g2.getFontMetrics().stringWidth(title);
            int lh = g2.getFontMetrics().getAscent();
            int tx = sx + (w - tw) / 2;
            int ty = sy + (int) (h * 0.42f) + lh / 2;
            g2.setColor(TITLE_SHADOW_DARK);
            g2.drawString(title, tx + 2, ty + 2);
            g2.setColor(TITLE_LIGHT);
            g2.drawString(title, tx, ty);
            return;
        }

        Font titleFont = FALLBACK_TITLE_FONT;
        g2.setFont(titleFont);
        int tw = g2.getFontMetrics().stringWidth(title);
        int tx = (panel.width - tw) / 2;
        int ty = panel.height / 2 - (buttons.size() * (BTN_H + BTN_GAP)) / 2 - 50;
        g2.setColor(TITLE_SHADOW_BLACK);
        g2.drawString(title, tx + 3, ty + 3);
        g2.setColor(TITLE_LIGHT);
        g2.drawString(title, tx, ty);
    }

    private void layoutButtons() {
        int totalH = buttons.size() * BTN_H + (buttons.size() - 1) * BTN_GAP;
        int x = (panel.width - BTN_W) / 2;
        int signH = signBoard != null
            ? (int) (signBoard.getHeight() * (SIGN_W / (float) signBoard.getWidth()))
            : 0;
        int y = Math.max((panel.height - totalH) / 2, signH + 30);
        for (WoodenButton b : buttons) {
            b.setBounds(x, y, BTN_W, BTN_H);
            y += BTN_H + BTN_GAP;
        }
    }

    // ---- mouse ----

    @Override
    public void mousePressed(MouseEvent e) {
        if (!visible) return;
        int mx = e.getX();
        int my = e.getY();
        for (WoodenButton b : buttons) {
            if (b.contains(mx, my)) {
                b.action.run();
                return;
            }
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if (!visible) return;
        int mx = e.getX();
        int my = e.getY();
        for (WoodenButton b : buttons) b.updateHover(mx, my);
    }

    // ---- actions ----

    private void actionRespawn() {
        // Online: the server owns the respawn — submit the command and let the
        // authoritative alive-state flip hide this screen. Single-player: drive
        // the lifecycle directly and hide immediately.
        if (panel.multiplayer() != null && panel.multiplayer().isOnline()) {
            panel.multiplayer().requestRespawn();
            return;
        }
        if (panel.player() != null && panel.player().lifecycle() != null) {
            panel.player().lifecycle().respawn();
        }
        hide();
    }

    /**
     * Leave the running game and return to the title screen (mirrors
     * {@link EscapeMenu}'s Main Menu): stop the loop / networking, then swap the
     * frame's content back to a fresh {@link IntroScreen} on the EDT.
     */
    private void actionQuit() {
        hide();
        panel.stopGameThread();
        javax.swing.JFrame frame = panel.frame;
        javax.swing.SwingUtilities.invokeLater(() -> {
            frame.getContentPane().removeAll();
            IntroScreen intro = new IntroScreen(frame);
            frame.getContentPane().add(intro);
            frame.revalidate();
            frame.repaint();
            intro.requestFocusInWindow();
        });
    }

    // ---- assets ----

    private static List<BufferedImage> loadBoards() {
        List<BufferedImage> out = new ArrayList<>();
        for (int i : BOARD_INDICES) {
            BufferedImage b = tryLoad(BOARDS_DIR + "board" + i + ".png");
            if (b != null) out.add(b);
        }
        return out;
    }

    private static BufferedImage randomBoard(List<BufferedImage> boards, Random rng) {
        if (boards.isEmpty()) return null;
        return boards.get(rng.nextInt(boards.size()));
    }

    private static BufferedImage tryLoad(String path) {
        try {
            File f = new File(path);
            if (f.exists()) return ImageIO.read(f);
        } catch (IOException ignored) {}
        return null;
    }

    // ---- nested wooden button (mirrors EscapeMenu.WoodenButton) ----

    private static final class WoodenButton {
        final String label;
        final BufferedImage board;
        final Runnable action;
        int x, y, w, h;
        boolean hover;

        WoodenButton(String label, BufferedImage board, Runnable action) {
            this.label  = label;
            this.board  = board;
            this.action = action;
        }

        void setBounds(int x, int y, int w, int h) {
            this.x = x; this.y = y; this.w = w; this.h = h;
        }

        boolean contains(int mx, int my) {
            return mx >= x && mx <= x + w && my >= y && my <= y + h;
        }

        void updateHover(int mx, int my) {
            hover = contains(mx, my);
        }

        void draw(Graphics2D g2, float t) {
            g2.setColor(BTN_SHADOW);
            g2.fillRoundRect(x + 4, y + 6, w, h, 14, 14);

            if (board != null) {
                g2.drawImage(board, x, y, w, h, null);
            } else {
                Paint old = g2.getPaint();
                g2.setPaint(new GradientPaint(x, y, WOOD_LIGHT,
                                              x, y + h, WOOD_DARK));
                g2.fillRoundRect(x, y, w, h, 12, 12);
                g2.setPaint(old);
            }

            if (hover) {
                float pulse = 0.5f + 0.5f * (float) Math.sin(t * 3.5);
                int glowA = 50 + (int) (pulse * 50);
                Paint old = g2.getPaint();
                g2.setPaint(new RadialGradientPaint(
                    new Point(x + w / 2, y + h / 2),
                    Math.max(w, h) * 0.7f,
                    new float[] { 0f, 1f },
                    new Color[] {
                        new Color(GLOW_RGB.getRed(), GLOW_RGB.getGreen(), GLOW_RGB.getBlue(), glowA),
                        GLOW_TRANSPARENT
                    }));
                g2.fillRoundRect(x, y, w, h, 12, 12);
                g2.setPaint(old);
            }

            Font f = BUTTON_FONT;
            g2.setFont(f);
            int lw = g2.getFontMetrics().stringWidth(label);
            int lh = g2.getFontMetrics().getAscent();
            int tx = x + (w - lw) / 2;
            int ty = y + (h + lh) / 2 - 4;

            g2.setColor(LABEL_SHADOW);
            g2.drawString(label, tx + 1, ty + 2);
            g2.setColor(LABEL_HIGHLIGHT);
            g2.drawString(label, tx, ty - 1);
            g2.setColor(hover ? LABEL_HOVER : LABEL_NORMAL);
            g2.drawString(label, tx, ty);
        }
    }
}
