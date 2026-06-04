package resources.testing.probes;

import resources.app.GamePanel;
import resources.net.multiplayer.MultiplayerConfig;
import resources.net.multiplayer.hostauth.HostAuthoritativeLobby;
import resources.net.multiplayer.hostauth.StableEntityIds;
import resources.net.multiplayer.protocol.ProtocolEnvelope;
import resources.net.multiplayer.protocol.ProtocolMessageType;
import resources.net.multiplayer.protocol.ProtocolPayloadCodec;
import resources.net.multiplayer.protocol.ProtocolPayloads;
import resources.net.multiplayer.server.codec.DefaultSnapshotCodec;
import resources.net.multiplayer.server.codec.SnapshotCodec;
import resources.testing.Probe;
import resources.testing.ProbeResult;
import resources.testing.TestHarness;

/**
 * Regression-suite coverage for the DEFAULT online lobby (host-authoritative).
 *
 * Every probe that runs through the harness via the standard loopback adapter
 * (e.g. {@code MultiplayerAuthorityProbe}) instantiates the legacy
 * {@code AuthoritativeLobbyRuntime}; the standalone hostauth probes
 * ({@code HostAuthLobbyProbe}, {@code HostAuthAcceptanceProbe}, ...) run their
 * own {@code main()} outside the harness. So the path the game actually ships
 * with — {@link MultiplayerConfig#lobby()} returning {@code "hostauth"} by
 * default — had no coverage inside {@code MultiplayerTestRunner}.
 *
 * This probe runs inside the suite, drives the host-authoritative lobby against
 * the harness's real engine, and asserts the full guest lifecycle:
 *   0. config.lobby() resolves to "hostauth" (the shipping default).
 *   1. A guest JOINs and receives WELCOME + a populated BASELINE_SNAPSHOT.
 *   2. The guest reports movement; a later snapshot lists it at the new position.
 *   3. The guest LEAVEs and disappears from subsequent snapshots' player list.
 *
 * Run standalone: java -cp out resources.testing.probes.HostAuthDefaultProbe
 */
public final class HostAuthDefaultProbe implements Probe {

    @Override
    public String name() { return "hostauth-default-lifecycle"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        // Exercise the shipping default explicitly: clear any override so
        // lobby() falls back to its hardcoded "hostauth".
        String prev = System.getProperty("game.multiplayer.lobby");
        System.clearProperty("game.multiplayer.lobby");
        try {
            return runHostAuth(harness);
        } finally {
            if (prev != null) System.setProperty("game.multiplayer.lobby", prev);
        }
    }

    private ProbeResult runHostAuth(TestHarness harness) {
        GamePanel panel = harness.panel();

        MultiplayerConfig config = MultiplayerConfig.fromSystemProperties();
        if (!"hostauth".equals(config.lobby())) {
            return ProbeResult.fail("default lobby is not hostauth",
                "config.lobby()=" + config.lobby());
        }

        SnapshotCodec codec = new DefaultSnapshotCodec();
        ProtocolPayloadCodec pc = new ProtocolPayloadCodec();
        HostAuthoritativeLobby lobby =
            new HostAuthoritativeLobby(panel, config, codec, new StableEntityIds());
        int v = config.protocolVersion();

        try {
            // A recipient's snapshot lists only the OTHER players (each client renders
            // itself), so verifying the mover's position requires a second observer
            // guest whose view of the mover we inspect.
            String mover = "guest-mover";
            String observer = "guest-observer";
            double sx = panel.player().getWorldX();
            double sy = panel.player().getWorldY();

            // (1) Both JOIN. The mover must get WELCOME + a populated baseline snapshot.
            join(lobby, pc, v, mover, sx, sy);
            join(lobby, pc, v, observer, sx, sy);
            lobby.tick();
            lobby.produceSnapshots();

            boolean sawWelcome = false, sawBaseline = false;
            int entities = -1;
            for (ProtocolEnvelope e : lobby.drainFor(mover)) {
                if (ProtocolMessageType.WELCOME.equals(e.messageType())) sawWelcome = true;
                if (isSnapshot(e)) {
                    sawBaseline = true;
                    ProtocolPayloads.Snapshot snap = codec.decode(e.payload());
                    entities = snap.entities == null ? 0 : snap.entities.size();
                }
            }
            lobby.drainFor(observer); // discard observer's baseline
            if (!sawWelcome || !sawBaseline || entities <= 0) {
                return ProbeResult.fail("hostauth join did not yield populated baseline",
                    "welcome=" + sawWelcome + " baseline=" + sawBaseline + " entities=" + entities);
            }

            // (2) Mover reports movement; host adopts it and the OBSERVER's snapshot
            //     must list the mover at the new position.
            ProtocolPayloads.InputState mv = new ProtocolPayloads.InputState(
                false, false, false, true, true, sx + 32.0, sy);
            lobby.receive(new ProtocolEnvelope(v, mover, 1L, 0L, 0L,
                ProtocolMessageType.INPUT_STATE, pc.encodeInputState(mv)));
            lobby.tick();
            lobby.applyInteractions();
            lobby.produceSnapshots();

            ProtocolPayloads.PlayerState moved = latestPlayer(lobby, codec, observer, mover);
            if (moved == null) {
                return ProbeResult.fail("mover absent from observer snapshot after move");
            }
            if (Math.abs(moved.worldX - (sx + 32.0)) >= 1.0) {
                return ProbeResult.fail("mover move not reflected in observer snapshot",
                    "x=" + moved.worldX + " want=" + (sx + 32.0));
            }

            // (3) Mover LEAVEs -> it must drop out of the observer's subsequent snapshots.
            lobby.receive(new ProtocolEnvelope(v, mover, 2L, 0L, 0L,
                ProtocolMessageType.LEAVE, new byte[0]));
            lobby.tick();
            lobby.produceSnapshots();
            if (latestPlayer(lobby, codec, observer, mover) != null) {
                return ProbeResult.fail("mover still present in observer snapshot after LEAVE");
            }

            return ProbeResult.pass("hostauth default lobby: join/move/leave verified",
                "entities=" + entities + " movedX=" + (int) moved.worldX);
        } finally {
            lobby.close();
        }
    }

    /** Most recent snapshot listing of {@code target} as seen in {@code viewer}'s queue. */
    private static ProtocolPayloads.PlayerState latestPlayer(
            HostAuthoritativeLobby lobby, SnapshotCodec codec, String viewer, String target) {
        ProtocolPayloads.PlayerState found = null;
        for (ProtocolEnvelope e : lobby.drainFor(viewer)) {
            if (!isSnapshot(e)) continue;
            ProtocolPayloads.Snapshot snap = codec.decode(e.payload());
            if (snap.players == null) continue;
            for (ProtocolPayloads.PlayerState p : snap.players) {
                if (target.equals(p.playerId)) found = p;
            }
        }
        return found;
    }

    private static void join(HostAuthoritativeLobby lobby, ProtocolPayloadCodec pc, int v,
                             String id, double x, double y) {
        byte[] join = pc.encodeJoinRequest(new ProtocolPayloads.JoinRequest(true, x, y));
        lobby.receive(new ProtocolEnvelope(v, id, 0L, 0L, 0L, ProtocolMessageType.JOIN, join));
    }

    private static boolean isSnapshot(ProtocolEnvelope e) {
        return ProtocolMessageType.BASELINE_SNAPSHOT.equals(e.messageType())
            || ProtocolMessageType.DELTA_SNAPSHOT.equals(e.messageType());
    }
}
