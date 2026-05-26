package resources.net.multiplayer.protocol;

import java.util.Arrays;

/**
 * Versioned binary message envelope used on the wire.
 */
public final class ProtocolEnvelope {

    private final int protocolVersion;
    private final String playerId;
    private final long sequence;
    private final long ackSequence;
    private final long serverTick;
    private final ProtocolMessageType messageType;
    private final byte[] payload;

    public ProtocolEnvelope(
            int protocolVersion,
            String playerId,
            long sequence,
            long ackSequence,
            long serverTick,
            ProtocolMessageType messageType,
            byte[] payload) {
        this.protocolVersion = Math.max(1, protocolVersion);
        this.playerId = (playerId == null) ? "" : playerId;
        this.sequence = Math.max(0L, sequence);
        this.ackSequence = Math.max(0L, ackSequence);
        this.serverTick = Math.max(0L, serverTick);
        this.messageType = messageType;
        this.payload = (payload == null) ? new byte[0] : Arrays.copyOf(payload, payload.length);
    }

    public int protocolVersion() { return protocolVersion; }
    public String playerId() { return playerId; }
    public long sequence() { return sequence; }
    public long ackSequence() { return ackSequence; }
    public long serverTick() { return serverTick; }
    public ProtocolMessageType messageType() { return messageType; }
    public byte[] payload() { return Arrays.copyOf(payload, payload.length); }
}
