package resources.presentation.ui;

import resources.presentation.ui.IntroScreen.IntroButton;

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
import java.util.Random;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Lobby screen for hosting or joining an online game.
 */
public class LobbyScreen extends JPanel {

    public enum Mode { HOST, JOIN }

    private static final int W = 64 * 20;
    private static final int H = 64 * 12;

    private static final String ASSETS     = "resources/images/ui/MainMenu/";
    private static final String BOARDS_DIR = "resources/images/ui/UISprites/boards/";
    private static final int[]  BOARD_INDICES = { 0, 1, 2, 3, 4, 5 };

    /** Default networking backend identifier for online play. */
    private static final String DEFAULT_BACKEND = "websocket";

    private final JFrame frame;
    private final Mode mode;
    private final BufferedImage background;

    private final JTextField nameField;
    private final JTextField portField;
    private final JTextField hostField;          // JOIN only
    private final JTextField maxPlayersField;    // HOST only
    private final String lobbyCode;              // HOST only

    private final IntroButton primaryBtn; // Start Game (host) or Connect (join)
    private final IntroButton backBtn;

    private final long startNanos = System.nanoTime();

    public static void show(JFrame frame, Mode mode) {
        LobbyScreen screen = new LobbyScreen(frame, mode);
        frame.getContentPane().removeAll();
        frame.getContentPane().add(screen);
        frame.revalidate();
        frame.repaint();
        screen.requestFocusInWindow();
    }

    private LobbyScreen(JFrame frame, Mode mode) {
        this.frame = frame;
        this.mode = mode;
        setLayout(null); // absolute positioning for text fields
        setPreferredSize(new Dimension(W, H));
        setBackground(Color.black);
        setDoubleBuffered(true);
        setFocusable(true);

        background = tryLoad(ASSETS + "farm.png");

        // Pre-load boards for buttons
        java.util.List<BufferedImage> boards = new java.util.ArrayList<>();
        for (int i : BOARD_INDICES) {
            BufferedImage b = tryLoad(BOARDS_DIR + "board" + i + ".png");
            if (b != null) boards.add(b);
        }
        Random rng = new Random();

        // ---- Text fields ----
        int fieldW = 320, fieldH = 36;
        int labelW = 130;
        int fx = (W - (labelW + 10 + fieldW)) / 2 + labelW + 10;
        int fy = (int) (H * 0.40);
        int rowGap = 50;

        nameField = createField(suggestedName());
        nameField.setBounds(fx, fy, fieldW, fieldH);
        add(nameField);

        if (mode == Mode.HOST) {
            portField = createField("7777");
            portField.setBounds(fx, fy + rowGap, fieldW, fieldH);
            add(portField);

            maxPlayersField = createField("10");
            maxPlayersField.setBounds(fx, fy + rowGap * 2, fieldW, fieldH);
            add(maxPlayersField);

            hostField = null;
            lobbyCode = generateLobbyCode();
        } else {
            hostField = createField("localhost");
            hostField.setBounds(fx, fy + rowGap, fieldW, fieldH);
            add(hostField);

            portField = createField("7777");
            portField.setBounds(fx, fy + rowGap * 2, fieldW, fieldH);
            add(portField);

            maxPlayersField = null;
            lobbyCode = null;
        }

        // ---- Buttons ----
        int bw = 240, bh = 60, bgap = 30;
        int by = (int) (H * 0.82);
        int totalW = bw * 2 + bgap;
        int bx1 = (W - totalW) / 2;
        int bx2 = bx1 + bw + bgap;
        String primaryLabel = (mode == Mode.HOST) ? "Start Game" : "Connect";
        primaryBtn = new IntroButton(primaryLabel, bx1, by, bw, bh, true, randomBoard(boards, rng));
        backBtn    = new IntroButton("Back",       bx2, by, bw, bh, true, randomBoard(boards, rng));

        MouseAdapter mouse = new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e)   { updateHover(e.getX(), e.getY()); repaint(); }
            @Override public void mouseDragged(MouseEvent e) { updateHover(e.getX(), e.getY()); repaint(); }
            @Override public void mousePressed(MouseEvent e) {
                if (primaryBtn.contains(e.getX(), e.getY())) onPrimary();
                else if (backBtn.contains(e.getX(), e.getY())) onBack();
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);

        new javax.swing.Timer(50, e -> repaint()).start();
    }

    private JTextField createField(String initial) {
        JTextField tf = new JTextField(initial);
        tf.setFont(new Font("SansSerif", Font.PLAIN, 18));
        tf.setForeground(new Color(250, 235, 205));
        tf.setBackground(new Color(20, 14, 10, 220));
        tf.setCaretColor(new Color(250, 235, 205));
        tf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(180, 160, 110), 2),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        tf.setOpaque(true);
        return tf;
    }

    private BufferedImage tryLoad(String path) {
        try {
            File f = new File(path);
            if (f.exists()) return ImageIO.read(f);
        } catch (IOException e) { System.err.println("[LobbyScreen] tryLoad failed for " + path + ": " + e); }
        return null;
    }

    private static BufferedImage randomBoard(java.util.List<BufferedImage> boards, Random rng) {
        if (boards == null || boards.isEmpty()) return null;
        return boards.get(rng.nextInt(boards.size()));
    }

    private static String suggestedName() {
        String[] adjectives = { "Brave", "Quiet", "Quick", "Wild", "Fierce", "Wise", "Bold" };
        String[] nouns      = { "Wolf", "Bear", "Fox", "Hawk", "Otter", "Lynx", "Stag" };
        Random r = new Random();
        return adjectives[r.nextInt(adjectives.length)] + nouns[r.nextInt(nouns.length)];
    }

    private static String generateLobbyCode() {
        // 6-char alphanumeric code derived from a fresh UUID
        return UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
    }

    private void updateHover(int mx, int my) {
        boolean any = false;
        any |= primaryBtn.updateHover(mx, my);
        any |= backBtn.updateHover(mx, my);
        setCursor(Cursor.getPredefinedCursor(any ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    private void onPrimary() {
        String playerName = trimOr(nameField.getText(), suggestedName());
        System.setProperty("game.multiplayer.playerId", playerName);
        System.setProperty("game.multiplayer.backend", DEFAULT_BACKEND);
        System.setProperty("game.multiplayer.gateway.enabled", "true");
        System.setProperty("game.multiplayer.reconcileLocal", "true");
        // Match local player movement so remote avatars don't appear to move
        // in slow motion relative to what each client controls.
        System.setProperty("game.multiplayer.serverMoveSpeedPerTick", "10.0");

        if (mode == Mode.HOST) {
            System.setProperty("game.multiplayer.mode", "host");
            int maxPlayers = parseIntOr(maxPlayersField.getText(), 10);
            System.setProperty("game.multiplayer.maxPlayers", Integer.toString(maxPlayers));
            int port = parseIntOr(portField.getText(), 7777);
            System.setProperty("game.multiplayer.gatewayPort", Integer.toString(port));
            System.setProperty("game.multiplayer.serverUrl", "ws://127.0.0.1:" + port + "/ws");
        } else {
            System.setProperty("game.multiplayer.mode", "client");
            String host = trimOr(hostField.getText(), "localhost");
            int port = parseIntOr(portField.getText(), 7777);
            System.setProperty("game.multiplayer.host", host);
            System.setProperty("game.multiplayer.port", Integer.toString(port));
            System.setProperty("game.multiplayer.serverUrl", "ws://" + host + ":" + port + "/ws");
        }

        LoadingScreen.show(frame, /*loadExisting=*/false);
    }

    private void onBack() {
        // Return to the main menu.
        frame.getContentPane().removeAll();
        IntroScreen intro = new IntroScreen(frame);
        frame.getContentPane().add(intro);
        frame.revalidate();
        frame.repaint();
        intro.requestFocusInWindow();
    }

    private static String trimOr(String s, String fallback) {
        if (s == null) return fallback;
        s = s.trim();
        return s.isEmpty() ? fallback : s;
    }

    private static int parseIntOr(String s, int fallback) {
        if (s == null) return fallback;
        try { return Integer.parseInt(s.trim()); }
        catch (NumberFormatException e) { return fallback; }
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

        // Background
        if (background != null) {
            g2.drawImage(background, 0, 0, W, H, null);
        } else {
            g2.setPaint(new GradientPaint(0, 0, new Color(40, 60, 90),
                                          0, H, new Color(10, 20, 30)));
            g2.fillRect(0, 0, W, H);
        }
        // Heavier vignette than the intro so the form reads against the busy farm.
        g2.setPaint(new java.awt.RadialGradientPaint(
            W / 2f, H / 2f, Math.max(W, H) * 0.6f,
            new float[] { 0f, 1f },
            new Color[] { new Color(0, 0, 0, 80), new Color(0, 0, 0, 200) }));
        g2.fillRect(0, 0, W, H);

        // Form panel: a darker translucent slab behind the fields
        int panelW = 640, panelH = 360;
        int panelX = (W - panelW) / 2;
        int panelY = (int) (H * 0.22);
        g2.setColor(new Color(0, 0, 0, 160));
        g2.fillRoundRect(panelX - 2, panelY - 2, panelW + 4, panelH + 4, 18, 18);
        g2.setPaint(new GradientPaint(0, panelY, new Color(40, 30, 22, 220),
                                      0, panelY + panelH, new Color(20, 14, 10, 220)));
        g2.fillRoundRect(panelX, panelY, panelW, panelH, 16, 16);
        g2.setColor(new Color(180, 160, 110));
        g2.drawRoundRect(panelX, panelY, panelW, panelH, 16, 16);

        // Title
        drawHeading(g2, mode == Mode.HOST ? "Host Online Game" : "Join Online Game",
                    panelY - 60);

        // Field labels and lobby-code chip
        drawLabels(g2, panelX);
        if (mode == Mode.HOST && lobbyCode != null) {
            drawLobbyCode(g2, panelX + panelW + 24, panelY);
        }

        // Hint
        Font hint = new Font("SansSerif", Font.ITALIC, 13);
        g2.setFont(hint);
        g2.setColor(new Color(220, 210, 190, 200));
        String hintText = mode == Mode.HOST
            ? "Host starts embedded websocket server for this session."
            : "Connect to host websocket endpoint.";
        int hw = g2.getFontMetrics().stringWidth(hintText);
        g2.drawString(hintText, (W - hw) / 2, panelY + panelH + 30);

        // Buttons
        float t = (System.nanoTime() - startNanos) / 1_000_000_000f;
        primaryBtn.draw(g2, t);
        backBtn.draw(g2, t);

        g2.dispose();
    }

    private void drawHeading(Graphics2D g2, String text, int y) {
        Font titleFont = new Font("Serif", Font.BOLD, 52);
        g2.setFont(titleFont);
        int tw = g2.getFontMetrics().stringWidth(text);
        int tx = (W - tw) / 2;
        int ty = y;
        g2.setColor(new Color(0, 0, 0, 220));
        g2.drawString(text, tx + 2, ty + 2);
        g2.setColor(new Color(250, 235, 205));
        g2.drawString(text, tx, ty);
    }

    private void drawLabels(Graphics2D g2, int panelX) {
        Font label = new Font("SansSerif", Font.BOLD, 17);
        g2.setFont(label);
        g2.setColor(new Color(245, 230, 200));

        int labelX = panelX + 30;
        int fy = (int) (H * 0.40);
        int rowGap = 50;

        g2.drawString("Player name", labelX, fy + 24);
        if (mode == Mode.HOST) {
            g2.drawString("Port",         labelX, fy + rowGap + 24);
            g2.drawString("Max players",  labelX, fy + rowGap * 2 + 24);
        } else {
            g2.drawString("Host address", labelX, fy + rowGap + 24);
            g2.drawString("Port",         labelX, fy + rowGap * 2 + 24);
        }
    }

    private void drawLobbyCode(Graphics2D g2, int x, int y) {
        int w = 200, h = 100;
        g2.setColor(new Color(0, 0, 0, 160));
        g2.fillRoundRect(x - 2, y - 2, w + 4, h + 4, 14, 14);
        g2.setPaint(new GradientPaint(0, y, new Color(50, 36, 22, 230),
                                      0, y + h, new Color(28, 20, 12, 230)));
        g2.fillRoundRect(x, y, w, h, 12, 12);
        g2.setColor(new Color(180, 160, 110));
        g2.drawRoundRect(x, y, w, h, 12, 12);

        Font small = new Font("SansSerif", Font.PLAIN, 12);
        g2.setFont(small);
        g2.setColor(new Color(220, 200, 160));
        g2.drawString("LOBBY CODE", x + 12, y + 22);

        Font code = new Font("Monospaced", Font.BOLD, 30);
        g2.setFont(code);
        g2.setColor(new Color(255, 235, 190));
        int cw = g2.getFontMetrics().stringWidth(lobbyCode);
        g2.drawString(lobbyCode, x + (w - cw) / 2, y + 70);
    }
}
