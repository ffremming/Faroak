package resources.testing.probes;

import javax.swing.JFrame;

import resources.app.GamePanel;
import resources.domain.entity.Entity;
import resources.domain.object.Boat;
import resources.net.multiplayer.MultiplayerConfig;
import resources.net.multiplayer.hostauth.HostAuthoritativeLobby;
import resources.net.multiplayer.hostauth.StableEntityIds;
import resources.net.multiplayer.protocol.ProtocolEnvelope;
import resources.net.multiplayer.protocol.ProtocolMessageType;
import resources.net.multiplayer.protocol.ProtocolPayloadCodec;
import resources.net.multiplayer.protocol.ProtocolPayloads;
import resources.net.multiplayer.server.codec.DefaultSnapshotCodec;

/**
 * A riding guest fires the ship broadside online. Asserts boat_projectile cannonballs are
 * spawned into the world (which then replicate to clients via the snapshot builder).
 * Run: java -cp out resources.testing.probes.RemoteBroadsideProbe
 */
public final class RemoteBroadsideProbe {

    public static void main(String[] xs) throws Exception {
        GamePanel panel = new GamePanel(new JFrame(), true);
        for (int i = 0; i < 30; i++) panel.update(1.0);

        // Spawn an ARMED player sloop on water (NPC boats in the world are mostly unarmed
        // rowboats; the player rides an armed player_sloop).
        int[] w = nearestWater(panel, (int) panel.player().getWorldX(), (int) panel.player().getWorldY());
        if (w == null) fail(panel, "no water near host");
        Boat boat = new Boat(panel, resources.domain.ship.ShipKindRegistry.PLAYER_SLOOP, w[0], w[1], false);
        if (!panel.world().placeShipOnWater(boat)) fail(panel, "could not place player sloop on water");
        for (int i = 0; i < 5; i++) panel.update(1.0);
        panel.world().update(new java.awt.Point(w[0], w[1]));

        MultiplayerConfig config = MultiplayerConfig.fromSystemProperties();
        ProtocolPayloadCodec pc = new ProtocolPayloadCodec();
        HostAuthoritativeLobby lobby =
            new HostAuthoritativeLobby(panel, config, new DefaultSnapshotCodec(), new StableEntityIds());
        int v = config.protocolVersion();

        // Guest joins at the boat and boards it.
        lobby.receive(new ProtocolEnvelope(v, "A", 0L, 0L, 0L, ProtocolMessageType.JOIN,
            pc.encodeJoinRequest(new ProtocolPayloads.JoinRequest(true, boat.getWorldX(), boat.getWorldY()))));
        lobby.tick();
        boat.getHitBox().updateCoords();
        lobby.receive(new ProtocolEnvelope(v, "A", 1L, 0L, 0L, ProtocolMessageType.COMMAND,
            pc.encodeCommand(ProtocolPayloads.CommandRequest.interactAt(
                boat.getHitBox().getCenterX(), boat.getHitBox().getCenterY()))));
        lobby.tick();
        lobby.applyInteractions();
        if (boat.rider() == null) fail(panel, "guest did not board");

        // Give the boat a facing (steer once) so the broadside has a direction.
        lobby.receive(new ProtocolEnvelope(v, "A", 2L, 0L, 0L, ProtocolMessageType.INPUT_STATE,
            pc.encodeInputState(new ProtocolPayloads.InputState(false, false, false, true, false, 0, 0))));
        lobby.tick();
        lobby.applyInteractions();

        System.out.println("[Broadside] boat kind=" + (boat.kind() == null ? "?" : boat.kind().id())
            + " armed=" + (boat.kind() != null && boat.kind().loadout() != null && boat.kind().loadout().armed()));
        int boltsBefore = countBolts(panel);
        // Fire the broadside.
        lobby.receive(new ProtocolEnvelope(v, "A", 3L, 0L, 0L, ProtocolMessageType.COMMAND,
            pc.encodeCommand(ProtocolPayloads.CommandRequest.fireBroadside(boat.getWorldX(), boat.getWorldY()))));
        lobby.tick();
        lobby.applyInteractions();
        panel.world().update(new java.awt.Point((int) boat.getWorldX(), (int) boat.getWorldY()));
        int boltsAfter = countBolts(panel);

        System.out.println("[Broadside] bolts before=" + boltsBefore + " after=" + boltsAfter
            + " fired=" + (boltsAfter > boltsBefore));
        if (boltsAfter <= boltsBefore) fail(panel, "no cannonballs spawned from broadside");
        System.out.println("PASS");
        panel.stopGameThread();
        System.exit(0);
    }

    private static int[] nearestWater(GamePanel panel, int px, int py) {
        int ts = 64;
        for (int r = 1; r <= 30; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -r; dy <= r; dy++) {
                    if (Math.max(Math.abs(dx), Math.abs(dy)) != r) continue;
                    int x = px + dx * ts, y = py + dy * ts;
                    var t = panel.world().getTile(new java.awt.Point(x, y));
                    String nm = (t == null || t.getName() == null) ? "" : t.getName();
                    if (resources.world.placement.TileRules.isWater(nm)) return new int[] { x, y };
                }
            }
        }
        return null;
    }

    private static int countBolts(GamePanel panel) {
        int n = 0;
        for (Entity e : panel.world().getEntities()) {
            String nm = e == null || e.getName() == null ? "" : e.getName();
            if (nm.contains("boat_projectile") || nm.contains("combat_bolt")) n++;
        }
        return n;
    }

    private static void fail(GamePanel panel, String msg) {
        System.err.println("FAIL: " + msg);
        panel.stopGameThread();
        System.exit(1);
    }
}
