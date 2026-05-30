package resources.testing.probes;

import java.awt.Point;

import resources.app.GameContext;
import resources.domain.entity.BaseEntity;
import resources.domain.entity.component.GrowableComponent;
import resources.domain.farming.Crop;
import resources.domain.farming.FarmTile;
import resources.domain.inventory.Inventory;
import resources.domain.inventory.Item;
import resources.domain.inventory.Stack;
import resources.domain.player.Playable;
import resources.domain.tile.Tile;
import resources.testing.Logger;
import resources.testing.Probe;
import resources.testing.ProbeResult;
import resources.testing.TestHarness;
import resources.world.placement.TileRules;

/**
 * Drives the full tile-layer farming flow:
 *   1. Equip a hoe and till the tile under the player into a {@link FarmTile}
 *      (in place on the tile layer — no Farmland object is spawned).
 *   2. Swap to seeds and plant on that FarmTile.
 *   3. Tick enough times to mature the crop (stages * ticks-per-stage).
 *
 * Skips cleanly if the player isn't standing on a tillable tile — the default
 * spawn is usually plains/forest/savanna, but worldgen variability could land
 * them on water/desert, so we don't want a flaky failure.
 */
public final class FarmingProbe implements Probe {

    private static final Logger LOG = Logger.forClass(FarmingProbe.class);

    private static final String HOE   = "hoe";
    // FarmingService matches items prefixed "seeds_" / "crop_". Plant a fantasy
    // seed directly — it has art at every stage (unlike legacy wheat/carrot).
    private static final String SEEDS = "seeds_emberwheat";
    /** Crops grow one stage per in-game day; maturing four stages by ticking
     *  one day at a time would be millions of updates, so the probe jumps the
     *  clock forward and ticks once to let growth settle. */
    private static final int STAGES = 4;

    @Override public String name() { return "farming"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        GameContext ctx = harness.context();
        if (!(ctx.player() instanceof Playable)) return ProbeResult.skip(name() + " no Playable");
        Playable player = (Playable) ctx.player();

        // --- hoe ---
        equipItem(player, HOE, 1);
        if (!relocateToTillable(ctx, player)) {
            return ProbeResult.skip(name() + " no tillable tile within scan radius");
        }
        Point at = player.getPoint();
        FarmTile tilled = ctx.world().tillTileAt(at);
        if (tilled == null) {
            return ProbeResult.skip(name() + " could not till tile under player (non-tillable terrain)");
        }
        ctx.world().update(at);
        if (!(ctx.world().getTile(at) instanceof FarmTile)) {
            return ProbeResult.fail(name() + " hoe succeeded but tile is not a FarmTile",
                "tile=" + describe(ctx, at));
        }

        // --- plant ---
        equipItem(player, SEEDS, 4);
        int cropsBefore = countOf(ctx, Crop.class);
        boolean planted = resources.domain.farming.FarmingService.plantSeedAt(player, ctx, at);
        if (!planted) {
            return ProbeResult.fail(name() + " plant failed despite FarmTile present");
        }
        ctx.world().update(at);
        int cropsAfter = countOf(ctx, Crop.class);
        if (cropsAfter <= cropsBefore) {
            return ProbeResult.fail(name() + " plant succeeded but no Crop appeared",
                String.format("before=%d, after=%d", cropsBefore, cropsAfter));
        }

        // --- mature ---
        Crop crop = firstOfType(ctx, Crop.class);
        if (crop == null) return ProbeResult.fail(name() + " crop missing after place");
        // Jump the clock past (STAGES-1) full days, then tick once so the
        // day-paced GrowableComponent recomputes its stage from the new time.
        long jump = ctx.clock().ticksPerDay() * STAGES;
        ctx.clock().advance(jump);
        harness.tick(1);
        boolean mature = crop.isMature();
        GrowableComponent g = crop.getComponent(GrowableComponent.class);
        int stage = g == null ? -1 : g.currentStage();

        String detail = String.format(
            "crop-added=%d, days-jumped=%d, final-stage=%d, mature=%s",
            cropsAfter - cropsBefore, STAGES, stage, mature);
        LOG.info(detail);
        if (!mature) return ProbeResult.fail(name() + " crop did not mature", detail);
        return ProbeResult.pass(name(), detail);
    }

    /**
     * Scan a spiral of nearby tile centres for tillable terrain and teleport the
     * player onto one if found. Earlier probes may have shifted the player onto
     * water/mountain, so we re-anchor on solid farm ground before hoeing.
     */
    private static boolean relocateToTillable(GameContext ctx, Playable player) {
        int ts = ctx.tileSize();
        int cx = (int) player.getWorldX();
        int cy = (int) player.getWorldY();
        for (int r = 0; r <= 10; r++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dx = -r; dx <= r; dx++) {
                    if (Math.abs(dx) != r && Math.abs(dy) != r) continue; // ring boundary only
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

    private static String describe(GameContext ctx, Point p) {
        Tile t = ctx.world().getTile(p);
        return t == null ? "null" : t.getName();
    }

    private static void equipItem(Playable player, String itemName, int amount) {
        Inventory inv = player.getInventory();
        int slot = 27 + inv.getIndex();
        Item item  = new Item(player.panel, itemName);
        Stack stack = new Stack(player.panel, item, amount);
        inv.setStack(slot, stack);
    }

    private static int countOf(GameContext ctx, Class<?> type) {
        int n = 0;
        for (BaseEntity e : ctx.world().getEntities()) if (type.isInstance(e)) n++;
        return n;
    }

    @SuppressWarnings("unchecked")
    private static <T extends BaseEntity> T firstOfType(GameContext ctx, Class<T> type) {
        for (BaseEntity e : ctx.world().getEntities()) {
            if (type.isInstance(e)) return (T) e;
        }
        return null;
    }
}
