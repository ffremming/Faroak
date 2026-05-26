package resources.net.multiplayer.message;

public final class ClientInputMessage implements ClientMessage {

    private final String playerId;
    private final long sequence;
    private final boolean up;
    private final boolean left;
    private final boolean down;
    private final boolean right;

    public ClientInputMessage(String playerId, long sequence, boolean up, boolean left, boolean down, boolean right) {
        this.playerId = playerId;
        this.sequence = sequence;
        this.up = up;
        this.left = left;
        this.down = down;
        this.right = right;
    }

    @Override public String playerId() { return playerId; }
    @Override public long sequence()    { return sequence; }

    public boolean up()     { return up; }
    public boolean left()   { return left; }
    public boolean down()   { return down; }
    public boolean right()  { return right; }
}
