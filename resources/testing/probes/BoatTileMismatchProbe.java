package resources.testing.probes;

import javax.swing.JFrame;

import resources.app.GamePanel;
import resources.domain.object.Boat;
import resources.domain.tile.Tile;
import resources.world.placement.TileRules;

/**
 * Diagnostic: does the boat's collision belief (Boat.canEnter via world().getTile) agree
 * with the VISUAL tiles, online (host) vs the same checks offline? Scans a grid of water
 * tiles around an armed sloop and reports, per tile:
 *   - visual: the rendered tile name (and isWater)
 *   - getTile-at-center returns null? (would block as "hidden restriction")
 * Also measures how big the boat's collision hitbox footprint is vs its sprite, since a
 * hitbox larger than the visible hull makes corners sample neighbouring land tiles.
 *
 * Run: java -cp out resources.testing.probes.BoatTileMismatchProbe   (offline)
 *      java -Dgame.multiplayer.mode=host -Dgame.multiplayer.backend=loopback -cp out resources.testing.probes.BoatTileMismatchProbe   (online host)
 */
public final class BoatTileMismatchProbe {

    public static void main(String[] a) throws Exception {
        boolean online = "host".equalsIgnoreCase(System.getProperty("game.multiplayer.mode", ""));
        GamePanel panel = new GamePanel(new JFrame(), true);
        for (int i = 0; i < (online ? 120 : 30); i++) { panel.update(1.0); Thread.sleep(1); }

        int px = (int) panel.player().getWorldX(), py = (int) panel.player().getWorldY();
        int[] w = nearestWater(panel, px, py);
        if (w == null) { System.out.println("[Mismatch] online=" + online + " no water found near host"); panel.stopGameThread(); System.exit(0); }

        Boat sloop = new Boat(panel, resources.domain.ship.ShipKindRegistry.PLAYER_SLOOP, w[0], w[1], false);
        System.out.println("[Mismatch] online=" + online
            + " sprite=" + sloop.getWidth() + "x" + sloop.getHeight()
            + " hitbox=" + sloop.kind().hitboxWidth() + "x" + sloop.kind().hitboxHeight()
            + " rel=(" + sloop.kind().hitboxRelX() + "," + sloop.kind().hitboxRelY() + ")");

        // Walk a grid of tiles around the water spot; report visual-water tiles where
        // getTile returns null (would be an invisible block) and count water tiles.
        int waterTiles = 0, nullTiles = 0, sampled = 0;
        int ts = 64;
        for (int dy = -8; dy <= 8; dy++) {
            for (int dx = -8; dx <= 8; dx++) {
                int x = w[0] + dx * ts, y = w[1] + dy * ts;
                Tile t = panel.world().getTile(new java.awt.Point(x, y));
                sampled++;
                if (t == null) { nullTiles++; continue; }
                if (TileRules.isWater(t.getName())) waterTiles++;
            }
        }
        System.out.println("[Mismatch] sampled=" + sampled + " water=" + waterTiles
            + " nullTiles(invisible-block)=" + nullTiles);

        // For a water spot, can the sloop be placed there (all 4 hitbox corners water)?
        boolean placed = panel.world().placeShipOnWater(sloop);
        System.out.println("[Mismatch] placeShipOnWater at nearest water=" + placed
            + (placed ? "" : "  <-- visually water but boat placement rejected"));

        panel.stopGameThread();
        System.exit(0);
    }

    private static int[] nearestWater(GamePanel panel, int px, int py) {
        int ts = 64;
        for (int r = 1; r <= 40; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -r; dy <= r; dy++) {
                    if (Math.max(Math.abs(dx), Math.abs(dy)) != r) continue;
                    int x = px + dx * ts, y = py + dy * ts;
                    Tile t = panel.world().getTile(new java.awt.Point(x, y));
                    if (t != null && TileRules.isWater(t.getName())) return new int[] { x, y };
                }
            }
        }
        return null;
    }
}
