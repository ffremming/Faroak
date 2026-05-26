package resources.net.multiplayer.protocol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Binary codec for {@link ProtocolEnvelope}.
 */
public final class BinaryEnvelopeCodec {

    private static final int MAX_PAYLOAD_BYTES = 1_000_000;

    public byte[] encode(ProtocolEnvelope envelope) {
        if (envelope == null || envelope.messageType() == null) return new byte[0];
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);
            out.writeInt(envelope.protocolVersion());
            out.writeByte(envelope.messageType().ordinal());
            writeString(out, envelope.playerId());
            out.writeLong(envelope.sequence());
            out.writeLong(envelope.ackSequence());
            out.writeLong(envelope.serverTick());
            byte[] payload = envelope.payload();
            out.writeInt(payload.length);
            out.write(payload);
            out.flush();
            return baos.toByteArray();
        } catch (IOException ignored) {
            return new byte[0];
        }
    }

    public ProtocolEnvelope decode(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return null;
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
            int version = Math.max(1, in.readInt());
            ProtocolMessageType type = ProtocolMessageType.fromOrdinal(in.readUnsignedByte());
            String playerId = readString(in);
            long sequence = Math.max(0L, in.readLong());
            long ackSequence = Math.max(0L, in.readLong());
            long serverTick = Math.max(0L, in.readLong());
            int len = in.readInt();
            if (len < 0 || len > MAX_PAYLOAD_BYTES) return null;
            byte[] payload = new byte[len];
            in.readFully(payload);
            if (type == null) return null;
            return new ProtocolEnvelope(version, playerId, sequence, ackSequence, serverTick, type, payload);
        } catch (IOException ignored) {
            return null;
        }
    }

    static void writeString(DataOutputStream out, String value) throws IOException {
        byte[] bytes = (value == null) ? new byte[0] : value.getBytes(StandardCharsets.UTF_8);
        out.writeInt(bytes.length);
        out.write(bytes);
    }

    static String readString(DataInputStream in) throws IOException {
        int len = in.readInt();
        if (len < 0 || len > MAX_PAYLOAD_BYTES) return "";
        byte[] bytes = new byte[len];
        in.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
