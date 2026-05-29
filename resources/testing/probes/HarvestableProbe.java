package resources.testing.probes;

import java.awt.Point;

import resources.app.GameContext;
import resources.domain.entity.component.HarvestableComponent;
import resources.domain.inventory.HarvestRegistry;
import resources.domain.inventory.Inventory;
import resources.domain.inventory.Item;
import resources.domain.inventory.Stack;
import resources.domain.entity.BaseEntity;
import resources.domain.object.GameObject;
import resources.domain.object.GroundItem;
import resources.domain.player.Playable;
import resources.testing.Logger;
import resources.testing.Probe;
import resources.testing.ProbeResult;
import resources.testing.TestHarness;

/**
 * Spawns a harvestable object (birch tree) adjacent to the player, equips an
 * axe, calls Playable.attack until the resource depletes, then verifies the
 * full drop→ground→pickup loop:
 *   - durability counter decremented per hit
 *   - the entity was queued for removal once depleted
 *   - drops materialised as {@link GroundItem}s in the world
 *   - walking the player over the drops collected them into the inventory
 *
 * Catches regressions where the component is attached but the service no-ops,
 * tool matching is broken, drops never spawn on the ground, or the walk-over
 * pickup fails to move them into the inventory.
 */
public final class HarvestableProbe implements Probe {

    private static final Logger LOG = Logger.forClass(HarvestableProbe.class);

    private static final String TARGET_OBJECT = "birch_M";
    private static final String TOOL_NAME     = "axe";
    private static final String EXPECTED_DROP = "block";

    @Override public String name() { return "harvestable"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        GameContext ctx = harness.context();
        if (!(ctx.player() instanceof Playable)) return ProbeResult.skip(name() + " no Playable");
        Playable player = (Playable) ctx.player();

        equipAxe(player);
        // Earlier probes may have shifted the player onto water/solid terrain
        // where placeEntity refuses the target; re-anchor on walkable ground.
        if (!relocateToWalkable(ctx, player)) {
            return ProbeResult.skip(name() + " no walkable tile within scan radius");
        }
        GameObject target = spawnTargetNextToPlayer(ctx, player);
        if (target == null) return ProbeResult.skip(name() + " no free tile to place target on this seed");
        // index.entities() is rebuilt by world.update(point), called only once
        // per second of game ticks; force a refresh so the new entity is
        // visible to HarvestService without a long wait.
        ctx.world().update(player.getPoint());

        HarvestableComponent harvest = target.getComponent(HarvestableComponent.class);
        if (harvest == null) {
            ctx.world().removeEntity(target);
            return ProbeResult.fail(name() + " target missing HarvestableComponent");
        }

        int startDrops = countItems(player.getInventory(), EXPECTED_DROP);
        int startDurability = harvest.durability();

        // Reset cached interaction box so it picks up the latest direction/pos.
        player.resetInteractionHitBox();
        int swings = 0;
        for (int i = 0; i < harvest.maxDurability() + 2 && !harvest.isDepleted(); i++) {
            player.attack();
            swings++;
        }
        // Drain the removal queue so getEntities() reflects the kill, and let
        // world.update(point) re-index so the freshly-placed GroundItems become
        // queryable.
        harness.tick(1);
        ctx.world().update(player.getPoint());
        boolean droppedOnGround = countGroundItems(ctx, EXPECTED_DROP) > 0;

        // Now run enough ticks for the pickup grace to elapse and the per-tick
        // walk-over collection in Playable.update() to pull the drops in. The
        // tree was placed on the player's hitbox, so the scattered drops land
        // within pickup range without any movement.
        harness.tick(40);

        int endDrops = countItems(player.getInventory(), EXPECTED_DROP);

        String detail = String.format(
            "swings=%d, start-dura=%d, end-dura=%d, depleted=%s, dropped-on-ground=%s, drops-collected=%d",
            swings, startDurability, harvest.durability(), harvest.isDepleted(),
            droppedOnGround, endDrops - startDrops);
        LOG.info(detail);

        if (!harvest.isDepleted())   return ProbeResult.fail(name() + " did not deplete", detail);
        if (!droppedOnGround)        return ProbeResult.fail(name() + " no ground drops spawned", detail);
        if (endDrops <= startDrops)  return ProbeResult.fail(name() + " drops not picked up", detail);
        return ProbeResult.pass(name(), detail);
    }

    /** Teleport the player onto the first nearby non-solid tile so the target
     *  tree and its drops have valid ground to occupy. */
    private static boolean relocateToWalkable(GameContext ctx, Playable player) {
        int ts = ctx.tileSize();
        int cx = (int) player.getWorldX();
        int cy = (int) player.getWorldY();
        for (int r = 0; r <= 12; r++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dx = -r; dx <= r; dx++) {
                    if (Math.abs(dx) != r && Math.abs(dy) != r) continue; // ring boundary only
                    Point p = new Point(cx + dx * ts, cy + dy * ts);
                    resources.domain.tile.Tile t = ctx.world().getTile(p);
                    if (t == null || t.isSolid()) continue;
                    player.position(p);
                    player.resetInteractionHitBox();
                    ctx.world().update(player.getPoint());
                    if (!ctx.world().solidCollision(player.getHitBox(), player)) return true;
                }
            }
        }
        return false;
    }

    private GameObject spawnTargetNextToPlayer(GameContext ctx, Playable player) {
        // Centre the target on the player's interaction box so attack() resolves
        // it regardless of facing, and so the post-harvest drops land within the
        // player's walk-over pickup region. Falls back to a few offsets if the
        // primary tile is blocked.
        player.resetInteractionHitBox();
        Point reach = player.getInteractionHitBox().getCenter();
        int[][] offsets = { {0, 0}, {0, -32}, {32, 0}, {-32, 0}, {0, 32} };
        for (int[] off : offsets) {
            int x = reach.x - 32 + off[0];
            int y = reach.y - 32 + off[1];
            GameObject obj = new GameObject(ctx.player().panel, TARGET_OBJECT,
                x, y, 64, 64, 48, 48, 0, 0, false);
            HarvestableComponent h = HarvestRegistry.componentFor(TARGET_OBJECT);
            if (h == null) return null;
            obj.components().add(h);
            if (ctx.world().placeEntity(obj)) return obj;
        }
        return null;
    }

    private void equipAxe(Playable player) {
        Inventory inv = player.getInventory();
        // Toolbar lives at slots 27..35; getEquipped() reads 27 + index.
        int slot = 27 + inv.getIndex();
        Item axe = new Item(player.panel, TOOL_NAME);
        Stack stack = new Stack(player.panel, axe, 1);
        inv.setStack(slot, stack);
    }

    private int countGroundItems(GameContext ctx, String itemName) {
        int total = 0;
        for (BaseEntity e : ctx.world().getEntities()) {
            if (e instanceof GroundItem && itemName.equals(((GroundItem) e).getItemName())) {
                total += ((GroundItem) e).getQuantity();
            }
        }
        return total;
    }

    private int countItems(Inventory inv, String itemName) {
        int total = 0;
        for (int i = 0; i < inv.getSize(); i++) {
            Stack s = inv.getStack(i);
            if (s != null && !s.isEmpty() && itemName.equals(s.getName())) {
                total += s.getAmount();
            }
        }
        return total;
    }
}
