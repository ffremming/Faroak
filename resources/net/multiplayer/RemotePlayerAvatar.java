package resources.net.multiplayer;

import java.util.ArrayDeque;

import resources.app.GamePanel;
import resources.domain.player.Moveable;
import resources.net.multiplayer.message.PlayerStateMessage;

/**
 * Rendered representation of a remote player on the local client.
 */
public final class RemotePlayerAvatar extends Moveable {

    private final String playerId;
    private final ArrayDeque<StateSample> samples = new ArrayDeque<>();

    public RemotePlayerAvatar(GamePanel panel, String playerId, double worldX, double worldY) {
        super(panel, "red", (int) worldX, (int) worldY,
            (short) 48, (short) 96, (short) 36, (short) 32, (short) 6, (short) 64);
        this.playerId = playerId;
    }

    @Override
    public void update() {
        // Remote players are driven by snapshots, not by local movement logic.
    }

    public String playerId() {
        return playerId;
    }

    public void pushSnapshot(long serverTick, PlayerStateMessage state) {
        if (state == null) return;
        samples.addLast(new StateSample(serverTick, state.worldX(), state.worldY()));
        while (samples.size() > 20) samples.removeFirst();
    }

    public void advanceInterpolation(long latestServerTick, int interpolationTicks) {
        if (samples.isEmpty()) return;
        long targetTick = Math.max(0L, latestServerTick - Math.max(0, interpolationTicks));
        while (samples.size() >= 2) {
            StateSample first = samples.peekFirst();
            StateSample second = samples.size() > 1 ? sampleAt(1) : null;
            if (second == null || second.tick > targetTick) break;
            samples.removeFirst();
        }
        if (samples.size() >= 2) {
            StateSample a = samples.peekFirst();
            StateSample b = sampleAt(1);
            double alpha = (b.tick == a.tick) ? 1.0 : clamp((targetTick - a.tick) / (double) (b.tick - a.tick), 0.0, 1.0);
            setWorldX(lerp(a.x, b.x, alpha));
            setWorldY(lerp(a.y, b.y, alpha));
        } else {
            StateSample only = samples.peekFirst();
            setWorldX(only.x);
            setWorldY(only.y);
        }
        getHitBox().updateCoords();
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
        final double x;
        final double y;

        StateSample(long tick, double x, double y) {
            this.tick = tick;
            this.x = x;
            this.y = y;
        }
    }
}
