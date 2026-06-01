package resources.testing.probes;

import resources.app.GamePanel;
import resources.domain.player.Playable;
import resources.domain.player.PlayerLifecycle;
import resources.testing.Probe;
import resources.testing.ProbeResult;
import resources.testing.TestHarness;

/**
 * A dead player must not move. Bug: held movement keys still fed velocity into
 * the player every frame ({@code InputHandlingSystem.update}) regardless of the
 * death state, so the corpse could drift / lurch.
 *
 * This probe kills the player, holds the "down" key, ticks the simulation, and
 * asserts the player neither moved nor accumulated residual velocity. It then
 * respawns so the shared harness is left in a clean, alive state for the
 * probes that run after it.
 */
public final class DeadInputFreezeProbe implements Probe {

    @Override public String name() { return "dead-input-freeze"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        GamePanel panel = harness.panel();
        Playable player = panel.player();
        if (player == null) return ProbeResult.fail(name() + " no player");
        PlayerLifecycle lifecycle = player.lifecycle();
        if (lifecycle == null) return ProbeResult.fail(name() + " no lifecycle");

        try {
            // Kill the player outright.
            lifecycle.damage(PlayerLifecycle.DEFAULT_MAX_HEALTH * 10);
            if (!lifecycle.isDead()) return ProbeResult.fail(name() + " damage did not kill");

            double startX = player.getWorldX();
            double startY = player.getWorldY();

            // Hold a movement key, as if the player kept pressing "down" after death.
            panel.input().setDown(true);
            harness.tick(30);
            panel.input().setDown(false);

            double endX = player.getWorldX();
            double endY = player.getWorldY();
            double vx = player.getVelocity().x;
            double vy = player.getVelocity().y;

            boolean moved = endX != startX || endY != startY;
            boolean hasVelocity = vx != 0.0 || vy != 0.0;

            String detail = String.format(
                "start=(%.1f,%.1f) end=(%.1f,%.1f) vel=(%.3f,%.3f)",
                startX, startY, endX, endY, vx, vy);

            if (moved)       return ProbeResult.fail(name() + " dead player moved", detail);
            if (hasVelocity) return ProbeResult.fail(name() + " dead player retained velocity", detail);
            return ProbeResult.pass(name(), detail);
        } finally {
            // Restore the shared harness: revive at spawn so later probes see a
            // live player, and drop any held input.
            panel.input().clearHeldInput();
            lifecycle.respawn();
        }
    }
}
