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

    public byte[] encodeJoinRequest(ProtocolPayloads.JoinRequest join) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);
            out.writeBoolean(join != null && join.hasSpawn);
            out.writeDouble((join == null) ? 0.0 : join.spawnX);
            out.writeDouble((join == null) ? 0.0 : join.spawnY);
            out.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            System.err.println("[ProtocolPayloadCodec] encodeJoinRequest failed: " + e);
            return new byte[0];
        }
    }

    public ProtocolPayloads.JoinRequest decodeJoinRequest(byte[] payload) {
        if (payload == null || payload.length < 17) {
            return new ProtocolPayloads.JoinRequest(false, 0.0, 0.0);
        }
        try {
            DataInputStream in = stream(payload);
            return new ProtocolPayloads.JoinRequest(
                in.readBoolean(),
                in.readDouble(),
                in.readDouble());
        } catch (IOException e) {
            System.err.println("[ProtocolPayloadCodec] decodeJoinRequest failed: " + e);
            return new ProtocolPayloads.JoinRequest(false, 0.0, 0.0);
        }
    }

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
        } catch (IOException e) {
            System.err.println("[ProtocolPayloadCodec] encodeInputState failed: " + e);
            return new byte[0];
        }
    }

    public ProtocolPayloads.InputState decodeInputState(byte[] payload) {
        try {
            DataInputStream in = stream(payload);
            return new ProtocolPayloads.InputState(in.readBoolean(), in.readBoolean(), in.readBoolean(), in.readBoolean());
        } catch (IOException e) {
            System.err.println("[ProtocolPayloadCodec] decodeInputState failed: " + e);
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
        } catch (IOException e) {
            System.err.println("[ProtocolPayloadCodec] encodeAction failed: " + e);
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
            // expected: trailing argument string is optional for backward-compatible payloads
            try { argument = BinaryEnvelopeCodec.readString(in); }
            catch (IOException ignored) {}
            return new ProtocolPayloads.ActionRequest(action, hasTarget, x, y, argument);
        } catch (IOException e) {
            System.err.println("[ProtocolPayloadCodec] decodeAction failed: " + e);
            return new ProtocolPayloads.ActionRequest(null, false, 0.0, 0.0, "");
        }
    }

    public byte[] encodeCommand(ProtocolPayloads.CommandRequest command) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);
            BinaryEnvelopeCodec.writeString(out, command == null ? "" : command.commandType);
            out.writeBoolean(command != null && command.hasTarget);
            out.writeDouble(command == null ? 0.0 : command.targetX);
            out.writeDouble(command == null ? 0.0 : command.targetY);
            out.writeLong(command == null ? 0L : command.targetEntityId);
            BinaryEnvelopeCodec.writeString(out, command == null ? "" : command.itemType);
            out.writeInt(command == null ? -1 : command.selectedSlot);
            out.writeLong(command == null ? 0L : command.inventoryId);
            out.writeInt(command == null ? -1 : command.slotIndex);
            out.writeInt(command == null ? 0 : command.button);
            out.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            System.err.println("[ProtocolPayloadCodec] encodeCommand failed: " + e);
            return new byte[0];
        }
    }

    public ProtocolPayloads.CommandRequest decodeCommand(byte[] payload) {
        try {
            DataInputStream in = stream(payload);
            return new ProtocolPayloads.CommandRequest(
                BinaryEnvelopeCodec.readString(in),
                in.readBoolean(),
                in.readDouble(),
                in.readDouble(),
                in.readLong(),
                BinaryEnvelopeCodec.readString(in),
                in.readInt(),
                in.readLong(),
                in.readInt(),
                in.readInt());
        } catch (IOException e) {
            System.err.println("[ProtocolPayloadCodec] decodeCommand failed: " + e);
            return new ProtocolPayloads.CommandRequest("", false, 0.0, 0.0, 0L, "", -1, 0L, -1, 0);
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
        } catch (IOException e) {
            System.err.println("[ProtocolPayloadCodec] encodeAck failed: " + e);
            return new byte[0];
        }
    }

    public ProtocolPayloads.Ack decodeAck(byte[] payload) {
        try {
            return new ProtocolPayloads.Ack(stream(payload).readLong());
        } catch (IOException e) {
            System.err.println("[ProtocolPayloadCodec] decodeAck failed: " + e);
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
        } catch (IOException e) {
            System.err.println("[ProtocolPayloadCodec] encodePresence failed: " + e);
            return new byte[0];
        }
    }

    public ProtocolPayloads.Presence decodePresence(byte[] payload) {
        try {
            DataInputStream in = stream(payload);
            return new ProtocolPayloads.Presence(BinaryEnvelopeCodec.readString(in), in.readBoolean());
        } catch (IOException e) {
            System.err.println("[ProtocolPayloadCodec] decodePresence failed: " + e);
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
                out.writeInt((state == null) ? 20 : state.health);
                out.writeInt((state == null) ? 20 : state.maxHealth);
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
            writeEntityStates(out, snapshot);
            writeInventoryStates(out, snapshot);
            writeTileMutations(out, snapshot);
            writePlayerAppearance(out, snapshot);
            out.writeLong(snapshot == null ? 0L : snapshot.worldTimeTicks);
            out.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            System.err.println("[ProtocolPayloadCodec] encodeSnapshot failed: " + e);
            return new byte[0];
        }
    }

    /**
     * Trailing, backward-compatible section carrying per-player appearance/status.
     * Written in player-list order so the decoder zips it back on by index. Older
     * decoders never reach it; when an older encoder omits it the decoder keeps the
     * default appearance already on each row.
     */
    private void writePlayerAppearance(DataOutputStream out, ProtocolPayloads.Snapshot snapshot) throws IOException {
        int count = (snapshot == null || snapshot.players == null) ? 0 : snapshot.players.size();
        out.writeInt(Math.max(0, count));
        for (int i = 0; i < count; i++) {
            ProtocolPayloads.PlayerState s = snapshot.players.get(i);
            out.writeInt(s == null ? 2 : s.facing);
            out.writeBoolean(s != null && s.moving);
            BinaryEnvelopeCodec.writeString(out, s == null ? "red" : s.spriteName);
            BinaryEnvelopeCodec.writeString(out, s == null ? "" : s.displayName);
            out.writeBoolean(s == null || s.alive);
        }
    }

    /** Zip the trailing appearance section back onto the decoded player rows by index. */
    private void readPlayerAppearance(DataInputStream in, ArrayList<ProtocolPayloads.PlayerState> players) {
        try {
            int count = Math.max(0, in.readInt());
            for (int i = 0; i < count; i++) {
                int facing = in.readInt();
                boolean moving = in.readBoolean();
                String spriteName = BinaryEnvelopeCodec.readString(in);
                String displayName = BinaryEnvelopeCodec.readString(in);
                boolean alive = in.readBoolean();
                if (i < players.size() && players.get(i) != null) {
                    players.set(i, players.get(i).withAppearance(facing, moving, spriteName, displayName, alive));
                }
            }
        } catch (IOException ignored) {
            // expected: older encoders omit the appearance section; keep defaults.
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
                int health = in.readInt();
                int maxHealth = in.readInt();
                players.add(new ProtocolPayloads.PlayerState(playerId, x, y, vx, vy, seq, health, maxHealth));
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
                // expected: optional trailing section for backward compatibility with
                // older payloads that only included player states.
            }
            ArrayList<ProtocolPayloads.EntityStatePayload> entities = readEntityStates(in);
            ArrayList<ProtocolPayloads.InventoryStatePayload> inventories = readInventoryStates(in);
            ArrayList<ProtocolPayloads.TileMutationPayload> tileMutations = readTileMutations(in);
            readPlayerAppearance(in, players);
            long worldTimeTicks = 0L;
            try {
                worldTimeTicks = in.readLong();
            } catch (IOException ignored) {
                // expected: older encoders omit the world-time section; default 0.
            }
            ProtocolPayloads.Snapshot result = new ProtocolPayloads.Snapshot(
                baseline, ack, players, worldObjects, entities, inventories, tileMutations);
            result.worldTimeTicks = Math.max(0L, worldTimeTicks);
            return result;
        } catch (IOException e) {
            System.err.println("[ProtocolPayloadCodec] decodeSnapshot failed: " + e);
            return new ProtocolPayloads.Snapshot(false, 0L, new ArrayList<>(), new ArrayList<>());
        }
    }

    public byte[] encodeCommandResult(ProtocolPayloads.CommandResult result) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);
            out.writeLong((result == null) ? 0L : result.commandSequence);
            out.writeBoolean(result != null && result.accepted);
            BinaryEnvelopeCodec.writeString(out, (result == null) ? "" : result.reason);
            out.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            System.err.println("[ProtocolPayloadCodec] encodeCommandResult failed: " + e);
            return new byte[0];
        }
    }

    public ProtocolPayloads.CommandResult decodeCommandResult(byte[] payload) {
        try {
            DataInputStream in = stream(payload);
            return new ProtocolPayloads.CommandResult(
                in.readLong(),
                in.readBoolean(),
                BinaryEnvelopeCodec.readString(in));
        } catch (IOException e) {
            System.err.println("[ProtocolPayloadCodec] decodeCommandResult failed: " + e);
            return new ProtocolPayloads.CommandResult(0L, false, "");
        }
    }

    private void writeEntityStates(DataOutputStream out, ProtocolPayloads.Snapshot snapshot) throws IOException {
        int count = (snapshot == null || snapshot.entities == null) ? 0 : snapshot.entities.size();
        out.writeInt(Math.max(0, count));
        for (int i = 0; i < count; i++) {
            ProtocolPayloads.EntityStatePayload entity = snapshot.entities.get(i);
            out.writeLong(entity == null ? 0L : entity.entityId);
            BinaryEnvelopeCodec.writeString(out, entity == null ? "" : entity.entityType);
            BinaryEnvelopeCodec.writeString(out, entity == null ? "" : entity.dimensionId);
            out.writeDouble(entity == null ? 0.0 : entity.worldX);
            out.writeDouble(entity == null ? 0.0 : entity.worldY);
            out.writeBoolean(entity != null && entity.removed);
            out.writeLong(entity == null ? 0L : entity.revision);
            int componentCount = (entity == null || entity.components == null) ? 0 : entity.components.size();
            out.writeInt(Math.max(0, componentCount));
            for (int c = 0; c < componentCount; c++) {
                ProtocolPayloads.ComponentStatePayload component = entity.components.get(c);
                BinaryEnvelopeCodec.writeString(out, component == null ? "" : component.key);
                BinaryEnvelopeCodec.writeString(out, component == null ? "" : component.value);
            }
        }
    }

    private ArrayList<ProtocolPayloads.EntityStatePayload> readEntityStates(DataInputStream in) {
        ArrayList<ProtocolPayloads.EntityStatePayload> out = new ArrayList<>();
        try {
            int count = Math.max(0, in.readInt());
            for (int i = 0; i < count; i++) {
                long entityId = in.readLong();
                String entityType = BinaryEnvelopeCodec.readString(in);
                String dimensionId = BinaryEnvelopeCodec.readString(in);
                double worldX = in.readDouble();
                double worldY = in.readDouble();
                boolean removed = in.readBoolean();
                long revision = in.readLong();
                int componentCount = Math.max(0, in.readInt());
                ArrayList<ProtocolPayloads.ComponentStatePayload> components = new ArrayList<>(componentCount);
                for (int c = 0; c < componentCount; c++) {
                    components.add(new ProtocolPayloads.ComponentStatePayload(
                        BinaryEnvelopeCodec.readString(in),
                        BinaryEnvelopeCodec.readString(in)));
                }
                out.add(new ProtocolPayloads.EntityStatePayload(
                    entityId, entityType, dimensionId, worldX, worldY, removed, revision, components));
            }
        } catch (IOException ignored) {
            // expected: v1 snapshots do not carry v2 entity sections
        }
        return out;
    }

    private void writeInventoryStates(DataOutputStream out, ProtocolPayloads.Snapshot snapshot) throws IOException {
        int count = (snapshot == null || snapshot.inventories == null) ? 0 : snapshot.inventories.size();
        out.writeInt(Math.max(0, count));
        for (int i = 0; i < count; i++) {
            ProtocolPayloads.InventoryStatePayload inv = snapshot.inventories.get(i);
            out.writeLong(inv == null ? 0L : inv.inventoryId);
            out.writeLong(inv == null ? 0L : inv.ownerEntityId);
            BinaryEnvelopeCodec.writeString(out, inv == null ? "" : inv.inventoryType);
            out.writeLong(inv == null ? 0L : inv.revision);
            int slotCount = (inv == null || inv.slots == null) ? 0 : inv.slots.size();
            out.writeInt(Math.max(0, slotCount));
            for (int s = 0; s < slotCount; s++) {
                ProtocolPayloads.ItemStackPayload stack = inv.slots.get(s);
                BinaryEnvelopeCodec.writeString(out, stack == null ? "empty" : stack.itemType);
                out.writeInt(stack == null ? 0 : stack.amount);
            }
        }
    }

    private ArrayList<ProtocolPayloads.InventoryStatePayload> readInventoryStates(DataInputStream in) {
        ArrayList<ProtocolPayloads.InventoryStatePayload> out = new ArrayList<>();
        try {
            int count = Math.max(0, in.readInt());
            for (int i = 0; i < count; i++) {
                long inventoryId = in.readLong();
                long ownerEntityId = in.readLong();
                String inventoryType = BinaryEnvelopeCodec.readString(in);
                long revision = in.readLong();
                int slotCount = Math.max(0, in.readInt());
                ArrayList<ProtocolPayloads.ItemStackPayload> slots = new ArrayList<>(slotCount);
                for (int s = 0; s < slotCount; s++) {
                    slots.add(new ProtocolPayloads.ItemStackPayload(
                        BinaryEnvelopeCodec.readString(in),
                        in.readInt()));
                }
                out.add(new ProtocolPayloads.InventoryStatePayload(
                    inventoryId, ownerEntityId, inventoryType, revision, slots));
            }
        } catch (IOException ignored) {
            // expected: v1 snapshots do not carry v2 inventory sections
        }
        return out;
    }

    private void writeTileMutations(DataOutputStream out, ProtocolPayloads.Snapshot snapshot) throws IOException {
        int count = (snapshot == null || snapshot.tileMutations == null) ? 0 : snapshot.tileMutations.size();
        out.writeInt(Math.max(0, count));
        for (int i = 0; i < count; i++) {
            ProtocolPayloads.TileMutationPayload tile = snapshot.tileMutations.get(i);
            BinaryEnvelopeCodec.writeString(out, tile == null ? "" : tile.dimensionId);
            out.writeInt(tile == null ? 0 : tile.tileX);
            out.writeInt(tile == null ? 0 : tile.tileY);
            BinaryEnvelopeCodec.writeString(out, tile == null ? "" : tile.tileType);
            out.writeBoolean(tile != null && tile.watered);
            BinaryEnvelopeCodec.writeString(out, tile == null ? "" : tile.cropType);
            out.writeInt(tile == null ? 0 : tile.cropStage);
            out.writeLong(tile == null ? 0L : tile.revision);
        }
    }

    private ArrayList<ProtocolPayloads.TileMutationPayload> readTileMutations(DataInputStream in) {
        ArrayList<ProtocolPayloads.TileMutationPayload> out = new ArrayList<>();
        try {
            int count = Math.max(0, in.readInt());
            for (int i = 0; i < count; i++) {
                out.add(new ProtocolPayloads.TileMutationPayload(
                    BinaryEnvelopeCodec.readString(in),
                    in.readInt(),
                    in.readInt(),
                    BinaryEnvelopeCodec.readString(in),
                    in.readBoolean(),
                    BinaryEnvelopeCodec.readString(in),
                    in.readInt(),
                    in.readLong()));
            }
        } catch (IOException ignored) {
            // expected: v1 snapshots do not carry v2 tile sections
        }
        return out;
    }

    private byte[] encodeString(String value) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);
            BinaryEnvelopeCodec.writeString(out, value);
            out.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            System.err.println("[ProtocolPayloadCodec] encodeString failed: " + e);
            return new byte[0];
        }
    }

    private String decodeString(byte[] payload) {
        try {
            return BinaryEnvelopeCodec.readString(stream(payload));
        } catch (IOException e) {
            System.err.println("[ProtocolPayloadCodec] decodeString failed: " + e);
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
