package resources.domain.inventory;

import resources.core.id.Identifier;

/**
 * Data-driven definition of a stackable item. Held in {@link ItemTypeRegistry}.
 *
 * Replaces the previous approach of subclassing/parsing strings inside {@code Item}.
 * New item kinds are added by registering a definition; gameplay code looks up
 * properties through this object rather than name-checking strings.
 */
public final class ItemType {

    private final Identifier id;
    private final String spriteName;
    private final int    maxStack;
    private final String placedEntityName;  // optional — for items that place a GameObject

    public ItemType(Identifier id, String spriteName, int maxStack, String placedEntityName) {
        this.id             = id;
        this.spriteName     = spriteName;
        this.maxStack       = maxStack;
        this.placedEntityName = placedEntityName;
    }

    public ItemType(Identifier id, String spriteName, int maxStack) {
        this(id, spriteName, maxStack, null);
    }

    public Identifier id()                { return id; }
    public String     spriteName()        { return spriteName; }
    public int        maxStack()          { return maxStack; }

    /** Name of the GameObject to place when the player uses this item, or null. */
    public String     placedEntityName()  { return placedEntityName; }

    public boolean    placeable()         { return placedEntityName != null; }
}
