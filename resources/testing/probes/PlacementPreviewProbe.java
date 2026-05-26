package resources.testing.probes;

import java.lang.reflect.Field;

import resources.app.GamePanel;
import resources.domain.entity.BaseEntity;
import resources.domain.inventory.Inventory;
import resources.domain.inventory.Item;
import resources.domain.inventory.Stack;
import resources.domain.object.GameObject;
import resources.domain.player.Playable;
import resources.geometry.HitBox;
import resources.input.Mouse;
import resources.testing.Logger;
import resources.testing.Probe;
import resources.testing.ProbeResult;
import resources.testing.TestHarness;

/**
 * Verifies the placement-preview validity flag flips correctly:
 *   - aiming at a free cell sets {@code previewValid == true},
 *   - aiming at a known-solid object sets {@code previewValid == false}.
 *
 * Drives the system by setting the {@code Mouse} world-pixel position via
 * reflection (the underlying {@code int x, int y} fields on {@code Mouse} are
 * package-private) and calling {@code panel.world.addObjectPreview}. To
 * guarantee the "invalid" case lands on a solid, the probe spawns a 64x64
 * solid {@code GameObject} at a known location and aims at it.
 */
public final class PlacementPreviewProbe implements Probe {

    private static final Logger LOG = Logger.forClass(PlacementPreviewProbe.class);

    private static final String PLACEABLE_ITEM = "block";

    @Override public String name() { return "placement-preview"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        GamePanel panel = harness.panel();
        if (!(panel.player instanceof Playable)) return ProbeResult.skip(name() + " no Playable");
        Playable player = panel.player;
        if (panel.camera == null) return ProbeResult.skip(name() + " no camera");

        equipPlaceable(player);

        // --- valid spot: a free tile far enough away that natural solids are
        // unlikely. Scan a ring around the player and pick the first cell whose
        // hitbox doesn't collide with anything solid. ---
        int[] freeCell = findFreeCell(panel, player, null);
        if (freeCell == null) return ProbeResult.skip(name() + " no free cell within scan radius");

        try {
            setMouseScreenXY(panel.mouse, freeCell[0], freeCell[1], panel);
        } catch (ReflectiveOperationException ex) {
            return ProbeResult.fail(name() + " mouse reflection failed", ex.toString());
        }
        panel.world.addObjectPreview(player.getEquipped());
        boolean validFree = panel.camera.isPreviewValid();

        // --- invalid spot: spawn a solid object at another free cell. ---
        int[] anchorCell = findFreeCell(panel, player, freeCell);
        if (anchorCell == null) return ProbeResult.skip(name() + " no second free cell for anchor");
        int solidX = anchorCell[0];
        int solidY = anchorCell[1];
        GameObject anchor = new GameObject(panel, "block",
            solidX, solidY, 64, 64, 64, 64, 0, 0, true);
        if (!panel.world.placeEntity(anchor)) {
            return ProbeResult.skip(name() + " could not place solid anchor");
        }
        panel.world.update(player.getPoint()); // make anchor visible to solid scans
        try {
            // The block preview's hitbox sits 64px below its visual centre, so
            // aim the visual centre 32px above the anchor — that puts the
            // hitbox right on top of the anchor.
            setMouseScreenXY(panel.mouse, solidX + 32, solidY - 32, panel);
        } catch (ReflectiveOperationException ex) {
            panel.world.removeEntity(anchor);
            return ProbeResult.fail(name() + " mouse reflection failed", ex.toString());
        }
        panel.world.addObjectPreview(player.getEquipped());
        boolean validOnSolid = panel.camera.isPreviewValid();

        panel.world.removeEntity(anchor);

        String detail = String.format("free-cell=(%d,%d), free-valid=%s, on-solid-valid=%s",
            freeCell[0], freeCell[1], validFree, validOnSolid);
        LOG.info(detail);

        if (!validFree)        return ProbeResult.fail(name() + " free spot reported invalid", detail);
        if (validOnSolid)      return ProbeResult.fail(name() + " overlap with solid reported valid", detail);
        return ProbeResult.pass(name(), detail);
    }

    private static int[] findFreeCell(GamePanel panel, Playable player, int[] exclude) {
        int cx = (int) player.getWorldX();
        int cy = (int) player.getWorldY();
        // Scan rings outward; for each candidate, check the 64x64 hitbox the
        // preview would occupy.
        for (int r = 2; r <= 8; r++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dx = -r; dx <= r; dx++) {
                    if (Math.abs(dx) != r && Math.abs(dy) != r) continue;
                    int px = cx + dx * 64;
                    int py = cy + dy * 64;
                    if (exclude != null && exclude[0] == px && exclude[1] == py) continue;
                    // The "block" placeable's hitbox sits 64px below its visual
                    // origin (relativeYPlus = 64). The preview centres its
                    // *visual* on the mouse, so the hitbox lives at
                    // (mouseX - 32, mouseY - 32 + 64, 64, 64).
                    HitBox hb = new HitBox(px - 32, py - 32 + 64, 64, 64);
                    boolean blocked = false;
                    if (panel.world.solidCollision(hb)) blocked = true;
                    if (!blocked) {
                        for (BaseEntity ent : panel.world.getEntitiesCollidedWith(hb)) {
                            if (ent.isSolid()) { blocked = true; break; }
                        }
                    }
                    if (!blocked) return new int[]{px, py};
                }
            }
        }
        return null;
    }

    private static void equipPlaceable(Playable player) {
        Inventory inv = player.getInventory();
        int slot = 27 + inv.getIndex();
        Item item = new Item(player.panel, PLACEABLE_ITEM);
        inv.setStack(slot, new Stack(player.panel, item, 1));
    }

    /** Reflective writes to the package-private {@code int x, int y} on Mouse. */
    private static void setMouseScreenXY(Mouse mouse, int worldX, int worldY, GamePanel panel)
            throws ReflectiveOperationException {
        int sx = worldX - (int) panel.camera.getWorldX();
        int sy = worldY - (int) panel.camera.getWorldY();
        Field fx = Mouse.class.getDeclaredField("x");
        Field fy = Mouse.class.getDeclaredField("y");
        fx.setAccessible(true);
        fy.setAccessible(true);
        fx.setInt(mouse, sx);
        fy.setInt(mouse, sy);
    }
}
