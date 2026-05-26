package resources.net.multiplayer.protocol;

/**
 * Protocol v1 message types.
 */
public enum ProtocolMessageType {
    // Client -> Server
    JOIN,
    INPUT_STATE,
    ACTION,
    PING,
    LEAVE,

    // Server -> Client
    WELCOME,
    REJECT,
    BASELINE_SNAPSHOT,
    DELTA_SNAPSHOT,
    ACK,
    PLAYER_JOIN_LEAVE;

    public static ProtocolMessageType fromOrdinal(int ordinal) {
        ProtocolMessageType[] values = values();
        if (ordinal < 0 || ordinal >= values.length) return null;
        return values[ordinal];
    }
}
