package resources.testing.probes;

import java.awt.Point;

import resources.app.GameContext;
import resources.domain.entity.BaseEntity;
import resources.domain.inventory.Inventory;
import resources.domain.inventory.Stack;
import resources.domain.object.GroundItem;
import resources.domain.player.Playable;
import resources.domain.tile.Tile;
import resources.testing.Logger;
import resources.testing.Probe;
import resources.testing.ProbeResult;
import resources.testing.TestHarness;

/**
 * Exercises the ground-item drop/pickup system end-to-end:
 *   1. Re-anchor the player onto a walkable tile (earlier probes may have
 *      shifted them onto water/solid terrain).
 *   2. Spawn a {@link GroundItem} on the player's position via the normal
 *      {@code placeEntity} path and confirm it lands in the world.
 *   3. Tick the simulation; the per-tick walk-over collector in
 *      {@link Playable#update()} should pull the stack into the inventory once
 *      the short pickup grace elapses.
 *
 * Verifies the drop becomes a real, queryable world entity, that it renders via
 * the standard entity path (non-null image), and that overlap-pickup transfers
 * the full quantity into the inventory and removes the ground entity.
 */
public final class GroundItemProbe implements Probe {

    private static final Logger LOG = Logger.forClass(GroundItemProbe.class);

    private static final String DROP_ITEM = "block";
    private static final int    DROP_QTY  = 5;

    @Override public String name() { return "ground-item"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        GameContext ctx = harness.context();
        if (!(ctx.player() instanceof Playable)) return ProbeResult.skip(name() + " no Playable");
        Playable player = (Playable) ctx.player();

        if (!relocateToWalkable(ctx, player)) {
            return ProbeResult.skip(name() + " no walkable tile within scan radius");
        }

        int startCount = countItems(player.getInventory(), DROP_ITEM);

        // Drop the item right on the player so its hitbox overlaps immediately.
        int px = (int) (player.getWorldX() + player.getWidth()  / 2.0);
        int py = (int) (player.getWorldY() + player.getHeight() / 2.0);
        GroundItem drop = new GroundItem(player.panel, DROP_ITEM, DROP_QTY, px, py);
        boolean hasImage = drop.getImage() != null;
        if (!ctx.world().placeEntity(drop)) {
            return ProbeResult.fail(name() + " could not place ground item");
        }
        // Make the freshly-placed entity queryable/renderable.
        ctx.world().update(player.getPoint());
        boolean inWorld = countGroundItems(ctx, DROP_ITEM) > 0;

        // Run past the pickup grace; the walk-over collector runs every tick in
        // Playable.update(), driven by world.simulate() inside panel.update().
        harness.tick(40);

        int endCount    = countItems(player.getInventory(), DROP_ITEM);
        int leftOnFloor = countGroundItems(ctx, DROP_ITEM);

        String detail = String.format(
            "has-image=%s, spawned-in-world=%s, gained=%d (expected %d), left-on-floor=%d",
            hasImage, inWorld, endCount - startCount, DROP_QTY, leftOnFloor);
        LOG.info(detail);

        if (!hasImage)                       return ProbeResult.fail(name() + " ground item has no sprite", detail);
        if (!inWorld)                        return ProbeResult.fail(name() + " drop never entered the world", detail);
        if (endCount - startCount != DROP_QTY) return ProbeResult.fail(name() + " wrong pickup quantity", detail);
        if (leftOnFloor != 0)                return ProbeResult.fail(name() + " drop not removed after pickup", detail);
        return ProbeResult.pass(name(), detail);
    }

    /** Teleport the player onto the first nearby non-solid tile so the drop has
     *  a valid place to spawn and the player can stand on it. */
    private static boolean relocateToWalkable(GameContext ctx, Playable player) {
        int ts = ctx.tileSize();
        int cx = (int) player.getWorldX();
        int cy = (int) player.getWorldY();
        for (int r = 0; r <= 12; r++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dx = -r; dx <= r; dx++) {
                    if (Math.abs(dx) != r && Math.abs(dy) != r) continue; // ring boundary only
                    Point p = new Point(cx + dx * ts, cy + dy * ts);
                    Tile t = ctx.world().getTile(p);
                    if (t == null || t.isSolid()) continue;
                    // position() (not setWorldX/Y) so the hitbox coords follow
                    // the move — pickup overlap is tested against the hitbox.
                    player.position(p);
                    player.resetInteractionHitBox();
                    ctx.world().update(player.getPoint());
                    if (!ctx.world().solidCollision(player.getHitBox(), player)) return true;
                }
            }
        }
        return false;
    }

    private static int countGroundItems(GameContext ctx, String itemName) {
        int total = 0;
        for (BaseEntity e : ctx.world().getEntities()) {
            if (e instanceof GroundItem && itemName.equals(((GroundItem) e).getItemName())) {
                total += ((GroundItem) e).getQuantity();
            }
        }
        return total;
    }

    private static int countItems(Inventory inv, String itemName) {
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
