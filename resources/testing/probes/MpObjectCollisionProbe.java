package resources.testing.probes;

import java.util.ArrayList;
import java.util.List;

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
 * Regression test for the "teleport past collision" bug: the server must block the
 * player from walking through a solid placed object (chest/wall), the same way the
 * client does. Without server-side object collision the authoritative position
 * walks through the obstacle and reconciliation later snaps the client past it.
 */
public final class MpObjectCollisionProbe implements Probe {

    private static final int HOTBAR_OFFSET = 27;
    private static final int SLOT_CHEST = HOTBAR_OFFSET + 4;

    @Override public String name() { return "mp-object-collision"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        // Isolate object-collision behaviour from Feature-B world seeding so the
        // placement spot is guaranteed clear.
        String prevCount = System.getProperty("game.multiplayer.worldObjectCount");
        String prevMobCap = System.getProperty("game.multiplayer.mobCap");
        System.setProperty("game.multiplayer.worldObjectCount", "0");
        System.setProperty("game.multiplayer.mobCap", "0");
        MultiplayerConfig cfg = new MultiplayerConfig(
            MultiplayerMode.HOST, "loopback", "p1", 10, 30, 20, 1, 120, 20.0, 4096.0, 1.0e9, "test.db");
        ProtocolPayloadCodec codec = new ProtocolPayloadCodec();
        ServerTerrainRules terrain = new ServerTerrainRules();
        LobbyRuntime lobby = new AuthoritativeLobbyRuntime(
            cfg, new DefaultAuthorityService(), new InMemoryPersistenceStore(), new DefaultSnapshotCodec());
        try {
            String pid = "p1";
            join(lobby, codec, pid);
            ProtocolPayloads.PlayerState self = selfRow(snaps(lobby.drainFor(pid), codec), pid);
            if (self == null) return ProbeResult.fail(name() + " no spawn");
            double px = self.worldX, py = self.worldY;

            // Place a solid chest on a clear land spot a little to the right, then
            // read its ACTUAL placed position from the snapshot (placement may tile-
            // snap), and walk toward it.
            long seq = 1L;
            double[] spot = nearbyLand(terrain, px + 80.0, py, 256.0);
            if (spot == null) return ProbeResult.skip(name() + " no land near spawn");
            Step place = command(lobby, codec, pid, seq++,
                ProtocolPayloads.CommandRequest.useEquippedAt(spot[0], spot[1], "chest", SLOT_CHEST));
            if (!place.accepted) return ProbeResult.skip(name() + " chest place rejected: " + place.reason);
            double[] chest = entityPos(place.snapshots, "chest");
            if (chest == null) return ProbeResult.skip(name() + " placed chest not in snapshot");

            // Walk toward the chest for many ticks, re-aiming each tick, and track the
            // CLOSEST the player center ever gets to the chest's raw position (what the
            // server collision is measured against). With collision the player center
            // is held outside the chest's footprint circle; without it the player walks
            // straight through and the closest approach drops to ~0.
            double minCenterGap = Double.MAX_VALUE;
            double startGap = Math.hypot(chest[0] - (px + 24.0), chest[1] - (py + 48.0));
            for (int i = 0; i < 100; i++) {
                ProtocolPayloads.PlayerState cur = selfRow(snaps(lobby.drainFor(pid), codec), pid);
                double cxp = cur == null ? px : cur.worldX;
                double cyp = cur == null ? py : cur.worldY;
                double g = Math.hypot(chest[0] - (cxp + 24.0), chest[1] - (cyp + 48.0));
                minCenterGap = Math.min(minCenterGap, g);
                boolean right = chest[0] - cxp > 2.0, left = chest[0] - cxp < -2.0;
                boolean down = chest[1] - cyp > 2.0, up = chest[1] - cyp < -2.0;
                lobby.receive(new ProtocolEnvelope(1, pid, seq++, 0L, 0L, ProtocolMessageType.INPUT_STATE,
                    codec.encodeInputState(new ProtocolPayloads.InputState(up, left, down, right))));
                lobby.tick();
            }

            // Player tried to reach the chest (start gap was sizeable) but was never
            // allowed to overlap its footprint (closest approach stayed outside ~26px).
            boolean approached = startGap > 40.0;
            boolean neverPassedThrough = minCenterGap >= 26.0;

            boolean ok = approached && neverPassedThrough;
            String details = "spawn=(" + (int) px + "," + (int) py + ") chest=(" + (int) chest[0] + "," + (int) chest[1] + ")"
                + " startGap=" + (int) startGap + " minCenterGap=" + (int) minCenterGap
                + " neverPassedThrough=" + neverPassedThrough;
            return ok ? ProbeResult.pass(name(), details) : ProbeResult.fail(name(), details);
        } catch (Exception e) {
            return ProbeResult.fail(name() + " threw", String.valueOf(e));
        } finally {
            if (prevCount == null) System.clearProperty("game.multiplayer.worldObjectCount");
            else System.setProperty("game.multiplayer.worldObjectCount", prevCount);
            if (prevMobCap == null) System.clearProperty("game.multiplayer.mobCap");
            else System.setProperty("game.multiplayer.mobCap", prevMobCap);
        }
    }

    private static void join(LobbyRuntime lobby, ProtocolPayloadCodec codec, String pid) {
        lobby.receive(new ProtocolEnvelope(1, pid, 0L, 0L, 0L, ProtocolMessageType.JOIN,
            codec.encodeJoinRequest(new ProtocolPayloads.JoinRequest(false, 0.0, 0.0))));
        lobby.tick();
    }

    private static final class Step { final boolean accepted; final String reason; final List<ProtocolPayloads.Snapshot> snapshots;
        Step(boolean a, String r, List<ProtocolPayloads.Snapshot> s) { accepted = a; reason = r; snapshots = s; } }

    private static Step command(LobbyRuntime lobby, ProtocolPayloadCodec codec, String pid, long seq,
                                ProtocolPayloads.CommandRequest cmd) {
        lobby.receive(new ProtocolEnvelope(1, pid, seq, 0L, 0L, ProtocolMessageType.COMMAND, codec.encodeCommand(cmd)));
        lobby.tick();
        List<ProtocolEnvelope> env = lobby.drainFor(pid);
        boolean accepted = false; String reason = "";
        for (ProtocolEnvelope e : env) {
            if (ProtocolMessageType.COMMAND_RESULT.equals(e.messageType())) {
                ProtocolPayloads.CommandResult r = codec.decodeCommandResult(e.payload());
                accepted = r.accepted; reason = r.reason;
            }
        }
        return new Step(accepted, reason, snaps(env, codec));
    }

    private static List<ProtocolPayloads.Snapshot> snaps(List<ProtocolEnvelope> env, ProtocolPayloadCodec codec) {
        ArrayList<ProtocolPayloads.Snapshot> out = new ArrayList<>();
        for (ProtocolEnvelope e : env) {
            if (ProtocolMessageType.BASELINE_SNAPSHOT.equals(e.messageType())
                || ProtocolMessageType.DELTA_SNAPSHOT.equals(e.messageType())) {
                out.add(codec.decodeSnapshot(e.payload()));
            }
        }
        return out;
    }

    private static ProtocolPayloads.PlayerState selfRow(List<ProtocolPayloads.Snapshot> snaps, String pid) {
        ProtocolPayloads.PlayerState found = null;
        for (ProtocolPayloads.Snapshot s : snaps) {
            for (ProtocolPayloads.PlayerState p : s.players) if (pid.equals(p.playerId)) found = p;
        }
        return found;
    }

    private static double[] nearbyLand(ServerTerrainRules terrain, double nearX, double nearY, double maxRadius) {
        for (int radius = 0; radius <= (int) maxRadius; radius += 32) {
            for (int dx = -radius; dx <= radius; dx += 32) {
                for (int dy = -radius; dy <= radius; dy += 32) {
                    if (radius > 0 && Math.abs(dx) < radius && Math.abs(dy) < radius) continue;
                    double x = nearX + dx;
                    double y = nearY + dy;
                    if (!terrain.isWaterAt(x, y)) return new double[] { x, y };
                }
            }
        }
        return null;
    }

    private static double[] entityPos(List<ProtocolPayloads.Snapshot> snaps, String type) {
        for (ProtocolPayloads.Snapshot s : snaps) {
            for (ProtocolPayloads.EntityStatePayload e : s.entities) {
                if (e != null && !e.removed && type.equals(e.entityType)) {
                    return new double[] { e.worldX, e.worldY };
                }
            }
        }
        return null;
    }
}
