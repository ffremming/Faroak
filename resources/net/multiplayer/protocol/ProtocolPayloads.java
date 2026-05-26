package resources.net.multiplayer.protocol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import resources.net.multiplayer.MultiplayerAction;

/**
 * Payload value objects for protocol v1.
 */
public final class ProtocolPayloads {

    private ProtocolPayloads() {}

    public static final class InputState {
        public final boolean up;
        public final boolean left;
        public final boolean down;
        public final boolean right;

        public InputState(boolean up, boolean left, boolean down, boolean right) {
            this.up = up;
            this.left = left;
            this.down = down;
            this.right = right;
        }
    }

    public static final class ActionRequest {
        public final MultiplayerAction action;
        public final boolean hasTarget;
        public final double targetX;
        public final double targetY;
        public final String argument;

        public ActionRequest(MultiplayerAction action, boolean hasTarget, double targetX, double targetY) {
            this(action, hasTarget, targetX, targetY, "");
        }

        public ActionRequest(
                MultiplayerAction action,
                boolean hasTarget,
                double targetX,
                double targetY,
                String argument) {
            this.action = action;
            this.hasTarget = hasTarget;
            this.targetX = targetX;
            this.targetY = targetY;
            this.argument = (argument == null) ? "" : argument;
        }
    }

    public static final class Ack {
        public final long acknowledgedSequence;

        public Ack(long acknowledgedSequence) {
            this.acknowledgedSequence = Math.max(0L, acknowledgedSequence);
        }
    }

    public static final class Presence {
        public final String playerId;
        public final boolean joined;

        public Presence(String playerId, boolean joined) {
            this.playerId = (playerId == null) ? "" : playerId;
            this.joined = joined;
        }
    }

    public static final class Reject {
        public final String reason;

        public Reject(String reason) {
            this.reason = (reason == null) ? "" : reason;
        }
    }

    public static final class PlayerState {
        public final String playerId;
        public final double worldX;
        public final double worldY;
        public final double velocityX;
        public final double velocityY;
        public final long processedSequence;

        public PlayerState(
                String playerId,
                double worldX,
                double worldY,
                double velocityX,
                double velocityY,
                long processedSequence) {
            this.playerId = (playerId == null) ? "" : playerId;
            this.worldX = worldX;
            this.worldY = worldY;
            this.velocityX = velocityX;
            this.velocityY = velocityY;
            this.processedSequence = Math.max(0L, processedSequence);
        }
    }

    public static final class Snapshot {
        public final boolean baseline;
        public final long acknowledgedSequence;
        public final List<PlayerState> players;
        public final List<WorldObjectState> worldObjects;

        public Snapshot(boolean baseline, long acknowledgedSequence, List<PlayerState> players) {
            this(baseline, acknowledgedSequence, players, new ArrayList<>());
        }

        public Snapshot(
                boolean baseline,
                long acknowledgedSequence,
                List<PlayerState> players,
                List<WorldObjectState> worldObjects) {
            this.baseline = baseline;
            this.acknowledgedSequence = Math.max(0L, acknowledgedSequence);
            List<PlayerState> safe = (players == null) ? new ArrayList<>() : new ArrayList<>(players);
            this.players = Collections.unmodifiableList(safe);
            List<WorldObjectState> safeObjects = (worldObjects == null) ? new ArrayList<>() : new ArrayList<>(worldObjects);
            this.worldObjects = Collections.unmodifiableList(safeObjects);
        }
    }

    public static final class WorldObjectState {
        public final long objectId;
        public final String objectType;
        public final double worldX;
        public final double worldY;
        public final boolean removed;
        public final long revision;

        public WorldObjectState(
                long objectId,
                String objectType,
                double worldX,
                double worldY,
                boolean removed,
                long revision) {
            this.objectId = Math.max(0L, objectId);
            this.objectType = (objectType == null) ? "" : objectType;
            this.worldX = worldX;
            this.worldY = worldY;
            this.removed = removed;
            this.revision = Math.max(0L, revision);
        }
    }
}
