package resources.testing.probes;

import resources.app.GameContext;
import resources.domain.entity.component.HarvestableComponent;
import resources.domain.inventory.HarvestRegistry;
import resources.domain.inventory.Inventory;
import resources.domain.inventory.Item;
import resources.domain.inventory.Stack;
import resources.domain.object.GameObject;
import resources.domain.player.Playable;
import resources.testing.Logger;
import resources.testing.Probe;
import resources.testing.ProbeResult;
import resources.testing.TestHarness;

/**
 * Spawns a harvestable object (birch tree) adjacent to the player, equips an
 * axe, calls Playable.attack until the resource depletes, then verifies:
 *   - durability counter decremented per hit
 *   - the entity was queued for removal once depleted
 *   - drops landed in the inventory
 *
 * Catches regressions where the component is attached but the service no-ops,
 * tool matching is broken, or drops never reach the inventory.
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
        GameObject target = spawnTargetNextToPlayer(ctx, player);
        if (target == null) return ProbeResult.fail(name() + " could not place target");
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
        // Drain the removal queue so getEntities() reflects the kill.
        harness.tick(1);

        int endDrops = countItems(player.getInventory(), EXPECTED_DROP);

        String detail = String.format(
            "swings=%d, start-dura=%d, end-dura=%d, depleted=%s, drops-gained=%d",
            swings, startDurability, harvest.durability(), harvest.isDepleted(),
            endDrops - startDrops);
        LOG.info(detail);

        if (!harvest.isDepleted()) return ProbeResult.fail(name() + " did not deplete", detail);
        if (endDrops <= startDrops) return ProbeResult.fail(name() + " no drops awarded", detail);
        return ProbeResult.pass(name(), detail);
    }

    private GameObject spawnTargetNextToPlayer(GameContext ctx, Playable player) {
        // Drop the tree onto the player's interaction box so attack() will hit
        // it regardless of which way the player is currently facing.
        int x = (int) player.getWorldX() + 8;
        int y = (int) player.getWorldY() - 32;
        GameObject obj = new GameObject(ctx.player().panel, TARGET_OBJECT,
            x, y, 64, 64, 48, 48, 0, 0, false);
        HarvestableComponent h = HarvestRegistry.componentFor(TARGET_OBJECT);
        if (h == null) return null;
        obj.components().add(h);
        if (!ctx.world().placeEntity(obj)) return null;
        return obj;
    }

    private void equipAxe(Playable player) {
        Inventory inv = player.getInventory();
        // Toolbar lives at slots 27..35; getEquipped() reads 27 + index.
        int slot = 27 + inv.getIndex();
        Item axe = new Item(player.panel, TOOL_NAME);
        Stack stack = new Stack(player.panel, axe, 1);
        inv.setStack(slot, stack);
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
