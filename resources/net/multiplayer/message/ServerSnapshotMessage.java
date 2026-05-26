package resources.net.multiplayer.message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ServerSnapshotMessage implements ServerMessage {

    private final long tick;
    private final boolean baseline;
    private final long acknowledgedSequence;
    private final List<PlayerStateMessage> players;
    private final List<WorldObjectStateMessage> worldObjects;

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
        this.tick = tick;
        this.baseline = baseline;
        this.acknowledgedSequence = Math.max(0L, acknowledgedSequence);
        this.players = Collections.unmodifiableList(new ArrayList<>(players));
        this.worldObjects = Collections.unmodifiableList(new ArrayList<>(worldObjects));
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
}
