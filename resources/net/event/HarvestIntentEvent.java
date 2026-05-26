package resources.net.event;

import resources.core.event.GameEvent;

/**
 * Proposed harvest action against a specific target.
 */
public final class HarvestIntentEvent implements GameEvent {

    private final String playerName;
    private final String targetName;
    private final String toolName;

    public HarvestIntentEvent(String playerName, String targetName, String toolName) {
        this.playerName = playerName;
        this.targetName = targetName;
        this.toolName = toolName;
    }

    public String playerName() { return playerName; }
    public String targetName() { return targetName; }
    public String toolName()   { return toolName; }
}
