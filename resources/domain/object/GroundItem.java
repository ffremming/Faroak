package resources.domain.object;

import java.awt.image.BufferedImage;
import java.util.ArrayList;

import resources.app.GamePanel;

/**
 * A stack of items lying on the ground, waiting to be picked up. Spawned when a
 * crop/tree/rock is harvested (see {@link resources.domain.inventory.HarvestService});
 * collected when the player walks over it (see
 * {@link resources.domain.player.Playable#tickGroundPickup()}).
 *
 * <p>Non-solid so the player walks through it, and renders the item's existing
 * inventory icon ({@code items/<itemName>.png}) drawn small and centred on a
 * tile so it reads as "loot on the floor" rather than a placed object. No new
 * art is required — the icon is reused from {@link resources.presentation.image.ImageContainer}.
 *
 * <p>Carries an item name + quantity rather than an {@link resources.domain.inventory.Item}
 * so it stays a pure world entity; the pickup site materialises the Item only
 * when it lands in an inventory.
 */
public final class GroundItem extends GameObject {

    /** Rendered footprint of a dropped stack, in px. Smaller than a tile so it
     *  reads as loot. The hitbox matches so pickup triggers on visual overlap. */
    private static final int SIZE = 32;

    private final String itemName;
    private int quantity;

    /** Brief grace period (ticks) before this drop can be picked up, so items
     *  dropped at the player's feet don't vanish the same frame they spawn and
     *  the player can see the drop animation/scatter. */
    private int pickupDelayTicks;

    private static final int DEFAULT_PICKUP_DELAY = 18; // ~0.3s at 60fps

    public GroundItem(GamePanel panel, String itemName, int quantity, int worldX, int worldY) {
        // Centre the small sprite/hitbox on the drop point.
        super(panel, itemName,
              worldX - SIZE / 2, worldY - SIZE / 2,
              SIZE, SIZE,
              SIZE, SIZE,
              0, 0,
              false);
        this.itemName = itemName;
        this.quantity = Math.max(1, quantity);
        this.pickupDelayTicks = DEFAULT_PICKUP_DELAY;
        loadIcon();
    }

    /** Replace the object-sprite stack loaded by {@link GameObject} with the
     *  item's inventory icon. Falls back to the object sprite if no item icon
     *  exists for this name. */
    private void loadIcon() {
        BufferedImage icon = null;
        try {
            icon = panel.images().getItemImage(itemName);
        } catch (RuntimeException e) {
            // fall through to whatever GameObject already loaded
            System.err.println("[GroundItem] loadIcon failed for item '" + itemName + "': " + e);
        }
        if (icon != null) {
            ArrayList<BufferedImage> stack = new ArrayList<>(1);
            stack.add(icon);
            images = stack;
        }
    }

    @Override
    public void update() {
        super.update();
        if (pickupDelayTicks > 0) pickupDelayTicks--;
    }

    /** Override so the stage-stamped {@link GameObject#getImage()} re-fetch
     *  doesn't clobber our item icon. The icon never changes, so just keep the
     *  current stack. */
    @Override
    public BufferedImage getImage() {
        return images.isEmpty() ? null : images.get(0);
    }

    public String getItemName()   { return itemName; }
    public int    getQuantity()   { return quantity; }
    public void   setQuantity(int q) { this.quantity = q; }
    public boolean isPickupReady() { return pickupDelayTicks <= 0; }
}
