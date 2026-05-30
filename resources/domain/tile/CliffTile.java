package resources.domain.tile;

import java.awt.image.BufferedImage;
import java.util.ArrayList;

import resources.app.GamePanel;
import resources.domain.tile.connect.ConnectionBitmask;
import resources.domain.tile.connect.ConnectionRule;

/**
 * Tile variant that picks a cliff-edge sprite based on which neighbours are
 * also cliff tiles. Image stack = base tile + connecting overlay from
 * {@link CliffConnectingSprite}.
 *
 * Connection policy and sprite-variant lookup live in the {@code connect}
 * package so the same machinery serves fences, walls, bushes, etc.
 */
public class CliffTile extends Tile {

    private static final ConnectionRule CLIFF_RULE = (self, n) -> n instanceof CliffTile;

    public CliffTile(GamePanel panel, String name, int worldX, int worldY, int altitude) {
        super(panel, name, worldX, worldY, altitude);
    }

    @Override
    public ArrayList<BufferedImage> getImages() {
        if (images.isEmpty()) populateImages();
        return images;
    }

    private void populateImages() {
        images.clear();
        images.add(getImage());
        ArrayList<String> keys = new ArrayList<>(1);
        int mask = ConnectionBitmask.cardinal(this, getNeighbors(), CLIFF_RULE);
        CliffConnectingSprite.INSTANCE.appendLayers(mask, keys);
        for (String key : keys) images.add(panel.images().getTileImage(key));
    }
}
