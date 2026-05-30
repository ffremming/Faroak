package resources.app;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import resources.presentation.ui.IntroScreen;
import resources.presentation.ui.LoadingScreen;

/**
 * Separate entry point that shows the {@link IntroScreen} first.
 * {@link Main} can call {@link #launch()} (or you can run this class
 * directly) to start the game from the title screen rather than dropping
 * straight into a new game.
 */
public class IntroLauncher {

    public static void main(String[] args) {
        launch();
    }

    /** Opens the intro screen in a new JFrame on the EDT. Safe to call from anywhere. */
    public static void launch() {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame();
            if (parseBoolean(System.getProperty("game.test.undecorated", "false"), false)) {
                frame.setUndecorated(true);
            }
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(true);
            frame.setTitle("Faroak");

            if (parseBoolean(System.getProperty("game.test.autostart", "false"), false)) {
                frame.setSize(64 * 20, 64 * 12);
                if (!applyConfiguredLocation(frame)) {
                    frame.setLocationRelativeTo(null);
                }
                frame.setVisible(true);
                frame.toFront();
                LoadingScreen.show(frame, /*loadExisting=*/false);
                return;
            }

            IntroScreen intro = new IntroScreen(frame);
            frame.getContentPane().add(intro);
            frame.pack();
            if (!applyConfiguredLocation(frame)) {
                frame.setLocationRelativeTo(null);
            }
            frame.setVisible(true);
            frame.toFront();
            intro.requestFocusInWindow();
        });
    }

    private static boolean applyConfiguredLocation(JFrame frame) {
        String sx = System.getProperty("game.window.x", "").trim();
        String sy = System.getProperty("game.window.y", "").trim();
        if (sx.isBlank() || sy.isBlank()) return false;
        try {
            frame.setLocation(Integer.parseInt(sx), Integer.parseInt(sy));
            return true;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private static boolean parseBoolean(String raw, boolean fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        if ("true".equalsIgnoreCase(raw.trim())) return true;
        if ("false".equalsIgnoreCase(raw.trim())) return false;
        return fallback;
    }
}
