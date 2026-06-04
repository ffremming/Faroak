package resources.net.multiplayer;

import java.util.ArrayDeque;

import resources.app.GamePanel;
import resources.domain.entity.component.HealthBarComponent;
import resources.domain.entity.component.LabelComponent;
import resources.domain.player.Moveable;
import resources.net.multiplayer.message.PlayerStateMessage;

/**
 * Rendered representation of a remote player on the local client. Position comes
 * from interpolated snapshots; facing, walk animation, name tag, and health bar
 * are driven from the latest authoritative {@link PlayerStateMessage}.
 */
public final class RemotePlayerAvatar extends Moveable {

    private final String playerId;
    private final ArrayDeque<StateSample> samples = new ArrayDeque<>();
    private final LabelComponent nameLabel;
    private final HealthBarComponent healthBar;

    private int facing = 2;
    private boolean moving;
    private boolean alive = true;

    public RemotePlayerAvatar(GamePanel panel, String playerId, String spriteName,
                              String displayName, double worldX, double worldY) {
        super(panel, (spriteName == null || spriteName.isBlank()) ? "red" : spriteName,
            (int) worldX, (int) worldY,
            (short) 48, (short) 96, (short) 36, (short) 32, (short) 6, (short) 64);
        this.playerId = playerId;
        this.nameLabel = addComponent(new LabelComponent(
            (displayName == null || displayName.isBlank()) ? playerId : displayName));
        this.healthBar = addComponent(new HealthBarComponent(20, 20));
    }

    @Override
    public void update() {
        // Remote players are driven by snapshots, not by local movement logic.
    }

    public String playerId() {
        return playerId;
    }

    /**
     * Mark this remote player as riding the given boat (or not, if null). While riding we
     * attach a {@link resources.domain.object.BoatRideComponent} so the scene renderer skips
     * drawing the rider sprite (same mechanism as the local rider), and anchor the avatar to
     * the boat's deck so its name tag / health bar track the boat.
     */
    public void setRidingBoat(resources.domain.object.Boat boat) {
        boolean ridingNow = boat != null;
        boolean hasComp = hasComponent(resources.domain.object.BoatRideComponent.class);
        if (ridingNow && !hasComp) {
            addComponent(new resources.domain.object.BoatRideComponent(boat));
        } else if (!ridingNow && hasComp) {
            components().remove(resources.domain.object.BoatRideComponent.class);
        }
        if (ridingNow) {
            setWorldX(boat.getWorldX() + (boat.getWidth() - getWidth()) / 2.0);
            setWorldY(boat.getWorldY() + (boat.getHeight() - getHeight()) / 2.0 - 16);
            getHitBox().updateCoords();
        }
    }

    public boolean alive() {
        return alive;
    }

    public void pushSnapshot(long serverTick, PlayerStateMessage state) {
        if (state == null) return;
        long nowMs = System.currentTimeMillis();
        samples.addLast(new StateSample(serverTick, nowMs,
            state.worldX(), state.worldY(), state.velocityX(), state.velocityY()));
        while (samples.size() > 20) samples.removeFirst();

        // Appearance / status from the authoritative row.
        this.facing = state.facing();
        this.moving = state.moving();
        this.alive = state.alive();
        healthBar.set(state.health(), state.maxHealth());
        // Hide the bar (and dim the avatar visually via the death flag) when dead.
        healthBar.setVisible(alive);
        if (state.displayName() != null && !state.displayName().isBlank()) {
            nameLabel.setText(state.displayName());
        }
    }

    public void advanceInterpolation(int interpolationDelayMs, int serverTickRate) {
        if (samples.isEmpty()) return;
        long nowMs = System.currentTimeMillis();
        long targetMs = nowMs - Math.max(0, interpolationDelayMs);
        while (samples.size() >= 2) {
            StateSample first = samples.peekFirst();
            StateSample second = samples.size() > 1 ? sampleAt(1) : null;
            if (second == null || second.arrivedAtMs > targetMs) break;
            samples.removeFirst();
        }
        if (samples.size() >= 2) {
            StateSample a = samples.peekFirst();
            StateSample b = sampleAt(1);
            long span = Math.max(1L, b.arrivedAtMs - a.arrivedAtMs);
            double alpha = clamp((targetMs - a.arrivedAtMs) / (double) span, 0.0, 1.0);
            setWorldX(lerp(a.x, b.x, alpha));
            setWorldY(lerp(a.y, b.y, alpha));
        } else {
            StateSample only = samples.peekFirst();
            long aheadMs = Math.max(0L, targetMs - only.arrivedAtMs);
            double cappedAhead = Math.min(aheadMs, 1000.0 / Math.max(1, serverTickRate));
            double ticksAhead = cappedAhead * (Math.max(1, serverTickRate) / 1000.0);
            setWorldX(only.x + (only.vx * ticksAhead));
            setWorldY(only.y + (only.vy * ticksAhead));
        }
        getHitBox().updateCoords();

        // Drive facing + walk animation from the authoritative appearance.
        setDirectionIndex(facing);
        StateSample latest = samples.peekLast();
        double vx = latest == null ? 0.0 : latest.vx;
        double vy = latest == null ? 0.0 : latest.vy;
        advanceNetworkAnimation(vx, vy, moving && alive);
    }

    private StateSample sampleAt(int index) {
        int i = 0;
        for (StateSample s : samples) {
            if (i == index) return s;
            i++;
        }
        return null;
    }

    private static double lerp(double a, double b, double t) {
        return a + ((b - a) * t);
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    private static final class StateSample {
        final long tick;
        final long arrivedAtMs;
        final double x;
        final double y;
        final double vx;
        final double vy;

        StateSample(long tick, long arrivedAtMs, double x, double y, double vx, double vy) {
            this.tick = tick;
            this.arrivedAtMs = arrivedAtMs;
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
        }
    }
}
