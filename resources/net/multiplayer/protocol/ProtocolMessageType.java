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
    PLAYER_JOIN_LEAVE,

    // Protocol v2 typed command path. Kept at the end so existing ordinal
    // values remain stable for older probes using v1 ACTION messages.
    COMMAND,
    COMMAND_RESULT,

    // Chat. Appended last to keep all earlier ordinals stable.
    CHAT,            // Client -> Server: a chat line from a player
    CHAT_BROADCAST;  // Server -> Client: a chat line relayed to everyone

    public static ProtocolMessageType fromOrdinal(int ordinal) {
        ProtocolMessageType[] values = values();
        if (ordinal < 0 || ordinal >= values.length) return null;
        return values[ordinal];
    }
}
