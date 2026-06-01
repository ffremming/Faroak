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

import resources.app.AudioSettings;
import resources.app.GamePanel;

/**
 * In-game pause / escape menu. Visually matches the intro screen's wooden
 * board buttons so the player gets one consistent menu aesthetic. Buttons:
 *
 *   - Resume            → close the menu
 *   - Music: N%         → cycle music volume
 *   - Sound: N%         → cycle SFX volume
 *   - Main Menu         → leave the game and return to the title screen
 *
 * Respawn lives on the dedicated {@link DeathScreen} overlay (shown only while
 * dead), not here — the pause menu is for a living player.
 *
 * Mouse handling is self-contained — the menu intercepts presses inside its
 * panel and dispatches to the relevant button. Outside the panel, presses
 * fall through so the underlying UI / placement system still gets them when
 * the menu is hidden.
 */
public final class EscapeMenu extends Component {

    private static final String BOARDS_DIR = "resources/images/ui/UISprites/boards/";
    // Boards 6 and 7 have ropes (they're hanging signs), so they're excluded
    // from the flat button pool and reserved for the hanging title sign.
    private static final int[] BOARD_INDICES = { 0, 1, 2, 3, 4, 5 };
    private static final int SIGN_BOARD_INDEX = 6;

    private static final Color PANEL_BG = new Color(0, 0, 0, 140);

    // ---- title / sign palette + fonts ----
    private static final Color TITLE_SHADOW_DARK  = new Color(40, 22, 8, 220);
    private static final Color TITLE_LIGHT        = new Color(250, 235, 205);
    private static final Color TITLE_SHADOW_BLACK = new Color(0, 0, 0, 200);
    private static final Font  SIGN_TITLE_FONT     = new Font("Serif", Font.BOLD, 44);
    private static final Font  FALLBACK_TITLE_FONT = new Font("Serif", Font.BOLD, 56);

    // ---- wooden button palette + font ----
    private static final Color BTN_SHADOW      = new Color(0, 0, 0, 130);
    private static final Color WOOD_LIGHT      = new Color(149, 102, 60);
    private static final Color WOOD_DARK       = new Color(70, 44, 22);
    private static final Color GLOW_RGB        = new Color(255, 170, 80);
    private static final Color GLOW_TRANSPARENT = new Color(255, 170, 80, 0);
    private static final Color LABEL_SHADOW    = new Color(20, 10, 4, 220);
    private static final Color LABEL_HIGHLIGHT = new Color(255, 220, 160, 130);
    private static final Color LABEL_HOVER     = new Color(255, 235, 190);
    private static final Color LABEL_NORMAL    = new Color(248, 220, 165);
    private static final Font  BUTTON_FONT     = new Font("Serif", Font.BOLD, 26);

    private static final int BTN_W = 320;
    private static final int BTN_H = 70;
    private static final int BTN_GAP = 14;

    /** Hanging sign drawn flush against the top edge; its ropes read as if it
     *  dangles from the top of the screen. Width is fixed; height follows the
     *  source aspect ratio. */
    private static final int SIGN_W = 460;

    private final GamePanel panel;
    private final long startNanos = System.nanoTime();
    private final List<WoodenButton> buttons = new ArrayList<>();
    private final WoodenButton musicButton;
    private final WoodenButton soundButton;
    private final BufferedImage signBoard;

    public EscapeMenu(GamePanel panel) {
        super(panel);
        this.panel = panel;
        Random rng = new Random(0xCAFEBABEL);
        List<BufferedImage> boards = loadBoards();
        buttons.add(new WoodenButton("Resume",        randomBoard(boards, rng), this::actionResume));
        musicButton = new WoodenButton("", randomBoard(boards, rng), this::actionMusic);
        soundButton = new WoodenButton("", randomBoard(boards, rng), this::actionSound);
        buttons.add(musicButton);
        buttons.add(soundButton);
        buttons.add(new WoodenButton("Main Menu",      randomBoard(boards, rng), this::actionQuit));
        signBoard = tryLoad(BOARDS_DIR + "board" + SIGN_BOARD_INDEX + ".png");
        refreshAudioLabels();
    }

    /**
     * Make the menu's rect cover the whole panel while visible so the parent
     * {@link Container}'s contains-based mouse dispatch routes events here.
     * Returns false when hidden so clicks fall through to the world.
     */
    @Override
    public boolean contains(java.awt.Point p) {
        if (!visible) return false;
        return p.x >= 0 && p.x < panel.width && p.y >= 0 && p.y < panel.height;
    }

    // ---- entry points ----

    /** Show the menu (visible + enabled). */
    public void show() {
        visible = true;
        enable();
    }

    /** Hide the menu. */
    public void hide() {
        visible = false;
        disable();
    }

    /** Toggle visibility. */
    public void toggle() {
        if (visible) hide();
        else         show();
    }

    public boolean isOpen() { return visible; }

    // ---- draw ----

    @Override
    public void draw(Graphics2D g2) {
        if (!visible) return;

        layoutButtons();

        // Full-screen dim so the player can tell the world is paused.
        g2.setColor(PANEL_BG);
        g2.fillRect(0, 0, panel.width, panel.height);

        drawHangingSign(g2);

        float t = (System.nanoTime() - startNanos) / 1_000_000_000f;
        for (WoodenButton b : buttons) b.draw(g2, t);
    }

    /**
     * Draws the "Paused" title on a wooden sign that hangs by its ropes from the
     * top edge of the screen. The source board (board6) already includes the
     * ropes running up to its top edge, so anchoring the image flush to y=0 makes
     * it read as if it dangles from the top of the screen.
     */
    private void drawHangingSign(Graphics2D g2) {
        String title = "Paused";

        if (signBoard != null) {
            int w = SIGN_W;
            int h = (int) (signBoard.getHeight() * (SIGN_W / (float) signBoard.getWidth()));
            int sx = (panel.width - w) / 2;
            int sy = 0;
            g2.drawImage(signBoard, sx, sy, w, h, null);

            // Title sits on the plank face, below the ropes. The board art
            // reserves roughly the top third for the ropes/nails.
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

        // Fallback: no sign art — keep a plain floating title.
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
        // Centre the button stack, but bias it downward so it clears the
        // hanging sign at the top of the screen.
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

    private void actionResume() { hide(); }

    private void cycleAudio(Runnable cycler) {
        cycler.run();
        panel.syncAudio();
        refreshAudioLabels();
    }

    private void actionMusic() {
        cycleAudio(() -> panel.audioSettings().cycleMusicVolume());
    }

    private void actionSound() {
        cycleAudio(() -> panel.audioSettings().cycleSoundVolume());
    }

    /**
     * Leave the running game and return to the title screen rather than killing
     * the process. Stops the game loop / networking, then swaps the frame's
     * content back to a fresh {@link IntroScreen} on the EDT.
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

    private void refreshAudioLabels() {
        AudioSettings audio = panel.audioSettings();
        musicButton.setLabel("Music: " + audio.musicVolume() + "%");
        soundButton.setLabel("Sound: " + audio.soundVolume() + "%");
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

    // ---- nested wooden button (mirrors IntroScreen.IntroButton) ----

    private static final class WoodenButton {
        String label;
        final BufferedImage board;
        final Runnable action;
        int x, y, w, h;
        boolean hover;

        WoodenButton(String label, BufferedImage board, Runnable action) {
            this.label  = label;
            this.board  = board;
            this.action = action;
        }

        void setLabel(String label) {
            this.label = label;
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
