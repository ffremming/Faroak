package resources.input;

import resources.app.GamePanel;
import resources.input.click.ClickRouter;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;


public class Mouse implements MouseListener, MouseMotionListener, MouseWheelListener {

    /** Minimum wall-clock gap between two world-click actions (ms). Prevents
     *  rapid-fire clicks (and any AWT double-press quirks) from dropping
     *  multiple entities at the cursor. */
    private static final long PLACE_COOLDOWN_MS        = 150L;
    /** Shorter throttle in multiplayer: the local click is optimistic and the
     *  server may reject it, so we don't lock the player out of legit retries. */
    private static final long PLACE_COOLDOWN_MS_ONLINE = 60L;

    int x = 0;
    int y = 0;
    GamePanel panel;
    private long lastPlaceMs;

    /** Ordered chain that decides what an offline left-click does. */
    private final ClickRouter clickRouter = new ClickRouter();

    public Mouse(GamePanel panel){
        this.panel = panel;
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        // Wheel cycles the hotbar — but only when no modal UI is taking focus.
        if (panel.userInterface != null && panel.userInterface.isModalUIOpen()) {
            panel.userInterface.mouseWheelMoved(e);
            return;
        }
        if (e.getWheelRotation() < 0) {
            panel.player.getInventory().decreaseIndex();
        } else if (e.getWheelRotation() > 0) {
            panel.player.getInventory().increseIndex();
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        panel.userInterface.mouseDragged(e);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        this.x = e.getX();
        this.y = e.getY();
        panel.UI.mouseMoved(e);
        panel.userInterface.mouseMoved(e);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        panel.userInterface.mouseClicked(e);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        // Clicking the game area reclaims keyboard focus so Escape / WASD keep
        // working even if focus drifted to the window chrome or another component.
        panel.requestFocusInWindow();

        // UI takes precedence: a modal UI (inventory, menu, container overlay)
        // owns the click and it never reaches the world.
        if (panel.userInterface.isModalUIOpen()) {
            panel.userInterface.mousePressed(e);
            return;
        }
        // Left button = act on the world. Other buttons do nothing on the world layer.
        if (e.getButton() != MouseEvent.BUTTON1) return;

        boolean offline = panel.multiplayer() == null || !panel.multiplayer().isOnline();
        long cooldown = offline ? PLACE_COOLDOWN_MS : PLACE_COOLDOWN_MS_ONLINE;
        long now = System.currentTimeMillis();
        if (now - lastPlaceMs < cooldown) return;

        if (offline) {
            // All cursor-targeted world actions flow through the click chain:
            // board boat → open container → use equipped item → harvest.
            Point worldPoint = new Point(
                (int) panel.camera.getWorldX() + e.getX(),
                (int) panel.camera.getWorldY() + e.getY());
            if (clickRouter.route(panel, worldPoint)) lastPlaceMs = now;
        } else {
            // Online: the server applies the intent. Stamp the cooldown either
            // way so a rejected intent can't open rapid-fire retries.
            panel.inputHandlingSystem.enqueueAction(InputAction.PLACE);
            lastPlaceMs = now;
        }
    }

    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}

    public int getX() { return x; }
    public int getY() { return y; }

    public double getMouseWorldX(){ return panel.camera.getWorldX() + getX(); }
    public double getMouseWorldY(){ return panel.camera.getWorldY() + getY(); }
}
