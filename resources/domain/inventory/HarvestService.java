package resources.domain.inventory;

import java.util.Random;

import resources.app.GameContext;
import resources.domain.entity.BaseEntity;
import resources.domain.entity.component.HarvestableComponent;
import resources.domain.object.GameObject;
import resources.domain.player.Playable;
import resources.geometry.HitBox;

/**
 * Applies a harvest hit from a player to whatever Harvestable-bearing
 * GameObject sits within the player's interaction box. On depletion, rolls
 * the {@link DropTable} and delivers items into the player's inventory.
 *
 * Stateless except for the RNG; one instance per player is fine. Kept apart
 * from {@link HarvestableComponent} so the component remains a data carrier
 * and the loot/world side-effects live on the system that owns them.
 */
public final class HarvestService {

    private final Random rng;

    public HarvestService()           { this(new Random()); }
    public HarvestService(Random rng) { this.rng = rng; }

    /**
     * Try one harvest action with the player's currently equipped item. Returns
     * the targeted entity (so callers can swing-animate / sfx), or {@code null}
     * if nothing was in range.
     */
    public BaseEntity attack(Playable player, GameContext ctx) {
        HitBox reach = player.getInteractionHitBox();
        String tool  = equippedToolName(player);

        for (BaseEntity ent : ctx.world().getEntities()) {
            if (!(ent instanceof GameObject)) continue;
            HarvestableComponent h = ent.getComponent(HarvestableComponent.class);
            if (h == null) continue;
            if (!ent.getHitBox().intersects(reach)) continue;

            boolean landed = h.hit(tool);
            if (h.isDepleted()) {
                awardDrops(player, ctx, h.dropTable());
                ctx.world().addToRemovalQueue(ent);
            }
            return landed ? ent : null;
        }
        return null;
    }

    private void awardDrops(Playable player, GameContext ctx, DropTable table) {
        if (table == null) return;
        for (DropTable.Drop drop : table.roll(rng)) {
            Item item = new Item(ctx.player().panel, drop.itemName);
            // Inventory.addItem(item, n) is the working path; Playable.addItem
            // / Inventory.addStack only land in same-named slots and silently
            // drops the rest, which is the wrong contract here.
            player.getInventory().addItem(item, drop.quantity);
        }
    }

    private String equippedToolName(Playable player) {
        Stack eq = player.getEquipped();
        if (eq == null || eq.isEmpty()) return null;
        return eq.getName();
    }
}
