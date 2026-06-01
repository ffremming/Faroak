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

/** Verifies server-owned combat: health, death, and projectile impact. */
public final class AuthoritativeCombatGameplayProbe implements Probe {

    private static final int HOTBAR_OFFSET = 27;
    private static final int SLOT_SWORD = HOTBAR_OFFSET + 8;

    @Override public String name() { return "mp-authoritative-combat-gameplay"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        MultiplayerConfig cfg = new MultiplayerConfig(
            MultiplayerMode.HOST, "loopback", "p1", 10, 30, 30, 1, 120, 2.0, 4096.0, 8192.0, "test.db");
        ProtocolPayloadCodec codec = new ProtocolPayloadCodec();
        ServerTerrainRules terrain = new ServerTerrainRules();
        LobbyRuntime lobby = new AuthoritativeLobbyRuntime(
            cfg, new DefaultAuthorityService(), new InMemoryPersistenceStore(), new DefaultSnapshotCodec());
        try {
            join(lobby, codec, "p1");
            List<ProtocolPayloads.Snapshot> initial = snapshots(lobby.drainFor("p1"), codec);
            double[] spawn = player(initial, "p1");

            double[] goblinSpot = nearbyLand(terrain, spawn[0] + 120.0, spawn[1] + 48.0, 384.0);
            if (goblinSpot == null) return ProbeResult.skip(name() + " no nearby land for goblin");
            List<ProtocolPayloads.Snapshot> placedGoblin = place(lobby, codec, 1L, goblinSpot[0], goblinSpot[1], "goblin");
            ProtocolPayloads.EntityStatePayload goblin = entity(placedGoblin, "goblin");
            if (goblin == null || health(goblin) != 10) return fail("goblin did not spawn with health");

            Step hitOne = command(lobby, codec, 2L,
                ProtocolPayloads.CommandRequest.lightAttackAt(goblin.worldX, goblin.worldY, "sword", SLOT_SWORD));
            if (!hitOne.accepted) return fail("melee hit rejected", hitOne.reason);
            goblin = entity(hitOne.snapshots, "goblin");
            if (goblin == null || health(goblin) >= 10 || health(goblin) <= 0) {
                return fail("melee did not reduce health", goblin == null ? "missing" : "hp=" + health(goblin));
            }

            Step hitTwo = command(lobby, codec, 3L,
                ProtocolPayloads.CommandRequest.lightAttackAt(goblin.worldX, goblin.worldY, "sword", SLOT_SWORD));
            if (!hitTwo.accepted) return fail("second melee hit rejected", hitTwo.reason);
            goblin = entity(hitTwo.snapshots, "goblin");
            Step hitThree = command(lobby, codec, 4L,
                ProtocolPayloads.CommandRequest.lightAttackAt(goblin.worldX, goblin.worldY, "sword", SLOT_SWORD));
            if (!hitThree.accepted) return fail("third melee hit rejected", hitThree.reason);
            ProtocolPayloads.EntityStatePayload deadGoblin = entityIncludingRemoved(hitThree.snapshots, "goblin");
            if (deadGoblin == null || !deadGoblin.removed) return fail("dead goblin tombstone missing");

            double[] spiderSpot = nearbyLand(terrain, spawn[0] + 240.0, spawn[1] + 48.0, 512.0);
            if (spiderSpot == null) return ProbeResult.skip(name() + " no nearby land for spider");
            List<ProtocolPayloads.Snapshot> placedSpider = place(lobby, codec, 5L, spiderSpot[0], spiderSpot[1], "spider");
            ProtocolPayloads.EntityStatePayload spider = entity(placedSpider, "spider");
            if (spider == null || health(spider) != 5) return fail("spider did not spawn with health");

            Step ranged = command(lobby, codec, 6L,
                ProtocolPayloads.CommandRequest.rangedAttackAt(spider.worldX, spider.worldY, "sword", SLOT_SWORD));
            if (!ranged.accepted) return fail("ranged attack rejected", ranged.reason);
            if (entity(ranged.snapshots, "combat_bolt") == null) return fail("projectile did not spawn");

            ArrayList<ProtocolPayloads.Snapshot> projectileTicks = new ArrayList<>();
            for (int i = 0; i < 24; i++) {
                lobby.tick();
                projectileTicks.addAll(snapshots(lobby.drainFor("p1"), codec));
            }
            spider = entity(projectileTicks, "spider");
            if (spider == null || health(spider) >= 5) {
                return fail("projectile did not damage spider", spider == null ? "missing" : "hp=" + health(spider));
            }

            double attackerX = spawn[0] + 40.0;
            double attackerY = spawn[1] + 48.0;
            if (terrain.isWaterAt(attackerX, attackerY)) {
                double[] attackSpot = nearbyLand(terrain, attackerX, attackerY, 160.0);
                if (attackSpot == null) return ProbeResult.skip(name() + " no nearby land for attacker");
                attackerX = attackSpot[0];
                attackerY = attackSpot[1];
            }
            place(lobby, codec, 7L, attackerX, attackerY, "goblin");
            ArrayList<ProtocolPayloads.Snapshot> hurtTicks = new ArrayList<>();
            for (int i = 0; i < 36; i++) {
                lobby.tick();
                hurtTicks.addAll(snapshots(lobby.drainFor("p1"), codec));
            }
            int playerHealth = playerHealth(hurtTicks, "p1");
            if (playerHealth >= 20 || playerHealth < 0) {
                return fail("mob did not damage player health", "health=" + playerHealth);
            }

            return ProbeResult.pass(name(), "melee health/death and projectile impact");
        } finally {
            lobby.close();
        }
    }

    private static void join(LobbyRuntime lobby, ProtocolPayloadCodec codec, String playerId) {
        lobby.receive(new ProtocolEnvelope(1, playerId, 0L, 0L, 0L, ProtocolMessageType.JOIN,
            codec.encodeJoinRequest(new ProtocolPayloads.JoinRequest(false, 0.0, 0.0))));
        lobby.tick();
    }

    private static List<ProtocolPayloads.Snapshot> place(
            LobbyRuntime lobby,
            ProtocolPayloadCodec codec,
            long seq,
            double x,
            double y,
            String type) {
        ProtocolPayloads.ActionRequest request = new ProtocolPayloads.ActionRequest(
            MultiplayerAction.PLACE, true, x, y, type);
        lobby.receive(new ProtocolEnvelope(1, "p1", seq, 0L, 0L, ProtocolMessageType.ACTION, codec.encodeAction(request)));
        lobby.tick();
        return snapshots(lobby.drainFor("p1"), codec);
    }

    private static Step command(LobbyRuntime lobby, ProtocolPayloadCodec codec, long seq, ProtocolPayloads.CommandRequest command) {
        lobby.receive(new ProtocolEnvelope(1, "p1", seq, 0L, 0L, ProtocolMessageType.COMMAND, codec.encodeCommand(command)));
        lobby.tick();
        List<ProtocolEnvelope> envelopes = lobby.drainFor("p1");
        ProtocolPayloads.CommandResult result = null;
        for (ProtocolEnvelope envelope : envelopes) {
            if (ProtocolMessageType.COMMAND_RESULT.equals(envelope.messageType())) {
                result = codec.decodeCommandResult(envelope.payload());
            }
        }
        return new Step(result != null && result.accepted, result == null ? "missing result" : result.reason, snapshots(envelopes, codec));
    }

    private static List<ProtocolPayloads.Snapshot> snapshots(List<ProtocolEnvelope> envelopes, ProtocolPayloadCodec codec) {
        ArrayList<ProtocolPayloads.Snapshot> out = new ArrayList<>();
        if (envelopes == null) return out;
        for (ProtocolEnvelope envelope : envelopes) {
            if (ProtocolMessageType.BASELINE_SNAPSHOT.equals(envelope.messageType())
                    || ProtocolMessageType.DELTA_SNAPSHOT.equals(envelope.messageType())) {
                out.add(codec.decodeSnapshot(envelope.payload()));
            }
        }
        return out;
    }

    private static double[] player(List<ProtocolPayloads.Snapshot> snapshots, String playerId) {
        for (ProtocolPayloads.Snapshot snapshot : snapshots) {
            for (ProtocolPayloads.PlayerState player : snapshot.players) {
                if (playerId.equals(player.playerId)) return new double[] { player.worldX, player.worldY };
            }
        }
        return new double[] { 0.0, 0.0 };
    }

    private static int playerHealth(List<ProtocolPayloads.Snapshot> snapshots, String playerId) {
        int found = -1;
        for (ProtocolPayloads.Snapshot snapshot : snapshots) {
            for (ProtocolPayloads.PlayerState player : snapshot.players) {
                if (playerId.equals(player.playerId)) found = player.health;
            }
        }
        return found;
    }

    private static ProtocolPayloads.EntityStatePayload entity(List<ProtocolPayloads.Snapshot> snapshots, String type) {
        ProtocolPayloads.EntityStatePayload found = null;
        for (ProtocolPayloads.Snapshot snapshot : snapshots) {
            for (ProtocolPayloads.EntityStatePayload entity : snapshot.entities) {
                if (!entity.removed && type.equals(entity.entityType)) found = entity;
            }
        }
        return found;
    }

    private static ProtocolPayloads.EntityStatePayload entityIncludingRemoved(List<ProtocolPayloads.Snapshot> snapshots, String type) {
        ProtocolPayloads.EntityStatePayload found = null;
        for (ProtocolPayloads.Snapshot snapshot : snapshots) {
            for (ProtocolPayloads.EntityStatePayload entity : snapshot.entities) {
                if (type.equals(entity.entityType)) found = entity;
            }
        }
        return found;
    }

    private static int health(ProtocolPayloads.EntityStatePayload entity) {
        if (entity == null) return -1;
        for (ProtocolPayloads.ComponentStatePayload component : entity.components) {
            if (!"health".equals(component.key)) continue;
            String value = component.value == null ? "" : component.value;
            int slash = value.indexOf('/');
            if (slash >= 0) value = value.substring(0, slash);
            try { return Integer.parseInt(value.trim()); }
            catch (NumberFormatException ignored) { return -1; }
        }
        return -1;
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

    private static ProbeResult fail(String headline) {
        return ProbeResult.fail("mp-authoritative-combat-gameplay " + headline);
    }

    private static ProbeResult fail(String headline, String detail) {
        return ProbeResult.fail("mp-authoritative-combat-gameplay " + headline, detail);
    }

    private static final class Step {
        final boolean accepted;
        final String reason;
        final List<ProtocolPayloads.Snapshot> snapshots;

        Step(boolean accepted, String reason, List<ProtocolPayloads.Snapshot> snapshots) {
            this.accepted = accepted;
            this.reason = reason == null ? "" : reason;
            this.snapshots = snapshots;
        }
    }
}
