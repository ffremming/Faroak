package resources.net.multiplayer.message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import resources.net.multiplayer.protocol.ProtocolPayloads;

public final class ServerSnapshotMessage implements ServerMessage {

    private final long tick;
    private final boolean baseline;
    private final long acknowledgedSequence;
    private final List<PlayerStateMessage> players;
    private final List<WorldObjectStateMessage> worldObjects;
    private final List<ProtocolPayloads.EntityStatePayload> entities;
    private final List<ProtocolPayloads.InventoryStatePayload> inventories;
    private final List<ProtocolPayloads.TileMutationPayload> tileMutations;

    public ServerSnapshotMessage(long tick, List<PlayerStateMessage> players) {
        this(tick, false, 0L, players, new ArrayList<>());
    }

    public ServerSnapshotMessage(
            long tick,
            boolean baseline,
            long acknowledgedSequence,
            List<PlayerStateMessage> players) {
        this(tick, baseline, acknowledgedSequence, players, new ArrayList<>());
    }

    public ServerSnapshotMessage(
            long tick,
            boolean baseline,
            long acknowledgedSequence,
            List<PlayerStateMessage> players,
            List<WorldObjectStateMessage> worldObjects) {
        this(tick, baseline, acknowledgedSequence, players, worldObjects,
            new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }

    public ServerSnapshotMessage(
            long tick,
            boolean baseline,
            long acknowledgedSequence,
            List<PlayerStateMessage> players,
            List<WorldObjectStateMessage> worldObjects,
            List<ProtocolPayloads.EntityStatePayload> entities,
            List<ProtocolPayloads.InventoryStatePayload> inventories,
            List<ProtocolPayloads.TileMutationPayload> tileMutations) {
        this.tick = tick;
        this.baseline = baseline;
        this.acknowledgedSequence = Math.max(0L, acknowledgedSequence);
        this.players = Collections.unmodifiableList(new ArrayList<>(players));
        this.worldObjects = Collections.unmodifiableList(new ArrayList<>(worldObjects));
        List<ProtocolPayloads.EntityStatePayload> safeEntities =
            entities == null ? new ArrayList<>() : new ArrayList<>(entities);
        this.entities = Collections.unmodifiableList(safeEntities);
        List<ProtocolPayloads.InventoryStatePayload> safeInventories =
            inventories == null ? new ArrayList<>() : new ArrayList<>(inventories);
        this.inventories = Collections.unmodifiableList(safeInventories);
        List<ProtocolPayloads.TileMutationPayload> safeTiles =
            tileMutations == null ? new ArrayList<>() : new ArrayList<>(tileMutations);
        this.tileMutations = Collections.unmodifiableList(safeTiles);
    }

    public long tick() { return tick; }
    public boolean baseline() { return baseline; }
    public long acknowledgedSequence() { return acknowledgedSequence; }

    public List<PlayerStateMessage> players() {
        return players;
    }

    public List<WorldObjectStateMessage> worldObjects() {
        return worldObjects;
    }

    public List<ProtocolPayloads.EntityStatePayload> entities() {
        return entities;
    }

    public List<ProtocolPayloads.InventoryStatePayload> inventories() {
        return inventories;
    }

    public List<ProtocolPayloads.TileMutationPayload> tileMutations() {
        return tileMutations;
    }

    /** Authoritative world-clock tick (server GameClock); 0 if unknown/legacy. */
    private long worldTimeTicks;
    public long worldTimeTicks() { return worldTimeTicks; }
    public ServerSnapshotMessage withWorldTime(long ticks) {
        this.worldTimeTicks = Math.max(0L, ticks);
        return this;
    }
}
