package resources.testing.probes;

import java.util.ArrayList;
import java.util.List;

import resources.net.multiplayer.MultiplayerAction;
import resources.net.multiplayer.MultiplayerConfig;
import resources.net.multiplayer.MultiplayerMode;
import resources.net.multiplayer.protocol.ProtocolEnvelope;
import resources.net.multiplayer.protocol.ProtocolMessageType;
import resources.net.multiplayer.protocol.ProtocolPayloadCodec;
import resources.net.multiplayer.protocol.ProtocolPayloads;
import resources.net.multiplayer.server.AuthoritativeLobbyRuntime;
import resources.net.multiplayer.server.LobbyRuntime;
import resources.net.multiplayer.server.ServerTerrainRules;
import resources.net.multiplayer.server.authority.DefaultAuthorityService;
import resources.net.multiplayer.server.codec.DefaultSnapshotCodec;
import resources.net.multiplayer.server.persistence.InMemoryPersistenceStore;
import resources.testing.Probe;
import resources.testing.ProbeResult;
import resources.testing.TestHarness;

/**
 * Verifies authoritative terrain parity basics:
 * - movement cannot end on water
 * - boat placement is accepted on water and rejected on land
 */
public final class MultiplayerTerrainRulesProbe implements Probe {

    @Override public String name() { return "mp-terrain-rules"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        MultiplayerConfig cfg = new MultiplayerConfig(
            MultiplayerMode.HOST, "loopback", "p1", 10, 30, 20, 1, 120, 10.0, 256.0, 2048.0, "test.db");
        LobbyRuntime lobby = new AuthoritativeLobbyRuntime(
            cfg, new DefaultAuthorityService(), new InMemoryPersistenceStore(), new DefaultSnapshotCodec());
        ProtocolPayloadCodec codec = new ProtocolPayloadCodec();
        ServerTerrainRules terrain = new ServerTerrainRules();

        ShoreCase shore = findShore(terrain);
        if (shore == null) {
            lobby.close();
            return ProbeResult.skip(name() + " no shoreline candidate");
        }

        long seq = 0L;
        ProtocolPayloads.JoinRequest join = new ProtocolPayloads.JoinRequest(true, shore.spawnX, shore.spawnY);
        lobby.receive(new ProtocolEnvelope(1, "p1", seq, 0L, 0L, ProtocolMessageType.JOIN, codec.encodeJoinRequest(join)));
        lobby.tick();
        lobby.drainFor("p1"); // consume welcome + baseline

        seq++;
        ProtocolPayloads.InputState pushTowardWater = new ProtocolPayloads.InputState(
            shore.dirY < 0, shore.dirX < 0, shore.dirY > 0, shore.dirX > 0);
        lobby.receive(new ProtocolEnvelope(1, "p1", seq, 0L, 0L, ProtocolMessageType.INPUT_STATE, codec.encodeInputState(pushTowardWater)));
        for (int i = 0; i < 40; i++) lobby.tick();

        seq++;
        ProtocolPayloads.InputState stop = new ProtocolPayloads.InputState(false, false, false, false);
        lobby.receive(new ProtocolEnvelope(1, "p1", seq, 0L, 0L, ProtocolMessageType.INPUT_STATE, codec.encodeInputState(stop)));
        lobby.tick();

        boolean ackMove = false;
        boolean ackStop = false;
        double finalX = shore.spawnX;
        double finalY = shore.spawnY;
        for (ProtocolEnvelope envelope : lobby.drainFor("p1")) {
            if (ProtocolMessageType.ACK.equals(envelope.messageType())) {
                long ack = codec.decodeAck(envelope.payload()).acknowledgedSequence;
                if (ack == 1L) ackMove = true;
                if (ack == 2L) ackStop = true;
            } else if (ProtocolMessageType.BASELINE_SNAPSHOT.equals(envelope.messageType())
                    || ProtocolMessageType.DELTA_SNAPSHOT.equals(envelope.messageType())) {
                ProtocolPayloads.Snapshot snap = codec.decodeSnapshot(envelope.payload());
                for (ProtocolPayloads.PlayerState ps : snap.players) {
                    if (!"p1".equals(ps.playerId)) continue;
                    finalX = ps.worldX;
                    finalY = ps.worldY;
                }
            }
        }

        boolean finalOnLand = terrain.canPlayerOccupy(finalX, finalY);

        // Respect per-action cooldown (3 ticks) before sending place intents.
        for (int i = 0; i < 4; i++) lobby.tick();
        seq++;
        ProtocolPayloads.ActionRequest placeBoatWater = new ProtocolPayloads.ActionRequest(
            MultiplayerAction.PLACE, true, shore.waterTargetX, shore.waterTargetY, "boat");
        lobby.receive(new ProtocolEnvelope(1, "p1", seq, 0L, 0L, ProtocolMessageType.ACTION, codec.encodeAction(placeBoatWater)));
        lobby.tick();

        for (int i = 0; i < 4; i++) lobby.tick();
        seq++;
        ProtocolPayloads.ActionRequest placeBoatLand = new ProtocolPayloads.ActionRequest(
            MultiplayerAction.PLACE, true, shore.landTargetX, shore.landTargetY, "boat");
        lobby.receive(new ProtocolEnvelope(1, "p1", seq, 0L, 0L, ProtocolMessageType.ACTION, codec.encodeAction(placeBoatLand)));
        lobby.tick();

        boolean ackBoatWater = false;
        boolean ackBoatLand = false;
        boolean waterBoatSeen = false;
        boolean landBoatSeen = false;
        for (ProtocolEnvelope envelope : lobby.drainFor("p1")) {
            if (ProtocolMessageType.ACK.equals(envelope.messageType())) {
                long ack = codec.decodeAck(envelope.payload()).acknowledgedSequence;
                if (ack == 3L) ackBoatWater = true;
                if (ack == 4L) ackBoatLand = true;
            } else if (ProtocolMessageType.BASELINE_SNAPSHOT.equals(envelope.messageType())
                    || ProtocolMessageType.DELTA_SNAPSHOT.equals(envelope.messageType())) {
                ProtocolPayloads.Snapshot snap = codec.decodeSnapshot(envelope.payload());
                for (ProtocolPayloads.WorldObjectState object : snap.worldObjects) {
                    if (object == null || object.removed || !"boat".equals(object.objectType)) continue;
                    if (closeTo(object.worldX, object.worldY, shore.waterTargetX, shore.waterTargetY, 120.0)) {
                        waterBoatSeen = true;
                    }
                    if (closeTo(object.worldX, object.worldY, shore.landTargetX, shore.landTargetY, 120.0)) {
                        landBoatSeen = true;
                    }
                }
            }
        }
        lobby.close();

        String detail = "ackMove=" + ackMove
            + ", ackStop=" + ackStop
            + ", finalOnLand=" + finalOnLand
            + ", finalX=" + fmt(finalX) + ", finalY=" + fmt(finalY)
            + ", ackBoatWater=" + ackBoatWater
            + ", ackBoatLand=" + ackBoatLand
            + ", waterBoatSeen=" + waterBoatSeen
            + ", landBoatSeen=" + landBoatSeen;

        if (!ackMove || !ackStop || !finalOnLand || !ackBoatWater || ackBoatLand || !waterBoatSeen || landBoatSeen) {
            return ProbeResult.fail(name() + " authoritative terrain/boat rules mismatch", detail);
        }
        return ProbeResult.pass(name(), detail);
    }

    private static ShoreCase findShore(ServerTerrainRules terrain) {
        int tile = 64;
        int[] dirsX = { 1, -1, 0, 0 };
        int[] dirsY = { 0, 0, 1, -1 };

        for (int x = -40 * tile; x <= 40 * tile; x += tile) {
            for (int y = -40 * tile; y <= 40 * tile; y += tile) {
                if (!terrain.canPlayerOccupy(x, y)) continue;
                for (int i = 0; i < dirsX.length; i++) {
                    int dx = dirsX[i];
                    int dy = dirsY[i];
                    boolean closeLand = terrain.canPlayerOccupy(x + (dx * 40), y + (dy * 40));
                    boolean fartherWater = !terrain.canPlayerOccupy(x + (dx * 220), y + (dy * 220));
                    if (!closeLand || !fartherWater) continue;
                    List<Double> water = firstWaterPoint(terrain, x, y, dx, dy);
                    List<Double> land = firstLandPoint(terrain, x, y, -dx, -dy);
                    if (water == null || land == null) continue;
                    return new ShoreCase(x, y, dx, dy, water.get(0), water.get(1), land.get(0), land.get(1));
                }
            }
        }
        return null;
    }

    private static List<Double> firstWaterPoint(ServerTerrainRules terrain, double x, double y, int dx, int dy) {
        for (int d = 80; d <= 320; d += 16) {
            double px = x + (dx * d);
            double py = y + (dy * d);
            if (terrain.canPlaceObject("boat", px, py)) {
                ArrayList<Double> out = new ArrayList<>(2);
                out.add(px);
                out.add(py);
                return out;
            }
        }
        return null;
    }

    private static List<Double> firstLandPoint(ServerTerrainRules terrain, double x, double y, int dx, int dy) {
        for (int d = 80; d <= 320; d += 16) {
            double px = x + (dx * d);
            double py = y + (dy * d);
            if (!terrain.isWaterAt(px, py) && !terrain.canPlaceObject("boat", px, py)) {
                ArrayList<Double> out = new ArrayList<>(2);
                out.add(px);
                out.add(py);
                return out;
            }
        }
        return null;
    }

    private static boolean closeTo(double ax, double ay, double bx, double by, double radius) {
        double dx = ax - bx;
        double dy = ay - by;
        return ((dx * dx) + (dy * dy)) <= (radius * radius);
    }

    private static String fmt(double v) {
        return String.format("%.2f", v);
    }

    private static final class ShoreCase {
        final double spawnX;
        final double spawnY;
        final int dirX;
        final int dirY;
        final double waterTargetX;
        final double waterTargetY;
        final double landTargetX;
        final double landTargetY;

        ShoreCase(double spawnX, double spawnY, int dirX, int dirY,
                  double waterTargetX, double waterTargetY,
                  double landTargetX, double landTargetY) {
            this.spawnX = spawnX;
            this.spawnY = spawnY;
            this.dirX = dirX;
            this.dirY = dirY;
            this.waterTargetX = waterTargetX;
            this.waterTargetY = waterTargetY;
            this.landTargetX = landTargetX;
            this.landTargetY = landTargetY;
        }
    }
}
