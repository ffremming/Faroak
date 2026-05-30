package resources.testing.probes;

import java.awt.Point;

import resources.app.GameContext;
import resources.domain.entity.BaseEntity;
import resources.domain.farming.Crop;
import resources.domain.farming.FarmTile;
import resources.domain.farming.FarmingService;
import resources.domain.inventory.Inventory;
import resources.domain.inventory.Item;
import resources.domain.inventory.Stack;
import resources.domain.object.GroundItem;
import resources.domain.player.Playable;
import resources.domain.tile.Tile;
import resources.testing.Logger;
import resources.testing.Probe;
import resources.testing.ProbeResult;
import resources.testing.TestHarness;
import resources.world.placement.TileRules;

/**
 * End-to-end farming probe through HARVEST: till → plant a fantasy crop →
 * mature it → harvest the mature crop and verify produce drops spawn on the
 * ground. Complements {@link FarmingProbe} (which stops at maturity) by
 * exercising the drop path that gameplay reported as broken.
 */
public final class CropHarvestProbe implements Probe {

    private static final Logger LOG = Logger.forClass(CropHarvestProbe.class);

    private static final String HOE     = "hoe";
    private static final String SEEDS   = "seeds_emberwheat";
    private static final String PRODUCE = "emberwheat";

    @Override public String name() { return "crop-harvest"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        GameContext ctx = harness.context();
        if (!(ctx.player() instanceof Playable)) return ProbeResult.skip(name() + " no Playable");
        Playable player = (Playable) ctx.player();

        equipItem(player, HOE, 1);
        if (!relocateToTillable(ctx, player)) {
            return ProbeResult.skip(name() + " no tillable tile within scan radius");
        }
        Point at = player.getPoint();
        FarmTile tilled = ctx.world().tillTileAt(at);
        if (tilled == null) return ProbeResult.skip(name() + " could not till tile under player");
        ctx.world().update(at);

        equipItem(player, SEEDS, 4);
        if (!FarmingService.plantSeedAt(player, ctx, at)) {
            return ProbeResult.fail(name() + " plant failed despite FarmTile present");
        }
        ctx.world().update(at);

        Crop crop = tilled.crop();
        if (crop == null) return ProbeResult.fail(name() + " crop missing after plant");

        // Mature it. Growth is day-paced, so jump the clock forward several
        // days and tick once rather than ticking through millions of updates.
        ctx.clock().advance(ctx.clock().ticksPerDay() * 4);
        harness.tick(1);
        if (!crop.isMature()) {
            return ProbeResult.fail(name() + " crop did not mature");
        }

        // Stand next to the crop so its hitbox is in interaction reach, then harvest.
        player.position(crop.getPoint());
        player.resetInteractionHitBox();
        ctx.world().update(player.getPoint());

        int produceBefore = countItems(player.getInventory(), PRODUCE);
        BaseEntity hit = player.harvestService().attackEntity(player, ctx, crop);
        harness.tick(1);
        ctx.world().update(player.getPoint());

        boolean droppedOnGround = countGroundItems(ctx, PRODUCE) > 0;
        // Let walk-over pickup pull the drops in.
        harness.tick(40);
        int produceAfter = countItems(player.getInventory(), PRODUCE);

        String detail = String.format(
            "harvest-hit=%s, dropped-on-ground=%s, produce-collected=%d",
            hit != null, droppedOnGround, produceAfter - produceBefore);
        LOG.info(detail);

        if (hit == null)            return ProbeResult.fail(name() + " harvest hit returned null", detail);
        if (!droppedOnGround)       return ProbeResult.fail(name() + " no produce dropped on ground", detail);
        if (produceAfter <= produceBefore) return ProbeResult.fail(name() + " produce not picked up", detail);
        return ProbeResult.pass(name(), detail);
    }

    private static boolean relocateToTillable(GameContext ctx, Playable player) {
        int ts = ctx.tileSize();
        int cx = (int) player.getWorldX();
        int cy = (int) player.getWorldY();
        for (int r = 0; r <= 10; r++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dx = -r; dx <= r; dx++) {
                    if (Math.abs(dx) != r && Math.abs(dy) != r) continue;
                    Point p = new Point(cx + dx * ts, cy + dy * ts);
                    Tile t = ctx.world().getTile(p);
                    if (t == null) continue;
                    if (TileRules.isTillable(t.getName())) {
                        player.setWorldX(p.x);
                        player.setWorldY(p.y);
                        player.resetInteractionHitBox();
                        ctx.world().update(player.getPoint());
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static void equipItem(Playable player, String itemName, int amount) {
        Inventory inv = player.getInventory();
        int slot = 27 + inv.getIndex();
        Item item  = new Item(player.panel, itemName);
        Stack stack = new Stack(player.panel, item, amount);
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
