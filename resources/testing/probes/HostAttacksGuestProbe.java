package resources.testing.probes;

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
 * The HOST attacks a nearby GUEST. Previously the host's attack was silently dropped
 * (no host avatar in RemoteInputApplier). Asserts the guest takes damage / dies, proving
 * the host can PvP through its own local CombatService.
 * Run: java -cp out resources.testing.probes.HostAttacksGuestProbe
 */
public final class HostAttacksGuestProbe {

    public static void main(String[] xs) throws Exception {
        GamePanel panel = new GamePanel(new JFrame(), true);
        for (int i = 0; i < 30; i++) panel.update(1.0);

        MultiplayerConfig config = MultiplayerConfig.fromSystemProperties();
        SnapshotCodec codec = new DefaultSnapshotCodec();
        ProtocolPayloadCodec pc = new ProtocolPayloadCodec();
        HostAuthoritativeLobby lobby =
            new HostAuthoritativeLobby(panel, config, codec, new StableEntityIds());
        int v = config.protocolVersion();
        String hostId = config.playerId();

        // The host's own player is the attacker; spawn a guest right next to it.
        double hx = panel.player().getWorldX(), hy = panel.player().getWorldY();
        double gx = hx + 40, gy = hy;
        // Host "joins" the lobby (loopback) + an observer C + the victim guest B.
        lobby.receive(new ProtocolEnvelope(v, hostId, 0L, 0L, 0L, ProtocolMessageType.JOIN,
            pc.encodeJoinRequest(new ProtocolPayloads.JoinRequest(true, hx, hy))));
        lobby.receive(new ProtocolEnvelope(v, "B", 0L, 0L, 0L, ProtocolMessageType.JOIN,
            pc.encodeJoinRequest(new ProtocolPayloads.JoinRequest(true, gx, gy))));
        lobby.receive(new ProtocolEnvelope(v, "C", 0L, 0L, 0L, ProtocolMessageType.JOIN,
            pc.encodeJoinRequest(new ProtocolPayloads.JoinRequest(true, hx, hy))));
        lobby.tick();
        // Refresh the flat entity index so the guest actor is combat-visible.
        panel.world().update(new java.awt.Point((int) hx, (int) hy));

        int hpBefore = bState(lobby, codec)[1];

        long seq = 1;
        boolean damaged = false;
        for (int i = 0; i < 30 && !damaged; i++) {
            // Host attacks toward B (the host's own player is the attacker).
            lobby.receive(new ProtocolEnvelope(v, hostId, ++seq, 0L, 0L, ProtocolMessageType.COMMAND,
                pc.encodeCommand(new ProtocolPayloads.CommandRequest(
                    ProtocolPayloads.CommandRequest.ATTACK_LIGHT_AT, true, gx + 18, gy + 48,
                    0L, "sword", -1, 0L, -1, 0))));
            lobby.tick();
            lobby.applyInteractions();
            for (int k = 0; k < 3; k++) panel.update(1.0);
            int[] s = bState(lobby, codec);
            if (s[0] == 0 || s[1] < hpBefore) damaged = true;
        }
        int[] after = bState(lobby, codec);
        System.out.println("[HostAtk] B before hp=" + hpBefore + " | after hp=" + after[1]
            + " alive=" + (after[0] == 1) + " damaged=" + damaged);
        if (!damaged) { System.err.println("FAIL: host could not damage guest"); panel.stopGameThread(); System.exit(1); }
        System.out.println("PASS");
        panel.stopGameThread();
        System.exit(0);
    }

    private static int[] bState(HostAuthoritativeLobby lobby, SnapshotCodec codec) {
        lobby.produceSnapshots();
        int[] s = { 1, -1 };
        for (ProtocolEnvelope e : lobby.drainFor("C")) {
            if (!ProtocolMessageType.BASELINE_SNAPSHOT.equals(e.messageType())
                && !ProtocolMessageType.DELTA_SNAPSHOT.equals(e.messageType())) continue;
            for (ProtocolPayloads.PlayerState p : codec.decode(e.payload()).players) {
                if ("B".equals(p.playerId)) { s[0] = p.alive ? 1 : 0; s[1] = p.health; }
            }
        }
        return s;
    }
}
