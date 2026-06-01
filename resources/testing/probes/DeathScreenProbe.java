package resources.testing.probes;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import resources.app.GamePanel;
import resources.domain.player.Playable;
import resources.domain.player.PlayerLifecycle;
import resources.presentation.ui.UserInterface;
import resources.testing.Probe;
import resources.testing.ProbeResult;
import resources.testing.TestHarness;

/**
 * The death overlay must appear when the local player dies and disappear once
 * they respawn — and while it's up it must read as modal so clicks route to its
 * Respawn / Main Menu buttons instead of acting on the world.
 *
 * Drives the same {@code UserInterface.draw} sync the live render loop runs
 * (off a throwaway image, headless) and inspects the public open / modal flags.
 * Restores an alive player so later probes are unaffected.
 */
public final class DeathScreenProbe implements Probe {

    @Override public String name() { return "death-screen"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        GamePanel panel = harness.panel();
        Playable player = panel.player();
        if (player == null) return ProbeResult.fail(name() + " no player");
        PlayerLifecycle lifecycle = player.lifecycle();
        if (lifecycle == null) return ProbeResult.fail(name() + " no lifecycle");
        UserInterface ui = panel.userInterface();
        if (ui == null) return ProbeResult.fail(name() + " no UI");

        // A small offscreen surface to drive the UI's per-frame sync.
        BufferedImage surface = new BufferedImage(
            Math.max(1, panel.width), Math.max(1, panel.height), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = surface.createGraphics();

        try {
            // Baseline: alive → no death screen.
            ui.draw(g2);
            boolean hiddenWhileAlive = !ui.isDeathScreenOpen();

            // Die → next render frame should open the overlay and mark it modal.
            lifecycle.damage(PlayerLifecycle.DEFAULT_MAX_HEALTH * 10);
            if (!lifecycle.isDead()) return ProbeResult.fail(name() + " damage did not kill");
            ui.draw(g2);
            boolean shownWhileDead = ui.isDeathScreenOpen();
            boolean modalWhileDead = ui.isModalUIOpen();

            // Respawn → next render frame should hide it again.
            lifecycle.respawn();
            ui.draw(g2);
            boolean hiddenAfterRespawn = !ui.isDeathScreenOpen();

            boolean ok = hiddenWhileAlive && shownWhileDead && modalWhileDead && hiddenAfterRespawn;
            String detail = "hiddenAlive=" + hiddenWhileAlive
                + " shownDead=" + shownWhileDead
                + " modalDead=" + modalWhileDead
                + " hiddenAfterRespawn=" + hiddenAfterRespawn;
            return ok ? ProbeResult.pass(name(), detail) : ProbeResult.fail(name(), detail);
        } finally {
            g2.dispose();
            // Make sure the harness leaves with a live player and a closed overlay.
            if (lifecycle.isDead()) lifecycle.respawn();
            ui.draw(g2 = surface.createGraphics());
            g2.dispose();
        }
    }
}
