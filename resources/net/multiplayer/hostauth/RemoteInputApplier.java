package resources.net.multiplayer.hostauth;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import resources.app.GameContext;
import resources.net.multiplayer.protocol.ProtocolPayloads;

/**
 * Tracks remote players' authoritative state inside the host-authoritative lobby and
 * applies their inbound input to it.
 *
 * <p>A remote player is NOT a full {@code Playable} — that class has single-local-player
 * side effects (it seizes the UI/inventory in its constructor). Instead each remote is a
 * lightweight {@link RemoteAvatar} mirroring the legacy {@code PlayerReplicaState}: just
 * the authoritative pose the host needs to serialize, plus the latest input.
 *
 * <p>Movement follows the proven client-authoritative model used by the legacy server:
 * the client reports its own collision-resolved position; the host adopts it, clamping
 * the per-message delta so a hacked client can't teleport. Terrain collision is verified
 * against the real engine world ({@code ctx.world().solidCollision}) so remotes obey the
 * same rules as the host.
 */
public final class RemoteInputApplier {

    /** Max distance (px) a client-reported position may jump per input message. */
    static final double CLIENT_MOVE_CLAMP = 256.0;

    private final GameContext ctx;
    private final Map<String, RemoteAvatar> avatars = new LinkedHashMap<>();

    public RemoteInputApplier(GameContext ctx) {
        this.ctx = ctx;
    }

    /** Register a joining remote player at a spawn position. Idempotent. */
    public synchronized RemoteAvatar join(String playerId, double spawnX, double spawnY) {
        return avatars.computeIfAbsent(playerId, id -> new RemoteAvatar(id, spawnX, spawnY));
    }

    public synchronized void leave(String playerId) {
        avatars.remove(playerId);
    }

    public synchronized RemoteAvatar avatar(String playerId) {
        return avatars.get(playerId);
    }

    public synchronized Collection<RemoteAvatar> avatars() {
        return new java.util.ArrayList<>(avatars.values());
    }

    /**
     * Apply one decoded input message for a remote player. Adopts the client-reported
     * position (clamped), verifying it is not inside solid terrain; falls back to the
     * prior position if the target is solid.
     */
    public synchronized void apply(String playerId, ProtocolPayloads.InputState input, long sequence) {
        if (input == null) return;
        RemoteAvatar a = avatars.get(playerId);
        if (a == null) return;
        a.up = input.up; a.left = input.left; a.down = input.down; a.right = input.right;
        a.lastSequence = Math.max(a.lastSequence, sequence);
        if (!input.hasPosition) { a.moving = false; return; }

        double dx = input.posX - a.x;
        double dy = input.posY - a.y;
        double dist = Math.hypot(dx, dy);
        double tx = input.posX, ty = input.posY;
        if (dist > CLIENT_MOVE_CLAMP && dist > 0.0) {
            tx = a.x + dx / dist * CLIENT_MOVE_CLAMP;
            ty = a.y + dy / dist * CLIENT_MOVE_CLAMP;
        }
        if (!solidAt(tx, ty)) {
            a.vx = tx - a.x;
            a.vy = ty - a.y;
            a.moving = (a.vx * a.vx + a.vy * a.vy) > 1.0e-4;
            a.x = tx;
            a.y = ty;
            if (a.moving) a.facing = facingFor(a.vx, a.vy, a.facing);
        } else {
            a.vx = 0.0; a.vy = 0.0; a.moving = false;
        }
    }

    /** True if a player-sized hitbox at (x,y) would overlap solid terrain in the real world. */
    private boolean solidAt(double x, double y) {
        if (ctx.world() == null) return false;
        // Player footprint mirrors GenerationManager wiring: 36x32 hitbox at rel (6,64).
        resources.geometry.HitBox hb = new resources.geometry.HitBox(
            (int) Math.floor(x + 6), (int) Math.floor(y + 64), 36, 32);
        return ctx.world().solidCollision(hb);
    }

    private static int facingFor(double vx, double vy, int prev) {
        if (Math.abs(vx) < 1.0e-4 && Math.abs(vy) < 1.0e-4) return prev;
        if (Math.abs(vx) >= Math.abs(vy)) return vx >= 0 ? 1 : 3; // right / left
        return vy >= 0 ? 2 : 0;                                     // down / up
    }

    /** Lightweight authoritative state for a remote player (no Playable side effects). */
    public static final class RemoteAvatar {
        public final String playerId;
        public double x, y, vx, vy;
        public boolean up, left, down, right, moving;
        public int facing = 2;          // default down
        public int health = 20, maxHealth = 20;
        public boolean alive = true;
        public long lastSequence;

        RemoteAvatar(String playerId, double x, double y) {
            this.playerId = playerId;
            this.x = x;
            this.y = y;
        }

        ProtocolPayloads.PlayerState toPayload() {
            return new ProtocolPayloads.PlayerState(
                playerId, x, y, vx, vy, lastSequence, health, maxHealth,
                facing, moving, "red", playerId, alive);
        }
    }
}
