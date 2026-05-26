package resources.domain.player;

import java.awt.Point;

import resources.app.GamePanel;
import resources.domain.entity.component.HealthComponent;
import resources.domain.entity.component.TerrainSpeedComponent;
import resources.domain.object.BoatRideComponent;

/**
 * Owns the player's spawn point and the death/respawn flow.
 *
 * Kept off Playable so the player class stays focused on movement +
 * inventory; this service handles "what happens when HP hits 0" as a single
 * concern. The lifecycle is data-only — no UI, no input — callers (damage
 * sources, the death overlay) drive the transitions explicitly.
 */
public final class PlayerLifecycle {

    public static final int DEFAULT_MAX_HEALTH = 20;

    private final GamePanel panel;
    private final Point     spawnPoint;
    private boolean         dead;

    public PlayerLifecycle(GamePanel panel, Point spawnPoint) {
        this.panel      = panel;
        this.spawnPoint = new Point(spawnPoint);
        ensureHealth(DEFAULT_MAX_HEALTH);
    }

    public Point spawnPoint() { return new Point(spawnPoint); }
    public boolean isDead()   { return dead; }

    /** Apply damage; flips the dead flag when the underlying HP hits 0. */
    public void damage(int amount) {
        HealthComponent hc = ensureHealth(DEFAULT_MAX_HEALTH);
        hc.takeDamage(amount);
        if (hc.isDead()) {
            dead = true;
            // Force-dismount on death so the corpse doesn't keep steering a
            // boat and the respawn doesn't carry water-walking permissions
            // back to land. The boat's rider field also needs to clear so
            // patrol AI can re-engage.
            forceDismount();
        }
    }

    /**
     * Sever any active boat ride without going through Boat.dismount (which
     * refuses on open water). On death the player is teleported back to spawn
     * regardless, so we can't insist on a shore tile — just detach the ride
     * components, null the boat's rider, and let the boat resume patrol.
     */
    private void forceDismount() {
        Playable p = panel.player;
        if (p == null) return;
        BoatRideComponent ride = p.getComponent(BoatRideComponent.class);
        if (ride == null) return;
        if (ride.boat() != null) ride.boat().forceDetachRider();
        p.components().remove(BoatRideComponent.class);
        p.components().remove(TerrainSpeedComponent.class);
    }

    /** Heal up to max HP. No-op if dead — call {@link #respawn()} first. */
    public void heal(int amount) {
        HealthComponent hc = ensureHealth(DEFAULT_MAX_HEALTH);
        hc.heal(amount);
    }

    /**
     * Reset HP, clear the dead flag, and teleport the player back to the
     * recorded spawn point. Safe to call even if the player is not currently
     * dead — useful as a "return to spawn" cheat.
     */
    public void respawn() {
        Playable p = panel.player;
        if (p == null) return;
        p.components().remove(HealthComponent.class);
        p.addComponent(new HealthComponent(DEFAULT_MAX_HEALTH));
        p.setWorldX(spawnPoint.x);
        p.setWorldY(spawnPoint.y);
        dead = false;
    }

    /** Read-only health snapshot. May be null in test harnesses with no player. */
    public HealthComponent health() {
        if (panel.player == null) return null;
        return panel.player.getComponent(HealthComponent.class);
    }

    private HealthComponent ensureHealth(int max) {
        if (panel.player == null) return null;
        HealthComponent hc = panel.player.getComponent(HealthComponent.class);
        if (hc == null) {
            hc = new HealthComponent(max);
            panel.player.addComponent(hc);
        }
        return hc;
    }
}
