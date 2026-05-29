package resources.domain.inventory;

import java.util.Random;

import resources.app.GameContext;
import resources.domain.entity.BaseEntity;
import resources.domain.entity.component.HarvestableComponent;
import resources.domain.entity.component.HealthComponent;
import resources.domain.object.GameObject;
import resources.domain.object.GroundItem;
import resources.domain.player.Playable;
import resources.geometry.HitBox;
import resources.geometry.Vector;
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
        // Resolve to the NEAREST harvestable in the interaction box so the
        // player swings at the visible target rather than whichever entity
        // the chunk happened to add first.
        return attackEntity(player, ctx, nearestHarvestable(player, ctx, reach));
    }

    /**
     * Targeted variant: apply one harvest hit to a specific entity. Used by
     * the mouse-driven harvest path where the player clicked a particular
     * tree/stone/crop. Same authorisation, event publishing, drop-table,
     * and removal-queue semantics as {@link #attack(Playable, GameContext)}.
     */
    public BaseEntity attackEntity(Playable player, GameContext ctx, BaseEntity target) {
        if (target == null) return null;
        HarvestableComponent h = target.getComponent(HarvestableComponent.class);
        if (h == null) return null;
        String tool = equippedToolName(player);

        HarvestIntentEvent intent = new HarvestIntentEvent(
            player.getName(), target.getName(), tool);
        if (!ctx.authority().authorize(intent)) return null;
        ctx.events().publish(intent);

        boolean landed = h.hit(tool);
        if (!landed) return null;
        spawnHarvestEffects(player, ctx, target, tool);
        if (h.isDepleted()) {
            awardDrops(ctx, h.dropTable(), target);
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
            if (ent.getComponent(HealthComponent.class) != null) continue;
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

    /** Swing-arc duration, spread and orbit radius for a harvest swing. Tuned to
     *  read as a deliberate chop rather than a fast combat combo. */
    private static final int    SWING_TICKS      = 12;
    private static final double SWING_ARC_DEG    = 130.0;
    private static final double SWING_RADIUS_PX  = 38.0;

    /**
     * Reuse the melee swing + impact VFX (slash arc from {@code equipment.png}
     * plus a star-burst on the target) so chopping/mining looks like striking
     * an enemy. Aims the swing from the player toward the harvested entity and
     * uses the equipped tool name (e.g. "axe") as the swing sprite.
     */
    private void spawnHarvestEffects(Playable player, GameContext ctx, BaseEntity target, String tool) {
        double px = player.getWorldX() + player.getWidth()  / 2.0;
        double py = player.getWorldY() + player.getHeight() / 2.0;
        double tx = target.getWorldX() + target.getWidth()  / 2.0;
        double ty = target.getWorldY() + target.getHeight() / 2.0;
        Vector aim = new Vector(tx - px, ty - py);
        player.combat().spawnActionEffects(
            player, ctx, aim, tool,
            SWING_TICKS, SWING_ARC_DEG, SWING_RADIUS_PX, target);
    }

    /**
     * Spill the rolled drops onto the ground at the harvested entity's centre
     * rather than straight into the inventory: the player collects them by
     * walking over the {@link GroundItem}s (see
     * {@link resources.domain.player.Playable}). Each stack is scattered a few
     * pixels off-centre so multiple drops from one harvest don't stack into a
     * single sprite.
     */
    private void awardDrops(GameContext ctx, DropTable table, BaseEntity source) {
        if (table == null) return;
        int cx = (int) (source.getWorldX() + source.getWidth()  / 2.0);
        int cy = (int) (source.getWorldY() + source.getHeight() / 2.0);
        for (DropTable.Drop drop : table.roll(rng)) {
            int dx = rng.nextInt(SCATTER_PX * 2 + 1) - SCATTER_PX;
            int dy = rng.nextInt(SCATTER_PX * 2 + 1) - SCATTER_PX;
            GroundItem ground = new GroundItem(
                ctx.player().panel, drop.itemName, drop.quantity, cx + dx, cy + dy);
            ctx.world().placeEntity(ground);
        }
    }

    /** Max pixel offset, in any direction, applied when scattering a harvest's
     *  individual drop stacks around the source's centre. */
    private static final int SCATTER_PX = 16;

    private String equippedToolName(Playable player) {
        Stack eq = player.getEquipped();
        if (eq == null || eq.isEmpty()) return null;
        return eq.getName();
    }
}
