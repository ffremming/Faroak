package resources.domain.entity.component;

import resources.domain.entity.BaseEntity;
import resources.domain.inventory.DropTable;

/**
 * Marks an entity as something the player can harvest with a tool. Holds a
 * durability counter, the required tool category (matched against the equipped
 * item's name), and a {@link DropTable} rolled when durability hits zero.
 *
 * Stateful but trivially small; one instance per harvestable. The actual hit
 * logic + drop spawning lives in {@code domain/inventory/HarvestService} so
 * this stays a data carrier — easier to swap tools/drops without touching
 * harvest mechanics.
 */
public final class HarvestableComponent implements EntityComponent {

    private final String     requiredTool;
    private final DropTable  dropTable;
    private final int        maxDurability;

    private int durability;
    private boolean depleted;

    public HarvestableComponent(String requiredTool, int durability, DropTable dropTable) {
        if (durability <= 0) throw new IllegalArgumentException("durability must be > 0");
        this.requiredTool   = requiredTool;
        this.dropTable      = dropTable;
        this.maxDurability  = durability;
        this.durability     = durability;
    }

    @Override public void onAttach(BaseEntity owner) { /* no registration needed */ }
    @Override public void onDetach(BaseEntity owner) { /* no registration needed */ }

    /**
     * Apply one hit with the given tool name. No-op if tool mismatches the
     * required tool or the resource is already depleted.
     *
     * @return true if the hit landed (durability decremented).
     */
    public boolean hit(String toolName) {
        if (depleted) return false;
        if (requiredTool != null && !requiredTool.equals(toolName)) return false;
        durability--;
        if (durability <= 0) depleted = true;
        return true;
    }

    public boolean   isDepleted()    { return depleted; }
    public int       durability()    { return durability; }
    public int       maxDurability() { return maxDurability; }
    public String    requiredTool()  { return requiredTool; }
    public DropTable dropTable()     { return dropTable; }
}
