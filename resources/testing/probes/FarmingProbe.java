package resources.testing.probes;

import java.awt.Point;

import resources.app.GameContext;
import resources.domain.entity.BaseEntity;
import resources.domain.entity.component.GrowableComponent;
import resources.domain.farming.Crop;
import resources.domain.farming.Farmland;
import resources.domain.farming.FarmingService;
import resources.domain.inventory.Inventory;
import resources.domain.inventory.Item;
import resources.domain.inventory.Stack;
import resources.domain.player.Playable;
import resources.domain.tile.Tile;
import resources.testing.Logger;
import resources.testing.Probe;
import resources.testing.ProbeResult;
import resources.testing.TestHarness;

/**
 * Drives the full farming flow:
 *   1. Equip a hoe and till the tile under the player into Farmland.
 *   2. Swap to seeds_wheat and plant on that Farmland.
 *   3. Tick enough times to mature the crop (stages * ticks-per-stage).
 *
 * Skips cleanly if the player isn't standing on a tillable tile — the default
 * spawn is usually plains/forest/savanna, but worldgen variability could land
 * them on water/desert, so we don't want a flaky failure.
 */
public final class FarmingProbe implements Probe {

    private static final Logger LOG = Logger.forClass(FarmingProbe.class);

    private static final String HOE   = "hoe";
    // FarmingService.tryPlantOnFarmland matches items prefixed "crop_" (the
    // crop registry key). The inventory's "seeds_wheat" item is a placeable
    // alias only, not a seed for the planting service.
    private static final String SEEDS = "crop_wheat";
    /** {@code Crop.STAGES * Crop.TICKS_PER_STAGE = 4 * 600 = 2400} */
    private static final int GROWTH_TICKS = 2400;

    @Override public String name() { return "farming"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        GameContext ctx = harness.context();
        if (!(ctx.player() instanceof Playable)) return ProbeResult.skip(name() + " no Playable");
        Playable player = (Playable) ctx.player();

        // --- hoe ---
        equipItem(player, HOE, 1);
        int farmlandBefore = countOf(ctx, Farmland.class);
        if (!relocateToTillable(ctx, player)) {
            return ProbeResult.skip(name() + " no tillable tile within scan radius");
        }
        boolean hoed = FarmingService.tryHoeTile(player, ctx);
        if (!hoed) {
            return ProbeResult.skip(name() + " could not till tile under player (non-tillable terrain)");
        }
        ctx.world().update(player.getPoint());
        int farmlandAfter = countOf(ctx, Farmland.class);
        if (farmlandAfter <= farmlandBefore) {
            return ProbeResult.fail(name() + " hoe succeeded but no Farmland appeared",
                String.format("before=%d, after=%d", farmlandBefore, farmlandAfter));
        }

        // --- plant ---
        equipItem(player, SEEDS, 4);
        int cropsBefore = countOf(ctx, Crop.class);
        boolean planted = FarmingService.tryPlantOnFarmland(player, ctx);
        if (!planted) {
            return ProbeResult.fail(name() + " plant failed despite farmland present");
        }
        ctx.world().update(player.getPoint());
        int cropsAfter = countOf(ctx, Crop.class);
        if (cropsAfter <= cropsBefore) {
            return ProbeResult.fail(name() + " plant succeeded but no Crop appeared",
                String.format("before=%d, after=%d", cropsBefore, cropsAfter));
        }

        // --- mature ---
        Crop crop = firstOfType(ctx, Crop.class);
        if (crop == null) return ProbeResult.fail(name() + " crop missing after place");
        harness.tick(GROWTH_TICKS + 10);
        boolean mature = crop.isMature();
        GrowableComponent g = crop.getComponent(GrowableComponent.class);
        int stage = g == null ? -1 : g.currentStage();

        String detail = String.format(
            "farmland-added=%d, crop-added=%d, ticks=%d, final-stage=%d, mature=%s",
            farmlandAfter - farmlandBefore, cropsAfter - cropsBefore,
            GROWTH_TICKS, stage, mature);
        LOG.info(detail);
        if (!mature) return ProbeResult.fail(name() + " crop did not mature", detail);
        return ProbeResult.pass(name(), detail);
    }

    /**
     * Scan a spiral of nearby tile centres for plains/forest/savanna terrain
     * and teleport the player onto one if found. Earlier probes may have
     * shifted the player onto water/mountain, so we re-anchor on solid farm
     * ground before driving the hoe service.
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
                    if (isTillable(t.getName())) {
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

    private static boolean isTillable(String name) {
        return name != null
            && (name.startsWith("plains") || name.startsWith("forest") || name.startsWith("savanna"));
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
