package resources.net.multiplayer;

import java.util.ArrayList;

import resources.net.multiplayer.message.ClientActionMessage;
import resources.net.multiplayer.message.ClientCommandMessage;
import resources.net.multiplayer.message.ClientInputMessage;
import resources.net.multiplayer.message.ClientJoinMessage;
import resources.net.multiplayer.message.ClientLeaveMessage;
import resources.net.multiplayer.message.ClientMessage;
import resources.net.multiplayer.message.ClientPingMessage;
import resources.net.multiplayer.message.PlayerStateMessage;
import resources.net.multiplayer.message.ServerAckMessage;
import resources.net.multiplayer.message.ServerCommandResultMessage;
import resources.net.multiplayer.message.ServerMessage;
import resources.net.multiplayer.message.ServerPlayerPresenceMessage;
import resources.net.multiplayer.message.ServerSnapshotMessage;
import resources.net.multiplayer.message.ServerWelcomeMessage;
import resources.net.multiplayer.message.WorldObjectStateMessage;
import resources.net.multiplayer.protocol.ProtocolEnvelope;
import resources.net.multiplayer.protocol.ProtocolMessageType;
import resources.net.multiplayer.protocol.ProtocolPayloadCodec;
import resources.net.multiplayer.protocol.ProtocolPayloads;

/**
 * Maps internal message contracts to binary protocol envelopes.
 */
final class ProtocolMessageTranslator {

    private final int protocolVersion;
    private final ProtocolPayloadCodec payloadCodec = new ProtocolPayloadCodec();

    ProtocolMessageTranslator(int protocolVersion) {
        this.protocolVersion = Math.max(1, protocolVersion);
    }

    ProtocolEnvelope toEnvelope(ClientMessage message) {
        if (message == null) return null;
        if (message instanceof ClientJoinMessage) {
            ClientJoinMessage join = (ClientJoinMessage) message;
            byte[] payload = payloadCodec.encodeJoinRequest(
                new ProtocolPayloads.JoinRequest(join.hasSpawn(), join.spawnX(), join.spawnY()));
            return envelope(join.playerId(), join.sequence(), 0L, 0L, ProtocolMessageType.JOIN, payload);
        }
        if (message instanceof ClientLeaveMessage) return envelope(message.playerId(), message.sequence(), 0L, 0L, ProtocolMessageType.LEAVE, new byte[0]);
        if (message instanceof ClientInputMessage) {
            ClientInputMessage input = (ClientInputMessage) message;
            byte[] payload = payloadCodec.encodeInputState(new ProtocolPayloads.InputState(
                input.up(), input.left(), input.down(), input.right()));
            return envelope(input.playerId(), input.sequence(), 0L, 0L, ProtocolMessageType.INPUT_STATE, payload);
        }
        if (message instanceof ClientActionMessage) {
            ClientActionMessage action = (ClientActionMessage) message;
            byte[] payload = payloadCodec.encodeAction(new ProtocolPayloads.ActionRequest(
                action.action(), action.hasTarget(), action.targetX(), action.targetY(), action.argument()));
            return envelope(action.playerId(), action.sequence(), 0L, 0L, ProtocolMessageType.ACTION, payload);
        }
        if (message instanceof ClientCommandMessage) {
            ClientCommandMessage command = (ClientCommandMessage) message;
            byte[] payload = payloadCodec.encodeCommand(command.command());
            return envelope(command.playerId(), command.sequence(), 0L, 0L, ProtocolMessageType.COMMAND, payload);
        }
        if (message instanceof ClientPingMessage) {
            ClientPingMessage ping = (ClientPingMessage) message;
            byte[] payload = payloadCodec.encodeAck(new ProtocolPayloads.Ack(ping.clientTimeMillis()));
            return envelope(ping.playerId(), ping.sequence(), 0L, 0L, ProtocolMessageType.PING, payload);
        }
        return null;
    }

    ServerMessage fromEnvelope(ProtocolEnvelope envelope) {
        if (envelope == null || envelope.messageType() == null) return null;
        ProtocolMessageType type = envelope.messageType();
        if (ProtocolMessageType.WELCOME.equals(type)) {
            return new ServerWelcomeMessage(
                envelope.playerId(), true, "", envelope.ackSequence());
        }
        if (ProtocolMessageType.REJECT.equals(type)) {
            ProtocolPayloads.Reject reject = payloadCodec.decodeReject(envelope.payload());
            return new ServerWelcomeMessage(
                envelope.playerId(), false, reject.reason, envelope.ackSequence());
        }
        if (ProtocolMessageType.ACK.equals(type)) {
            ProtocolPayloads.Ack ack = payloadCodec.decodeAck(envelope.payload());
            return new ServerAckMessage(envelope.playerId(), ack.acknowledgedSequence, envelope.serverTick());
        }
        if (ProtocolMessageType.COMMAND_RESULT.equals(type)) {
            ProtocolPayloads.CommandResult result = payloadCodec.decodeCommandResult(envelope.payload());
            return new ServerCommandResultMessage(
                envelope.playerId(), result.commandSequence, result.accepted, result.reason, envelope.serverTick());
        }
        if (ProtocolMessageType.PLAYER_JOIN_LEAVE.equals(type)) {
            ProtocolPayloads.Presence p = payloadCodec.decodePresence(envelope.payload());
            return new ServerPlayerPresenceMessage(p.playerId, p.joined, envelope.serverTick());
        }
        if (ProtocolMessageType.BASELINE_SNAPSHOT.equals(type) || ProtocolMessageType.DELTA_SNAPSHOT.equals(type)) {
            ProtocolPayloads.Snapshot snapshot = payloadCodec.decodeSnapshot(envelope.payload());
            ArrayList<PlayerStateMessage> players = new ArrayList<>();
            for (ProtocolPayloads.PlayerState s : snapshot.players) {
                players.add(new PlayerStateMessage(
                    s.playerId, s.worldX, s.worldY, s.velocityX, s.velocityY,
                    s.processedSequence, s.health, s.maxHealth,
                    s.facing, s.moving, s.spriteName, s.displayName, s.alive));
            }
            ArrayList<WorldObjectStateMessage> objects = new ArrayList<>();
            for (ProtocolPayloads.WorldObjectState s : snapshot.worldObjects) {
                objects.add(new WorldObjectStateMessage(
                    s.objectId, s.objectType, s.worldX, s.worldY, s.removed, s.revision));
            }
            return new ServerSnapshotMessage(
                envelope.serverTick(), snapshot.baseline, snapshot.acknowledgedSequence,
                players, objects, snapshot.entities, snapshot.inventories, snapshot.tileMutations)
                .withWorldTime(snapshot.worldTimeTicks);
        }
        return null;
    }

    private ProtocolEnvelope envelope(
            String playerId,
            long seq,
            long ackSeq,
            long serverTick,
            ProtocolMessageType type,
            byte[] payload) {
        return new ProtocolEnvelope(protocolVersion, playerId, seq, ackSeq, serverTick, type, payload);
    }
}
