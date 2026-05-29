package resources.domain.entity.component;

import resources.domain.inventory.DropTable;

/**
 * Optional loot payload for health-based entities.
 *
 * Combat systems read this on death and roll the attached table.
 */
public final class LootComponent implements EntityComponent {

    private final DropTable dropTable;

    public LootComponent(DropTable dropTable) {
        this.dropTable = dropTable;
    }

    public DropTable dropTable() { return dropTable; }
}
