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
import resources.net.multiplayer.server.authority.DefaultAuthorityService;
import resources.net.multiplayer.server.codec.DefaultSnapshotCodec;
import resources.net.multiplayer.server.persistence.InMemoryPersistenceStore;
import resources.testing.Probe;
import resources.testing.ProbeResult;
import resources.testing.TestHarness;

/**
 * Verifies PvP melee damage, the death state machine, and respawn: player A
 * attacks player B until B dies (alive=false, frozen at 0 HP), then B respawns
 * on request with full health.
 */
public final class MpDeathRespawnProbe implements Probe {

    @Override public String name() { return "mp-death-respawn"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        String prevPvp = System.getProperty("game.multiplayer.pvp");
        System.setProperty("game.multiplayer.pvp", "true");
        try {
            MultiplayerConfig cfg = new MultiplayerConfig(
                MultiplayerMode.HOST, "loopback", "host", 10, 30, 20, 1, 120, 20.0, 4096.0, 1.0e9, "test.db");
            ProtocolPayloadCodec codec = new ProtocolPayloadCodec();
            LobbyRuntime lobby = new AuthoritativeLobbyRuntime(
                cfg, new DefaultAuthorityService(), new InMemoryPersistenceStore(), new DefaultSnapshotCodec());

            String a = "Attacker-aaa111";
            String b = "Victim-bbb222";
            join(lobby, codec, a);
            join(lobby, codec, b);
            // Read A's position from a fresh baseline (join an observer to force a
            // full snapshot listing every player).
            String obs = "Obs-cccc33";
            lobby.receive(new ProtocolEnvelope(1, obs, 0L, 0L, 0L, ProtocolMessageType.JOIN,
                codec.encodeJoinRequest(new ProtocolPayloads.JoinRequest(false, 0.0, 0.0))));
            lobby.tick();
            double[] posA = playerPos(snapshots(lobby.drainFor(obs), codec), a);
            if (posA == null) return ProbeResult.fail(name() + " no A position");

            // A attacks toward A's own location repeatedly; B (co-located after join
            // at the same spawn) is within melee range. Drive enough swings to kill.
            long seq = 100L;
            int maxHealthB = 20;
            boolean diedDuringAttacks = false;
            boolean deadAtZeroHp = false;
            for (int i = 0; i < 60 && !diedDuringAttacks; i++) {
                lobby.receive(new ProtocolEnvelope(1, a, seq++, 0L, 0L, ProtocolMessageType.COMMAND,
                    codec.encodeCommand(ProtocolPayloads.CommandRequest.attackAt(posA[0] + 24.0, posA[1] + 48.0))));
                lobby.tick();
                ProtocolPayloads.PlayerState bRow = playerRow(snapshots(lobby.drainFor(b), codec), b);
                if (bRow != null && !bRow.alive) {
                    diedDuringAttacks = true;
                    maxHealthB = bRow.maxHealth;
                    deadAtZeroHp = bRow.health == 0;
                }
                lobby.drainFor(a);
            }
            // A dead, frozen player stops changing, so later deltas won't re-list it;
            // the death we observed at zero HP during the attack loop is authoritative.
            boolean isDead = diedDuringAttacks && deadAtZeroHp;

            // Request respawn; B should come back alive at full health.
            lobby.receive(new ProtocolEnvelope(1, b, 200L, 0L, 0L, ProtocolMessageType.COMMAND,
                codec.encodeCommand(ProtocolPayloads.CommandRequest.respawn())));
            for (int i = 0; i < 3; i++) lobby.tick();
            ProtocolPayloads.PlayerState aliveRow = playerRow(snapshots(lobby.drainFor(b), codec), b);
            boolean respawned = aliveRow != null && aliveRow.alive && aliveRow.health == maxHealthB;

            boolean ok = diedDuringAttacks && isDead && respawned;
            String details = "died=" + diedDuringAttacks + " isDead=" + isDead + " respawned=" + respawned;
            return ok ? ProbeResult.pass(name(), details) : ProbeResult.fail(name(), details);
        } catch (Exception e) {
            return ProbeResult.fail(name() + " threw", String.valueOf(e));
        } finally {
            if (prevPvp == null) System.clearProperty("game.multiplayer.pvp");
            else System.setProperty("game.multiplayer.pvp", prevPvp);
        }
    }

    private static void join(LobbyRuntime lobby, ProtocolPayloadCodec codec, String playerId) {
        lobby.receive(new ProtocolEnvelope(1, playerId, 0L, 0L, 0L, ProtocolMessageType.JOIN,
            codec.encodeJoinRequest(new ProtocolPayloads.JoinRequest(false, 0.0, 0.0))));
        lobby.tick();
        lobby.drainFor(playerId);
    }

    private static List<ProtocolPayloads.Snapshot> snapshots(List<ProtocolEnvelope> envelopes, ProtocolPayloadCodec codec) {
        ArrayList<ProtocolPayloads.Snapshot> out = new ArrayList<>();
        for (ProtocolEnvelope e : envelopes) {
            if (ProtocolMessageType.BASELINE_SNAPSHOT.equals(e.messageType())
                || ProtocolMessageType.DELTA_SNAPSHOT.equals(e.messageType())) {
                out.add(codec.decodeSnapshot(e.payload()));
            }
        }
        return out;
    }

    private static ProtocolPayloads.PlayerState playerRow(List<ProtocolPayloads.Snapshot> snaps, String playerId) {
        ProtocolPayloads.PlayerState found = null;
        for (ProtocolPayloads.Snapshot s : snaps) {
            for (ProtocolPayloads.PlayerState p : s.players) {
                if (playerId.equals(p.playerId)) found = p;
            }
        }
        return found;
    }

    private static double[] playerPos(List<ProtocolPayloads.Snapshot> snaps, String playerId) {
        ProtocolPayloads.PlayerState p = playerRow(snaps, playerId);
        return p == null ? null : new double[] { p.worldX, p.worldY };
    }
}
