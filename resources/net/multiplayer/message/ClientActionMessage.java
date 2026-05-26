package resources.net.multiplayer.message;

import resources.net.multiplayer.MultiplayerAction;

public final class ClientActionMessage implements ClientMessage {

    private final String playerId;
    private final long sequence;
    private final MultiplayerAction action;
    private final boolean hasTarget;
    private final double targetX;
    private final double targetY;
    private final String argument;

    public ClientActionMessage(String playerId, long sequence, MultiplayerAction action) {
        this(playerId, sequence, action, false, 0.0, 0.0, "");
    }

    public ClientActionMessage(
            String playerId,
            long sequence,
            MultiplayerAction action,
            boolean hasTarget,
            double targetX,
            double targetY) {
        this(playerId, sequence, action, hasTarget, targetX, targetY, "");
    }

    public ClientActionMessage(
            String playerId,
            long sequence,
            MultiplayerAction action,
            boolean hasTarget,
            double targetX,
            double targetY,
            String argument) {
        this.playerId = playerId;
        this.sequence = sequence;
        this.action = action;
        this.hasTarget = hasTarget;
        this.targetX = targetX;
        this.targetY = targetY;
        this.argument = (argument == null) ? "" : argument;
    }

    @Override public String playerId() { return playerId; }
    @Override public long sequence()    { return sequence; }

    public MultiplayerAction action()   { return action; }
    public boolean hasTarget()          { return hasTarget; }
    public double targetX()             { return targetX; }
    public double targetY()             { return targetY; }
    public String argument()            { return argument; }
}
