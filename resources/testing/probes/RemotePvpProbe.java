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
 * PvP: two guests join adjacent on land; guest A swings a sword toward guest B. Asserts B's
 * health drops (visible in the snapshot delivered to a third observer), proving players can
 * damage each other online through the real CombatService.
 * Run: java -cp out resources.testing.probes.RemotePvpProbe
 */
public final class RemotePvpProbe {

    public static void main(String[] xs) throws Exception {
        GamePanel panel = new GamePanel(new JFrame(), true);
        for (int i = 0; i < 30; i++) panel.update(1.0);

        MultiplayerConfig config = MultiplayerConfig.fromSystemProperties();
        SnapshotCodec codec = new DefaultSnapshotCodec();
        ProtocolPayloadCodec pc = new ProtocolPayloadCodec();
        HostAuthoritativeLobby lobby =
            new HostAuthoritativeLobby(panel, config, codec, new StableEntityIds());
        int v = config.protocolVersion();

        // Spawn A and B adjacent on the host's land spawn; C observes.
        double lx = panel.player().getWorldX(), ly = panel.player().getWorldY();
        double bx = lx + 40, by = ly; // B one ~half-tile to the right of A
        join(lobby, pc, v, "A", lx, ly);
        join(lobby, pc, v, "B", bx, by);
        join(lobby, pc, v, "C", lx, ly);
        lobby.tick();

        // Build A's actor and give it a sword equipped (hotbar slot with sword).
        // Boarding-less: applyAttack uses the actor's equipped item; ensure a weapon.
        // The Playable starts with a "sword" in inventory; select it.
        // Drive an interaction once to lazily build A's actor, then equip sword.
        lobby.receive(new ProtocolEnvelope(v, "A", 1L, 0L, 0L, ProtocolMessageType.INPUT_STATE,
            pc.encodeInputState(new ProtocolPayloads.InputState(false, false, false, false, true, lx, ly))));
        lobby.tick();
        lobby.applyInteractions();

        // Refresh the flat entity index so freshly-placed actors are enumerable by
        // getEntities() (combat scans it). In the real game the host render frame does this.
        panel.world().update(new java.awt.Point((int) lx, (int) ly));

        // Diagnostics: is B's actor actually a world entity the attacker can see?
        int playables = 0;
        for (resources.domain.entity.Entity e : panel.world().getEntities()) {
            if (e instanceof resources.domain.player.Playable) playables++;
        }
        System.out.println("[PvP] playables-in-world=" + playables
            + " (expect >=3: A,B,C actors) hostPlayer-also-counts");
        System.out.println("[PvP] A equipped=" + lobby.debugEquipped("A")
            + " B equipped=" + lobby.debugEquipped("B"));

        int[] before = bState(lobby, codec, v); // {alive(1/0), health}
        int hpBefore = before[1];

        // A attacks toward B repeatedly (cooldown/aim) until B is damaged or killed.
        long seq = 2;
        boolean damaged = false;
        for (int i = 0; i < 30 && !damaged; i++) {
            lobby.receive(new ProtocolEnvelope(v, "A", ++seq, 0L, 0L, ProtocolMessageType.COMMAND,
                pc.encodeCommand(new ProtocolPayloads.CommandRequest(
                    ProtocolPayloads.CommandRequest.ATTACK_LIGHT_AT, true, bx + 18, by + 48,
                    0L, "sword", -1, 0L, -1, 0))));
            lobby.tick();
            lobby.applyInteractions();
            for (int k = 0; k < 3; k++) panel.update(1.0); // let lifecycle/cooldowns advance
            int[] s = bState(lobby, codec, v);
            if (s[0] == 0 || s[1] < hpBefore) damaged = true; // killed OR took damage
        }
        int[] after = bState(lobby, codec, v);
        int hpAfter = after[1];
        boolean bKilledOrHurt = after[0] == 0 || hpAfter < hpBefore;

        // Dump every player's health from C's snapshot to see who actually took damage.
        lobby.produceSnapshots();
        for (ProtocolEnvelope e : lobby.drainFor("C")) {
            if (!ProtocolMessageType.BASELINE_SNAPSHOT.equals(e.messageType())
                && !ProtocolMessageType.DELTA_SNAPSHOT.equals(e.messageType())) continue;
            for (ProtocolPayloads.PlayerState p : codec.decode(e.payload()).players) {
                System.out.println("[PvP] player " + p.playerId + " hp=" + p.health + " alive=" + p.alive);
            }
        }
        System.out.println("[PvP] B before hp=" + hpBefore + " alive=" + (before[0] == 1)
            + " | after hp=" + hpAfter + " alive=" + (after[0] == 1)
            + " | killedOrHurt=" + bKilledOrHurt);
        if (!bKilledOrHurt) { System.err.println("FAIL: B took no damage from A"); panel.stopGameThread(); System.exit(1); }
        System.out.println("PASS");
        panel.stopGameThread();
        System.exit(0);
    }

    /** Returns {alive(1/0), health} for player B as seen in observer C's snapshot. */
    private static int[] bState(HostAuthoritativeLobby lobby, SnapshotCodec codec, int v) {
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

    private static void join(HostAuthoritativeLobby l, ProtocolPayloadCodec pc, int v, String id, double x, double y) {
        l.receive(new ProtocolEnvelope(v, id, 0L, 0L, 0L, ProtocolMessageType.JOIN,
            pc.encodeJoinRequest(new ProtocolPayloads.JoinRequest(true, x, y))));
    }
}
