package resources.testing.probes;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import resources.app.GamePanel;
import resources.domain.player.Playable;
import resources.domain.player.PlayerLifecycle;
import resources.presentation.ui.DeathScreen;
import resources.presentation.ui.UserInterface;
import resources.testing.Probe;
import resources.testing.ProbeResult;
import resources.testing.TestHarness;

/**
 * Clicking the death overlay's "Respawn" button must actually revive the player
 * and dismiss the overlay — not re-show the death screen. Reproduces the user
 * report by driving the real mouse path:
 *   kill → draw (opens overlay) → press on the Respawn button → draw → assert
 *   alive + overlay closed.
 */
public final class DeathRespawnClickProbe implements Probe {

    @Override public String name() { return "death-respawn-click"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        GamePanel panel = harness.panel();
        Playable player = panel.player();
        if (player == null) return ProbeResult.fail(name() + " no player");
        PlayerLifecycle lifecycle = player.lifecycle();
        if (lifecycle == null) return ProbeResult.fail(name() + " no lifecycle");
        UserInterface ui = panel.userInterface();
        if (ui == null) return ProbeResult.fail(name() + " no UI");
        DeathScreen death = ui.deathScreen();
        if (death == null) return ProbeResult.fail(name() + " no death screen");

        BufferedImage surface = new BufferedImage(
            Math.max(1, panel.width), Math.max(1, panel.height), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = surface.createGraphics();

        try {
            // Die, then render a frame so the overlay opens + lays out its buttons.
            lifecycle.damage(PlayerLifecycle.DEFAULT_MAX_HEALTH * 10);
            ui.draw(g2);
            if (!death.isOpen()) return ProbeResult.fail(name() + " overlay did not open on death");

            Point respawn = death.buttonCenter("Respawn");
            if (respawn == null) return ProbeResult.fail(name() + " no Respawn button");

            // Simulate the exact click the live game routes for a modal UI.
            MouseEvent press = new MouseEvent(
                panel, MouseEvent.MOUSE_PRESSED, 0L, 0, respawn.x, respawn.y, 1, false, MouseEvent.BUTTON1);
            ui.mousePressed(press);

            // Render the next frame: a working respawn keeps the overlay closed.
            ui.draw(g2);

            boolean alive = !lifecycle.isDead();
            boolean overlayClosed = !death.isOpen();

            boolean ok = alive && overlayClosed;
            String detail = "alive=" + alive + " overlayClosed=" + overlayClosed;
            return ok ? ProbeResult.pass(name(), detail) : ProbeResult.fail(name(), detail);
        } finally {
            if (lifecycle.isDead()) lifecycle.respawn();
            ui.draw(g2);
            g2.dispose();
        }
    }
}
