package resources.presentation.camera;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import resources.domain.entity.component.AnimationComponent;
import resources.domain.tile.Tile;
import resources.world.Chunk;

/**
 * Per-chunk bake of the static tile layer. The chunk's non-animated tiles are
 * composited once into a single {@link BufferedImage}; subsequent frames blit
 * that one image instead of issuing N×N tile draws.
 *
 * Animated tiles (ocean, shallow water) are deliberately skipped here and
 * drawn per-frame by the regular tile loop — their image stack changes every
 * tick, so caching them would defeat the point.
 *
 * Invalidation is keyed off the chunk: callers that mutate tiles in a chunk
 * (border re-resolve, generator post-process) should call
 * {@link #invalidate(Chunk)} so the bake gets rebuilt on next draw.
 */
public final class ChunkTileCache {

    private final Map<Chunk, BufferedImage> baked = new IdentityHashMap<>();

    /** Get or build the bake for a chunk. Returns null if the chunk has no static tiles. */
    public BufferedImage getOrBuild(Chunk chunk) {
        BufferedImage img = baked.get(chunk);
        if (img != null) return img;
        img = build(chunk);
        if (img != null) baked.put(chunk, img);
        return img;
    }

    public void invalidate(Chunk chunk) {
        baked.remove(chunk);
    }

    public void clear() {
        baked.clear();
    }

    /**
     * Drop bakes for chunks not in {@code live}. Prevents the cache from
     * accumulating entries for chunks the world has since unloaded —
     * IdentityHashMap holds strong refs so without this the bakes leak
     * even after the chunk objects are unreachable elsewhere.
     */
    public void pruneTo(Collection<Chunk> live) {
        Set<Chunk> keep = Collections.newSetFromMap(new IdentityHashMap<>());
        keep.addAll(live);
        for (Iterator<Chunk> it = baked.keySet().iterator(); it.hasNext(); ) {
            if (!keep.contains(it.next())) it.remove();
        }
    }

    private BufferedImage build(Chunk chunk) {
        int w = chunk.width;
        int h = chunk.height;
        if (w <= 0 || h <= 0) return null;

        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        try {
            boolean any = false;
            for (int i = 0; i < chunk.tiles.length; i++) {
                for (int j = 0; j < chunk.tiles[i].length; j++) {
                    Tile t = chunk.tiles[i][j];
                    if (t == null) continue;
                    if (t.hasComponent(AnimationComponent.class)) continue;
                    int tx = (int) t.getWorldX() - chunk.x;
                    int ty = (int) t.getWorldY() - chunk.y;
                    for (java.awt.image.BufferedImage layer : t.getImages()) {
                        g.drawImage(layer, tx, ty, t.getWidth(), t.getHeight(), null);
                    }
                    any = true;
                }
            }
            return any ? out : null;
        } finally {
            g.dispose();
        }
    }
}
