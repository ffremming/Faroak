package resources.domain.inventory;

import java.util.Random;

import resources.app.GameContext;
import resources.domain.entity.BaseEntity;
import resources.domain.entity.component.HarvestableComponent;
import resources.domain.object.GameObject;
import resources.domain.player.Playable;
import resources.geometry.HitBox;
import resources.net.event.HarvestIntentEvent;

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

        // Resolve to the NEAREST harvestable in the interaction box so the
        // player swings at the visible target rather than whichever entity
        // the chunk happened to add first.
        BaseEntity target = nearestHarvestable(player, ctx, reach);
        if (target == null) return null;
        HarvestableComponent h = target.getComponent(HarvestableComponent.class);

        HarvestIntentEvent intent = new HarvestIntentEvent(
            player.getName(), target.getName(), tool);
        if (!ctx.authority().authorize(intent)) return null;
        ctx.events().publish(intent);

        boolean landed = h.hit(tool);
        if (!landed) return null;
        if (h.isDepleted()) {
            awardDrops(player, ctx, h.dropTable());
            ctx.world().addToRemovalQueue(target);
        }
        return target;
    }

    private static BaseEntity nearestHarvestable(Playable player, GameContext ctx, HitBox reach) {
        double pcx = player.getWorldX() + player.getWidth()  / 2.0;
        double pcy = player.getWorldY() + player.getHeight() / 2.0;
        BaseEntity best = null;
        double bestSq = Double.POSITIVE_INFINITY;
        for (BaseEntity ent : ctx.world().getEntities()) {
            if (!(ent instanceof GameObject)) continue;
            if (ent.getComponent(HarvestableComponent.class) == null) continue;
            if (!ent.getHitBox().intersects(reach)) continue;
            double ex = ent.getWorldX() + ent.getWidth()  / 2.0;
            double ey = ent.getWorldY() + ent.getHeight() / 2.0;
            double dx = ex - pcx;
            double dy = ey - pcy;
            double sq = dx * dx + dy * dy;
            if (sq < bestSq) { bestSq = sq; best = ent; }
        }
        return best;
    }

    private void awardDrops(Playable player, GameContext ctx, DropTable table) {
        if (table == null) return;
        for (DropTable.Drop drop : table.roll(rng)) {
            Item item = new Item(ctx.player().panel, drop.itemName);
            player.getInventory().addItem(item, drop.quantity);
        }
    }

    private String equippedToolName(Playable player) {
        Stack eq = player.getEquipped();
        if (eq == null || eq.isEmpty()) return null;
        return eq.getName();
    }
}
