package resources.testing.probes;

import java.awt.image.BufferedImage;

import resources.app.GameContext;
import resources.domain.inventory.Inventory;
import resources.domain.inventory.Item;
import resources.domain.inventory.ItemType;
import resources.domain.inventory.ItemTypeRegistry;
import resources.domain.inventory.Stack;
import resources.domain.player.Playable;
import resources.testing.Probe;
import resources.testing.ProbeResult;
import resources.testing.TestHarness;

/**
 * Verifies the creative catalog's data layer: every registered ItemType resolves
 * an icon, and giving an item lands it in the player's inventory. Also confirms
 * the sliced misc objects actually made it into the registry.
 */
public final class CatalogProbe implements Probe {

    @Override public String name() { return "catalog"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        GameContext ctx = harness.context();
        if (!(ctx.player() instanceof Playable)) return ProbeResult.skip(name() + " no Playable");
        Playable player = (Playable) ctx.player();

        int missingIcons = 0, total = 0;
        boolean sawSliced = false;
        ItemType sample = null;
        for (ItemType t : ItemTypeRegistry.instance().values()) {
            total++;
            BufferedImage img = ctx.images().getItemImage(t.spriteName());
            if (img == null) missingIcons++;
            if ("furnace".equals(t.spriteName()) || "anvil".equals(t.spriteName())) sawSliced = true;
            if (sample == null) sample = t;
        }
        if (sample == null) return ProbeResult.fail(name() + " registry empty");

        int before = countItems(player.getInventory(), sample.spriteName());
        player.addItem(new Item(player.panel, sample.spriteName()), 3);
        int after = countItems(player.getInventory(), sample.spriteName());

        String detail = "registered=" + total + ", missingIcons=" + missingIcons
            + ", sawSlicedObjects=" + sawSliced
            + ", gave=" + sample.spriteName() + " +" + (after - before);

        if (!sawSliced)               return ProbeResult.fail(name() + " sliced objects not registered", detail);
        if (missingIcons > 0)         return ProbeResult.fail(name() + " items without icons", detail);
        if (after - before != 3)      return ProbeResult.fail(name() + " give did not add 3", detail);
        return ProbeResult.pass(name(), detail);
    }

    private static int countItems(Inventory inv, String name) {
        int total = 0;
        for (int i = 0; i < inv.getSize(); i++) {
            Stack s = inv.getStack(i);
            if (s != null && !s.isEmpty() && name.equals(s.getName())) total += s.getAmount();
        }
        return total;
    }
}
