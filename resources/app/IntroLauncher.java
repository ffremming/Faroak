package resources.app;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import resources.presentation.ui.IntroScreen;

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
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(true);
            frame.setTitle("Faroak");

            IntroScreen intro = new IntroScreen(frame);
            frame.getContentPane().add(intro);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            frame.toFront();
            intro.requestFocusInWindow();
        });
    }
}
