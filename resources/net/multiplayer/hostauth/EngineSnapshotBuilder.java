package resources.net.multiplayer.hostauth;

import java.util.ArrayList;
import java.util.List;

import resources.app.GameContext;
import resources.domain.entity.Entity;
import resources.domain.entity.component.HealthComponent;
import resources.domain.object.Boat;
import resources.net.multiplayer.protocol.ProtocolPayloads;

/**
 * Serializes the live (host-authoritative) game engine world into the multiplayer
 * {@link ProtocolPayloads.Snapshot} the existing codec, transport and client
 * {@code ReplicatedWorldState} already understand.
 *
 * <p>This is a pure mapping: it reads {@code ctx.world().getEntities()} and the
 * world clock and produces payloads. It performs no mutation and holds no state
 * beyond the {@link StableEntityIds} registry handed in.
 *
 * <p>Coordinate convention: the client reconstructs an entity with
 * {@code worldX = centerX - width/2} (see {@code ReplicatedWorldState.positionCentered}),
 * so this builder emits CENTER coordinates (corner + half-size).
 *
 * <p>Players, inventories, tile mutations and delta filtering are added in later
 * phases; this phase establishes entity + world-time serialization.
 */
public final class EngineSnapshotBuilder {

    private final GameContext ctx;
    private final StableEntityIds ids;

    public EngineSnapshotBuilder(GameContext ctx, StableEntityIds ids) {
        this.ctx = ctx;
        this.ids = ids;
    }

    public ProtocolPayloads.Snapshot buildBaseline(long ackSeq) {
        return build(true, ackSeq, null);
    }

    public ProtocolPayloads.Snapshot buildDelta(long ackSeq) {
        return build(false, ackSeq, null);
    }

    /** Baseline snapshot including the given remote players (host serializes peers). */
    public ProtocolPayloads.Snapshot buildBaseline(long ackSeq, List<ProtocolPayloads.PlayerState> players) {
        return build(true, ackSeq, players);
    }

    /** Delta snapshot including the given remote players. */
    public ProtocolPayloads.Snapshot buildDelta(long ackSeq, List<ProtocolPayloads.PlayerState> players) {
        return build(false, ackSeq, players);
    }

    private ProtocolPayloads.Snapshot build(boolean baseline, long ackSeq,
            List<ProtocolPayloads.PlayerState> playerStates) {
        ArrayList<ProtocolPayloads.PlayerState> players =
            playerStates == null ? new ArrayList<>() : new ArrayList<>(playerStates);
        ArrayList<ProtocolPayloads.WorldObjectState> compat = new ArrayList<>();       // legacy layer, unused here
        ArrayList<ProtocolPayloads.EntityStatePayload> entities = new ArrayList<>();
        ArrayList<ProtocolPayloads.InventoryStatePayload> inventories = new ArrayList<>(); // Phase 5
        ArrayList<ProtocolPayloads.TileMutationPayload> tiles = new ArrayList<>();          // Phase 5

        if (ctx.world() != null) {
            for (Entity e : ctx.world().getEntities()) {
                if (e == null || e == ctx.player()) continue;
                entities.add(toPayload(e));
            }
        }

        long worldTime = ctx.clock() == null ? 0L : ctx.clock().ticks();
        return new ProtocolPayloads.Snapshot(baseline, ackSeq, players, compat, entities, inventories, tiles)
            .withWorldTime(worldTime);
    }

    private ProtocolPayloads.EntityStatePayload toPayload(Entity e) {
        long id = ids.idFor(e);
        double centerX = e.getWorldX() + e.getWidth() / 2.0;
        double centerY = e.getWorldY() + e.getHeight() / 2.0;

        ArrayList<ProtocolPayloads.ComponentStatePayload> comps = new ArrayList<>();
        HealthComponent health = e.getComponent(HealthComponent.class);
        if (health != null) {
            comps.add(component("health", health.current() + "/" + health.max()));
            comps.add(component("max_health", Integer.toString(health.max())));
        }
        if (e instanceof Boat) {
            Boat boat = (Boat) e;
            comps.add(component("movement", "water_only"));
            comps.add(component("rider", boat.rider() == null ? "" : "host"));
        }

        return new ProtocolPayloads.EntityStatePayload(
            id, e.getName(), "core:overworld", centerX, centerY, false, 1L, comps);
    }

    private static ProtocolPayloads.ComponentStatePayload component(String key, String value) {
        return new ProtocolPayloads.ComponentStatePayload(key, value);
    }
}
