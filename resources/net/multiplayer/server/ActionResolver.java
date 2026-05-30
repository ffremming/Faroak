package resources.net.multiplayer.server;

import java.util.Map;

import resources.net.multiplayer.MultiplayerAction;
import resources.net.multiplayer.protocol.ProtocolPayloads;

/**
 * Resolves authoritative player actions (place / attack / interact) against the
 * world object maps. Pure logic helper: it holds no state of its own, performs
 * no synchronization, and is invoked exclusively from inside the
 * {@link AuthoritativeLobbyRuntime}'s lock. All mutable runtime state it touches
 * is handed in per call via the maps and the {@link Mutation} accumulator, so it
 * never aliases the runtime's scalar counters.
 */
final class ActionResolver {

    private static final double MIN_OBJECT_GAP = 32.0;

    /**
     * Sink for the side effects an action wants to apply. The runtime supplies an
     * instance seeded with its current counters; the resolver advances them and
     * the runtime reads the final values back. This keeps the single owning copy
     * of {@code nextObjectId}/{@code worldRevision} in the runtime.
     */
    static final class Mutation {
        long nextObjectId;
        long worldRevision;
        boolean worldDirty;
        final long tick;
        final EventSink events;

        Mutation(long nextObjectId, long worldRevision, boolean worldDirty, long tick, EventSink events) {
            this.nextObjectId = nextObjectId;
            this.worldRevision = worldRevision;
            this.worldDirty = worldDirty;
            this.tick = tick;
            this.events = events;
        }
    }

    /** Callback for appending session events; backed by the runtime's persistence. */
    interface EventSink {
        void event(String playerId, String type, String payload);
    }

    private final double maxActionRange;
    private final ServerTerrainRules terrainRules;

    ActionResolver(double maxActionRange, ServerTerrainRules terrainRules) {
        this.maxActionRange = maxActionRange;
        this.terrainRules = terrainRules;
    }

    boolean applyAction(
            Session session,
            ProtocolPayloads.ActionRequest action,
            Map<Long, SimObject> worldObjects,
            Map<Long, SimObject> tombstones,
            Mutation mutation) {
        if (action == null || action.action == null) return false;
        if (MultiplayerAction.PLACE.equals(action.action)) {
            return applyPlaceAction(session, action, worldObjects, tombstones, mutation);
        }
        if (MultiplayerAction.ATTACK.equals(action.action)) {
            return applyAttackAction(session, action, worldObjects, tombstones, mutation);
        }
        if (MultiplayerAction.INTERACT.equals(action.action)) {
            return applyInteractAction(session, action, worldObjects, mutation);
        }
        return false;
    }

    private boolean applyPlaceAction(
            Session session,
            ProtocolPayloads.ActionRequest action,
            Map<Long, SimObject> worldObjects,
            Map<Long, SimObject> tombstones,
            Mutation mutation) {
        if (!action.hasTarget) return false;
        String objectType = sanitizeObjectType(action.argument);
        if (objectType.isBlank()) return false;
        double x = Math.rint(action.targetX);
        double y = Math.rint(action.targetY);
        if (terrainRules != null && !terrainRules.canPlaceObject(objectType, x, y)) return false;
        if (placementBlocked(x, y, worldObjects)) return false;

        long objectId = mutation.nextObjectId++;
        long revision = ++mutation.worldRevision;
        SimObject object = new SimObject(objectId, objectType, x, y, false, revision, mutation.tick);
        worldObjects.put(objectId, object);
        tombstones.remove(objectId);
        mutation.worldDirty = true;
        mutation.events.event(session.playerId, "action", "place:" + objectType + ":" + (int) x + "," + (int) y);
        return true;
    }

    private boolean applyAttackAction(
            Session session,
            ProtocolPayloads.ActionRequest action,
            Map<Long, SimObject> worldObjects,
            Map<Long, SimObject> tombstones,
            Mutation mutation) {
        double targetX = action.hasTarget ? action.targetX : session.x;
        double targetY = action.hasTarget ? action.targetY : session.y;
        SimObject nearest = nearestObject(targetX, targetY, maxActionRange, worldObjects);
        if (nearest == null) {
            mutation.events.event(session.playerId, "action", "attack:none");
            return true;
        }
        worldObjects.remove(nearest.objectId);
        SimObject tombstone = new SimObject(
            nearest.objectId, nearest.objectType, nearest.worldX, nearest.worldY, true, ++mutation.worldRevision, mutation.tick);
        tombstones.put(tombstone.objectId, tombstone);
        mutation.worldDirty = true;
        mutation.events.event(session.playerId, "action", "attack:remove:" + nearest.objectId);
        return true;
    }

    private boolean applyInteractAction(
            Session session,
            ProtocolPayloads.ActionRequest action,
            Map<Long, SimObject> worldObjects,
            Mutation mutation) {
        double targetX = action.hasTarget ? action.targetX : session.x;
        double targetY = action.hasTarget ? action.targetY : session.y;
        SimObject nearest = nearestObject(targetX, targetY, maxActionRange, worldObjects);
        if (nearest == null) {
            mutation.events.event(session.playerId, "action", "interact:none");
            return true;
        }
        nearest.revision = ++mutation.worldRevision;
        nearest.lastChangedTick = mutation.tick;
        mutation.worldDirty = true;
        mutation.events.event(session.playerId, "action", "interact:" + nearest.objectId);
        return true;
    }

    private SimObject nearestObject(double x, double y, double maxRange, Map<Long, SimObject> worldObjects) {
        SimObject nearest = null;
        double bestDist2 = maxRange * maxRange;
        for (SimObject object : worldObjects.values()) {
            double dx = object.worldX - x;
            double dy = object.worldY - y;
            double dist2 = (dx * dx) + (dy * dy);
            if (dist2 > bestDist2) continue;
            bestDist2 = dist2;
            nearest = object;
        }
        return nearest;
    }

    private boolean placementBlocked(double x, double y, Map<Long, SimObject> worldObjects) {
        double minDist2 = MIN_OBJECT_GAP * MIN_OBJECT_GAP;
        for (SimObject object : worldObjects.values()) {
            double dx = object.worldX - x;
            double dy = object.worldY - y;
            if (((dx * dx) + (dy * dy)) <= minDist2) return true;
        }
        return false;
    }

    private String sanitizeObjectType(String raw) {
        if (raw == null) return "";
        String value = raw.trim().toLowerCase();
        if (value.isBlank() || "empty".equals(value)) return "";
        if (value.length() > 64) value = value.substring(0, 64);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z')
                    || (c >= '0' && c <= '9')
                    || c == '_' || c == '-';
            if (!ok) return "";
        }
        return value;
    }
}
