package resources.net.multiplayer.hostauth;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import resources.app.GameContext;
import resources.app.GamePanel;
import resources.domain.player.Playable;
import resources.input.click.ClickRouter;
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
    private final ClickRouter clickRouter = new ClickRouter();
    private final resources.domain.combat.CombatService combat = new resources.domain.combat.CombatService();
    private final Map<String, RemoteAvatar> avatars = new LinkedHashMap<>();

    public RemoteInputApplier(GameContext ctx) {
        this.ctx = ctx;
    }

    /**
     * Run a remote player's cursor interaction through the REAL engine, so board /
     * open-container / place / harvest behave exactly as they do offline. The engine's
     * interaction code is hardwired to {@code panel.player()}, so we temporarily swap the
     * panel's player to this guest's headless {@link Playable} actor, route the click, and
     * restore. MUST be called on the host frame thread (single-threaded with simulation).
     *
     * @return true if the click was consumed.
     */
    public synchronized boolean applyInteraction(String playerId, double worldX, double worldY) {
        RemoteAvatar a = avatars.get(playerId);
        if (a == null || !(ctx instanceof GamePanel)) return false;
        GamePanel panel = (GamePanel) ctx;
        Playable actor = a.actor(panel);
        // Position the actor at the avatar's authoritative location so reach/range checks
        // measure from where the guest actually is.
        actor.setWorldX(a.x);
        actor.setWorldY(a.y);
        actor.getHitBox().updateCoords();

        Playable previous = panel.player;
        try {
            panel.player = actor;
            return clickRouter.route(panel, new java.awt.Point((int) Math.round(worldX), (int) Math.round(worldY)));
        } finally {
            panel.player = previous;
        }
    }

    /**
     * Resolve a guest's attack command through the REAL CombatService against the live
     * world, so a hit damages other players (PvP), mobs, and damageable objects exactly as
     * offline. The acting guest's world actor is the attacker; aim points from the actor
     * toward the command's target (the click/cursor world point). MUST run on the host frame
     * thread. Returns true if the command was a recognized attack (even if it hit nothing).
     */
    public synchronized boolean applyAttack(String playerId, String commandType, double worldX, double worldY) {
        RemoteAvatar a = avatars.get(playerId);
        if (a == null || a.actor == null || !(ctx instanceof GamePanel)) return false;
        syncActorToAvatar(a);
        return resolveAttack(a.actor, a.equippedItemName(), commandType, worldX, worldY);
    }

    /**
     * Resolve an attack by the HOST's own player (the engine is authoritative for the host;
     * its attack must NOT round-trip through the guest avatar map, which has no host entry).
     * Damages guest actors / mobs / objects via the same real CombatService.
     */
    public synchronized boolean applyHostAttack(Playable hostPlayer, String commandType, double worldX, double worldY) {
        if (hostPlayer == null || !(ctx instanceof GamePanel)) return false;
        resources.domain.inventory.Stack eq = hostPlayer.getEquipped();
        String equipped = (eq == null || eq.isEmpty()) ? "" : eq.getName();
        return resolveAttack(hostPlayer, equipped, commandType, worldX, worldY);
    }

    /** Shared attack resolution against the live world for any attacker Playable. */
    private boolean resolveAttack(Playable attacker, String equippedName,
            String commandType, double worldX, double worldY) {
        resources.domain.combat.WeaponProfile weapon =
            resources.domain.combat.WeaponProfile.forItem(equippedName);
        resources.geometry.Vector aim = new resources.geometry.Vector(
            worldX - (attacker.getWorldX() + attacker.getWidth() / 2.0),
            worldY - (attacker.getWorldY() + attacker.getHeight() / 2.0));

        if (ProtocolPayloads.CommandRequest.ATTACK_RANGED_AT.equals(commandType)) {
            combat.fireProjectile(attacker, ctx, aim, weapon.rangedDamage,
                weapon.projectileSpeedPxPerTick, weapon.projectileLifeTicks, weapon.projectileSpriteName);
            return true;
        }
        boolean heavy = ProtocolPayloads.CommandRequest.ATTACK_HEAVY_AT.equals(commandType);
        int damage = heavy ? weapon.heavyDamage : weapon.lightDamage;
        int rangePx = heavy ? weapon.heavyRangePx : weapon.lightRangePx;
        double arc = heavy ? weapon.heavyArcDegrees : weapon.lightArcDegrees;
        combat.meleeAttack(attacker, ctx, aim, new resources.domain.combat.MeleeAttackSpec(
            damage, rangePx, arc, heavy ? 3 : 1,
            weapon.swingSpriteName,
            heavy ? weapon.swingDurationTicks + 2 : weapon.swingDurationTicks,
            heavy ? weapon.swingArcDegrees + 10.0 : weapon.swingArcDegrees,
            weapon.swingRadiusPx));
        return true;
    }

    /** Fire the broadside of the boat ridden by the given player (guest actor or the host's
     *  own player). Resolved server-side; the spawned BoatProjectiles are world entities and
     *  replicate to all clients via the snapshot builder. Returns true if a boat fired. */
    public synchronized boolean applyBroadside(String playerId, Playable hostPlayer) {
        resources.domain.player.Playable rider = null;
        RemoteAvatar a = avatars.get(playerId);
        if (a != null && a.actor != null) rider = a.actor;
        else if (hostPlayer != null) rider = hostPlayer;
        if (rider == null || ctx.world() == null) return false;
        for (resources.domain.entity.Entity e : ctx.world().getEntities()) {
            if (e instanceof resources.domain.object.Boat
                    && ((resources.domain.object.Boat) e).rider() == rider) {
                return ((resources.domain.object.Boat) e).fireBroadside();
            }
        }
        return false;
    }

    /** True if a command type is one of the attack commands handled by {@link #applyAttack}. */
    public static boolean isAttackCommand(String commandType) {
        return ProtocolPayloads.CommandRequest.ATTACK_AT.equals(commandType)
            || ProtocolPayloads.CommandRequest.ATTACK_LIGHT_AT.equals(commandType)
            || ProtocolPayloads.CommandRequest.ATTACK_HEAVY_AT.equals(commandType)
            || ProtocolPayloads.CommandRequest.ATTACK_RANGED_AT.equals(commandType);
    }

    /** Register a joining remote player at a spawn position, and add its real (headless)
     *  Playable actor to the world so combat hit-detection and other players can see/damage
     *  it. Idempotent. */
    public synchronized RemoteAvatar join(String playerId, double spawnX, double spawnY) {
        RemoteAvatar a = avatars.computeIfAbsent(playerId, id -> new RemoteAvatar(id, spawnX, spawnY));
        if (ctx instanceof GamePanel && a.actor == null) {
            Playable actor = a.actor((GamePanel) ctx);
            actor.setWorldX(a.x);
            actor.setWorldY(a.y);
            actor.getHitBox().updateCoords();
            // placeEntityAuthoritative bypasses the solid-collision gate (the actor is a
            // replicated participant, positioned authoritatively), matching how the client
            // inserts replicated entities.
            ctx.world().placeEntityAuthoritative(actor);
        }
        return a;
    }

    public synchronized void leave(String playerId) {
        RemoteAvatar a = avatars.remove(playerId);
        if (a != null && a.actor != null && ctx.world() != null) {
            ctx.world().removeEntity(a.actor);
        }
    }

    public synchronized RemoteAvatar avatar(String playerId) {
        return avatars.get(playerId);
    }

    /**
     * The playerId of the guest whose headless actor is {@code candidate}, or null if the
     * given Playable is not a registered guest actor. Used by the snapshot builder to label
     * the {@code rider} of a boat with the correct owning guest id instead of a placeholder.
     */
    public synchronized String playerIdForActor(Playable candidate) {
        if (candidate == null) return null;
        for (RemoteAvatar a : avatars.values()) {
            if (a.actor == candidate) return a.playerId;
        }
        return null;
    }

    /** Test-only: the equipped-item name reported for a guest (via its actor). */
    public synchronized String debugEquipped(String playerId) {
        RemoteAvatar a = avatars.get(playerId);
        return a == null ? "<no avatar>" : (a.actor == null ? "<no actor>" : a.equippedItemName());
    }

    public synchronized Collection<RemoteAvatar> avatars() {
        return new java.util.ArrayList<>(avatars.values());
    }

    /** The headless actor's inventory for a guest, or null if no actor was built yet.
     *  Used by the lobby to replicate each guest's own inventory back to them. */
    public synchronized resources.domain.inventory.Inventory actorInventory(String playerId) {
        RemoteAvatar a = avatars.get(playerId);
        if (a == null || a.actor == null) return null;
        return a.actor.getInventory();
    }

    /** The headless actor's cursor (tempInHand) for a guest, or null if no actor was built
     *  yet or the cursor is empty. Used by the lobby to replicate each guest's held item. */
    public synchronized resources.domain.inventory.Stack actorTempInHand(String playerId) {
        RemoteAvatar a = avatars.get(playerId);
        if (a == null || a.actor == null) return null;
        return a.actor.getTempInHand();
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

        // If this guest is riding a boat, steer the BOAT from their input through the real
        // engine (water-only collision + drag-rider), then adopt the rider's resulting
        // position. The client-reported position is IGNORED while riding — the boat is
        // authoritative over where the rider ends up (this is what stops boats sailing onto
        // land and keeps the rider glued to the deck).
        resources.domain.object.Boat boat = ridingBoat(a);
        if (boat != null) {
            int ix = (input.right ? 1 : 0) - (input.left ? 1 : 0);
            int iy = (input.down ? 1 : 0) - (input.up ? 1 : 0);
            boat.steer(ix, iy);
            boat.syncRider();
            resources.domain.player.Playable actor = a.actor;
            if (actor != null) {
                a.x = actor.getWorldX();
                a.y = actor.getWorldY();
            }
            a.moving = (ix != 0 || iy != 0);
            if (a.moving) a.facing = facingFor(ix, iy, a.facing);
            return;
        }

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
        // Keep the world actor co-located with the avatar so combat hit-detection (which
        // scans world entities) finds this guest where they actually are.
        syncActorToAvatar(a);
    }

    /** Move the guest's world actor to the avatar's authoritative position. */
    private void syncActorToAvatar(RemoteAvatar a) {
        if (a == null || a.actor == null) return;
        a.actor.setWorldX(a.x);
        a.actor.setWorldY(a.y);
        a.actor.getHitBox().updateCoords();
    }

    /** The boat this guest's actor is currently riding, or null. Scans the world for a
     *  Boat whose rider() is this avatar's headless actor. */
    private resources.domain.object.Boat ridingBoat(RemoteAvatar a) {
        if (a == null || a.actor == null || ctx.world() == null) return null;
        for (resources.domain.entity.Entity e : ctx.world().getEntities()) {
            if (e instanceof resources.domain.object.Boat
                    && ((resources.domain.object.Boat) e).rider() == a.actor) {
                return (resources.domain.object.Boat) e;
            }
        }
        return null;
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
        // Real engine actor for running interactions (board/open/harvest/place) on this
        // guest's behalf. Built lazily, headless (local=false) so it never touches the
        // host UI. NOT added to the world entity list — it is a stand-in actor only.
        private Playable actor;

        RemoteAvatar(String playerId, double x, double y) {
            this.playerId = playerId;
            this.x = x;
            this.y = y;
        }

        Playable actor(GamePanel panel) {
            if (actor == null) {
                actor = new Playable(panel, "red", (int) x, (int) y,
                    (short) 48, (short) 96, (short) 36, (short) 32, (short) 6, (short) 64, false);
            }
            return actor;
        }

        public ProtocolPayloads.PlayerState toPayload() {
            // Read live health/alive from the real actor so combat damage + death replicate.
            int hp = health, maxHp = maxHealth;
            boolean live = alive;
            if (actor != null) {
                resources.domain.entity.component.HealthComponent h =
                    actor.getComponent(resources.domain.entity.component.HealthComponent.class);
                if (h != null) { hp = h.current(); maxHp = h.max(); }
                if (actor.lifecycle() != null) live = !actor.lifecycle().isDead();
            }
            return new ProtocolPayloads.PlayerState(
                playerId, x, y, vx, vy, lastSequence, hp, maxHp,
                facing, moving, "red", playerId, live, equippedItemName());
        }

        /** Name of the item this guest currently has selected in their hotbar, or "" when
         *  nothing is equipped / no actor has been built yet. The selection lives in the
         *  headless actor's inventory (synced via the guest's inventory clicks). */
        String equippedItemName() {
            if (actor == null) return "";
            resources.domain.inventory.Stack s = actor.getEquipped();
            return (s == null || s.isEmpty()) ? "" : s.getName();
        }
    }
}
