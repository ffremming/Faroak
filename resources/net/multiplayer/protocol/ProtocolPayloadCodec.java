package resources.net.multiplayer.protocol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import resources.net.multiplayer.MultiplayerAction;

/**
 * Binary payload codec for protocol message bodies.
 */
public final class ProtocolPayloadCodec {

    public byte[] encodeInputState(ProtocolPayloads.InputState input) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);
            out.writeBoolean(input != null && input.up);
            out.writeBoolean(input != null && input.left);
            out.writeBoolean(input != null && input.down);
            out.writeBoolean(input != null && input.right);
            out.flush();
            return baos.toByteArray();
        } catch (IOException ignored) {
            return new byte[0];
        }
    }

    public ProtocolPayloads.InputState decodeInputState(byte[] payload) {
        try {
            DataInputStream in = stream(payload);
            return new ProtocolPayloads.InputState(in.readBoolean(), in.readBoolean(), in.readBoolean(), in.readBoolean());
        } catch (IOException ignored) {
            return new ProtocolPayloads.InputState(false, false, false, false);
        }
    }

    public byte[] encodeAction(ProtocolPayloads.ActionRequest action) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);
            int ordinal = (action == null || action.action == null) ? -1 : action.action.ordinal();
            out.writeInt(ordinal);
            out.writeBoolean(action != null && action.hasTarget);
            out.writeDouble((action == null) ? 0.0 : action.targetX);
            out.writeDouble((action == null) ? 0.0 : action.targetY);
            BinaryEnvelopeCodec.writeString(out, (action == null) ? "" : action.argument);
            out.flush();
            return baos.toByteArray();
        } catch (IOException ignored) {
            return new byte[0];
        }
    }

    public ProtocolPayloads.ActionRequest decodeAction(byte[] payload) {
        try {
            DataInputStream in = stream(payload);
            MultiplayerAction action = actionFromOrdinal(in.readInt());
            boolean hasTarget = in.readBoolean();
            double x = in.readDouble();
            double y = in.readDouble();
            String argument = "";
            try { argument = BinaryEnvelopeCodec.readString(in); }
            catch (IOException ignored) {}
            return new ProtocolPayloads.ActionRequest(action, hasTarget, x, y, argument);
        } catch (IOException ignored) {
            return new ProtocolPayloads.ActionRequest(null, false, 0.0, 0.0, "");
        }
    }

    public byte[] encodeReject(ProtocolPayloads.Reject reject) { return encodeString(reject == null ? "" : reject.reason); }
    public ProtocolPayloads.Reject decodeReject(byte[] payload) { return new ProtocolPayloads.Reject(decodeString(payload)); }

    public byte[] encodeAck(ProtocolPayloads.Ack ack) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);
            out.writeLong((ack == null) ? 0L : ack.acknowledgedSequence);
            out.flush();
            return baos.toByteArray();
        } catch (IOException ignored) {
            return new byte[0];
        }
    }

    public ProtocolPayloads.Ack decodeAck(byte[] payload) {
        try {
            return new ProtocolPayloads.Ack(stream(payload).readLong());
        } catch (IOException ignored) {
            return new ProtocolPayloads.Ack(0L);
        }
    }

    public byte[] encodePresence(ProtocolPayloads.Presence presence) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);
            BinaryEnvelopeCodec.writeString(out, (presence == null) ? "" : presence.playerId);
            out.writeBoolean(presence != null && presence.joined);
            out.flush();
            return baos.toByteArray();
        } catch (IOException ignored) {
            return new byte[0];
        }
    }

    public ProtocolPayloads.Presence decodePresence(byte[] payload) {
        try {
            DataInputStream in = stream(payload);
            return new ProtocolPayloads.Presence(BinaryEnvelopeCodec.readString(in), in.readBoolean());
        } catch (IOException ignored) {
            return new ProtocolPayloads.Presence("", false);
        }
    }

    public byte[] encodeSnapshot(ProtocolPayloads.Snapshot snapshot) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);
            out.writeBoolean(snapshot != null && snapshot.baseline);
            out.writeLong((snapshot == null) ? 0L : snapshot.acknowledgedSequence);
            int count = (snapshot == null || snapshot.players == null) ? 0 : snapshot.players.size();
            out.writeInt(Math.max(0, count));
            for (int i = 0; i < count; i++) {
                ProtocolPayloads.PlayerState state = snapshot.players.get(i);
                BinaryEnvelopeCodec.writeString(out, (state == null) ? "" : state.playerId);
                out.writeDouble((state == null) ? 0.0 : state.worldX);
                out.writeDouble((state == null) ? 0.0 : state.worldY);
                out.writeDouble((state == null) ? 0.0 : state.velocityX);
                out.writeDouble((state == null) ? 0.0 : state.velocityY);
                out.writeLong((state == null) ? 0L : state.processedSequence);
            }
            int worldCount = (snapshot == null || snapshot.worldObjects == null) ? 0 : snapshot.worldObjects.size();
            out.writeInt(Math.max(0, worldCount));
            for (int i = 0; i < worldCount; i++) {
                ProtocolPayloads.WorldObjectState state = snapshot.worldObjects.get(i);
                out.writeLong((state == null) ? 0L : state.objectId);
                BinaryEnvelopeCodec.writeString(out, (state == null) ? "" : state.objectType);
                out.writeDouble((state == null) ? 0.0 : state.worldX);
                out.writeDouble((state == null) ? 0.0 : state.worldY);
                out.writeBoolean(state != null && state.removed);
                out.writeLong((state == null) ? 0L : state.revision);
            }
            out.flush();
            return baos.toByteArray();
        } catch (IOException ignored) {
            return new byte[0];
        }
    }

    public ProtocolPayloads.Snapshot decodeSnapshot(byte[] payload) {
        try {
            DataInputStream in = stream(payload);
            boolean baseline = in.readBoolean();
            long ack = in.readLong();
            int count = Math.max(0, in.readInt());
            ArrayList<ProtocolPayloads.PlayerState> players = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                String playerId = BinaryEnvelopeCodec.readString(in);
                double x = in.readDouble();
                double y = in.readDouble();
                double vx = in.readDouble();
                double vy = in.readDouble();
                long seq = in.readLong();
                players.add(new ProtocolPayloads.PlayerState(playerId, x, y, vx, vy, seq));
            }
            ArrayList<ProtocolPayloads.WorldObjectState> worldObjects = new ArrayList<>();
            try {
                int worldCount = Math.max(0, in.readInt());
                for (int i = 0; i < worldCount; i++) {
                    long objectId = in.readLong();
                    String objectType = BinaryEnvelopeCodec.readString(in);
                    double worldX = in.readDouble();
                    double worldY = in.readDouble();
                    boolean removed = in.readBoolean();
                    long revision = in.readLong();
                    worldObjects.add(new ProtocolPayloads.WorldObjectState(
                        objectId, objectType, worldX, worldY, removed, revision));
                }
            } catch (IOException ignored) {
                // Optional trailing section for backward compatibility with
                // older payloads that only included player states.
            }
            return new ProtocolPayloads.Snapshot(baseline, ack, players, worldObjects);
        } catch (IOException ignored) {
            return new ProtocolPayloads.Snapshot(false, 0L, new ArrayList<>(), new ArrayList<>());
        }
    }

    private byte[] encodeString(String value) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);
            BinaryEnvelopeCodec.writeString(out, value);
            out.flush();
            return baos.toByteArray();
        } catch (IOException ignored) {
            return new byte[0];
        }
    }

    private String decodeString(byte[] payload) {
        try {
            return BinaryEnvelopeCodec.readString(stream(payload));
        } catch (IOException ignored) {
            return "";
        }
    }

    private DataInputStream stream(byte[] payload) {
        byte[] safe = (payload == null) ? new byte[0] : payload;
        return new DataInputStream(new ByteArrayInputStream(safe));
    }

    private MultiplayerAction actionFromOrdinal(int ordinal) {
        MultiplayerAction[] actions = MultiplayerAction.values();
        if (ordinal < 0 || ordinal >= actions.length) return null;
        return actions[ordinal];
    }
}
