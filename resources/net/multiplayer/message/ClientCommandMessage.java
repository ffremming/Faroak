package resources.net.multiplayer.message;

import resources.net.multiplayer.protocol.ProtocolPayloads;

public final class ClientCommandMessage implements ClientMessage {

    private final String playerId;
    private final long sequence;
    private final ProtocolPayloads.CommandRequest command;

    public ClientCommandMessage(String playerId, long sequence, ProtocolPayloads.CommandRequest command) {
        this.playerId = playerId == null ? "" : playerId;
        this.sequence = Math.max(0L, sequence);
        this.command = command;
    }

    @Override public String playerId() { return playerId; }
    @Override public long sequence() { return sequence; }
    public ProtocolPayloads.CommandRequest command() { return command; }
}
