package resources.testing.probes;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JFrame;

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

/**
 * Regression probe for the "server-thread world-read race" finding.
 *
 * <p>Remote INPUT_STATE application calls {@code RemoteInputApplier.apply()} which reads
 * the live world via {@code solidCollision()} to validate the reported position against
 * terrain. That world read MUST happen on the host FRAME thread (single-threaded with
 * {@code world.simulate()}), never on the server thread inside {@code lobby.tick()} —
 * otherwise it races the frame thread mutating the (unsynchronized) entity/tile lists.
 *
 * <p>This probe asserts the architectural invariant that fixes the race:
 * <ul>
 *   <li><b>Deterministic part:</b> after sending INPUT_STATE and calling {@code tick()}
 *       (the server-thread step), the guest's authoritative pose must NOT have changed
 *       yet — input is only applied on the frame thread via {@code applyInteractions()}.
 *       BEFORE the fix, {@code tick()} applied input immediately, so the guest moved and
 *       this assertion fails.</li>
 *   <li><b>Stress part:</b> a server-thread loop hammering {@code tick()} (which now only
 *       buffers) runs concurrently with the main thread calling {@code panel.update()}
 *       ({@code world.simulate()}). With the fix no world read happens on the server
 *       thread, so this never touches the world concurrently. We also run it to surface
 *       any exception/corruption.</li>
 * </ul>
 *
 * Run: java -cp out resources.testing.probes.StressedRemoteInputProbe
 */
public final class StressedRemoteInputProbe {

    public static void main(String[] args) throws Exception {
        GamePanel panel = new GamePanel(new JFrame(), true);
        for (int i = 0; i < 30; i++) panel.update(1.0);

        MultiplayerConfig config = MultiplayerConfig.fromSystemProperties();
        SnapshotCodec codec = new DefaultSnapshotCodec();
        ProtocolPayloadCodec pc = new ProtocolPayloadCodec();
        HostAuthoritativeLobby lobby =
            new HostAuthoritativeLobby(panel, config, codec, new StableEntityIds());
        int v = config.protocolVersion();

        double sx = panel.player().getWorldX();
        double sy = panel.player().getWorldY();
        // A guest (observer) plus the moving guest, so produceSnapshots emits the mover
        // as a peer in the observer's snapshot.
        join(lobby, pc, v, "guest-mover", sx, sy);
        join(lobby, pc, v, "guest-watch", sx, sy);
        lobby.tick();
        lobby.produceSnapshots();
        drain(lobby);

        // ---- Deterministic invariant: tick() must NOT apply input ----
        ProtocolPayloads.InputState mv = new ProtocolPayloads.InputState(
            false, false, false, true, true, sx + 64.0, sy);
        lobby.receive(new ProtocolEnvelope(v, "guest-mover", 1L, 0L, 0L,
            ProtocolMessageType.INPUT_STATE, pc.encodeInputState(mv)));
        lobby.tick(); // server-thread step: must only BUFFER, not read the world / move

        double afterTick = moverX(lobby, codec);
        boolean unmovedAfterTick = Math.abs(afterTick - sx) < 0.5;

        // Now the frame-thread step applies the buffered input (world read happens here).
        lobby.applyInteractions();
        double afterApply = moverX(lobby, codec);
        boolean movedAfterApply = Math.abs(afterApply - (sx + 64.0)) < 1.0;

        System.out.println("[StressedRemoteInput] moverX after tick=" + (int) afterTick
            + " (want " + (int) sx + ", unmoved=" + unmovedAfterTick + ");"
            + " after applyInteractions=" + (int) afterApply
            + " (want " + (int) (sx + 64) + ", moved=" + movedAfterApply + ")");

        // ---- Stress: hammer tick() on a server thread while frame thread simulates ----
        final AtomicBoolean stop = new AtomicBoolean(false);
        final AtomicReference<Throwable> serverErr = new AtomicReference<>();
        Thread server = new Thread(() -> {
            long seq = 2L;
            try {
                while (!stop.get()) {
                    ProtocolPayloads.InputState in = new ProtocolPayloads.InputState(
                        false, false, false, true, true, sx + (seq % 50), sy);
                    lobby.receive(new ProtocolEnvelope(v, "guest-mover", seq++, 0L, 0L,
                        ProtocolMessageType.INPUT_STATE, pc.encodeInputState(in)));
                    lobby.tick();
                }
            } catch (Throwable t) {
                serverErr.set(t);
            }
        }, "stress-server");
        server.start();

        Throwable frameErr = null;
        try {
            for (int i = 0; i < 400; i++) {
                panel.update(1.0);         // world.simulate() on the "frame" thread
                lobby.applyInteractions(); // drains buffered inputs on the frame thread
            }
        } catch (Throwable t) {
            frameErr = t;
        }
        stop.set(true);
        server.join(2000);

        boolean noServerErr = serverErr.get() == null;
        boolean noFrameErr = frameErr == null;
        if (!noServerErr) serverErr.get().printStackTrace();
        if (!noFrameErr) frameErr.printStackTrace();
        System.out.println("[StressedRemoteInput] stress: serverThreadClean=" + noServerErr
            + " frameThreadClean=" + noFrameErr);

        boolean ok = unmovedAfterTick && movedAfterApply && noServerErr && noFrameErr;
        if (!ok) { System.err.println("FAIL"); panel.stopGameThread(); System.exit(1); }
        System.out.println("PASS");
        panel.stopGameThread();
        System.exit(0);
    }

    /** Mover's authoritative X as reported in the watcher's snapshot peer list. */
    private static double moverX(HostAuthoritativeLobby lobby, SnapshotCodec codec) {
        lobby.produceSnapshots();
        double x = Double.NaN;
        for (ProtocolEnvelope e : lobby.drainFor("guest-watch")) {
            if (!isSnapshot(e)) continue;
            ProtocolPayloads.Snapshot snap = codec.decode(e.payload());
            for (ProtocolPayloads.PlayerState p : snap.players) {
                if ("guest-mover".equals(p.playerId)) x = p.worldX;
            }
        }
        return x;
    }

    private static void drain(HostAuthoritativeLobby lobby) {
        lobby.drainFor("guest-watch");
        lobby.drainFor("guest-mover");
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
