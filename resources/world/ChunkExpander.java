package resources.world;

import java.awt.Rectangle;

/**
 * Grows the chunk quadtree when the render rectangle leaves the current root's
 * bounds. The expansion strategy chooses one of four quadrant placements
 * (SE / SW / NE / NW) that contains the rectangle and wraps the old root as a
 * child of the new one.
 *
 * Extracted from ChunkSystem so the expansion policy can evolve (different
 * growth ratios, asymmetric expansion, dimension-specific bounds) without
 * tangling with the rest of chunk management.
 */
public final class ChunkExpander {

    private final ChunkSystem system;

    public ChunkExpander(ChunkSystem system) {
        this.system = system;
    }

    /** Re-root the quadtree so it contains the given rectangle. */
    public void expand(Rectangle rect) throws OutOfChunkBounds {
        TreeNode parent = system.parent;
        Rectangle[] quads = quadrants(parent);

        for (Rectangle q : quads) {
            if (q.contains(rect)) { rebaseTo(q); return; }
        }
        throw new OutOfChunkBounds("unable to create new chunks with the right constraints");
    }

    private void rebaseTo(Rectangle newBounds) {
        system.parent = new TreeNode(system,
            newBounds.x, newBounds.y,
            newBounds.width, newBounds.height,
            system.parent);
    }

    private Rectangle[] quadrants(TreeNode parent) {
        return new Rectangle[]{
            new Rectangle(parent.x,                parent.y,                parent.width * 2, parent.height * 2), // SE
            new Rectangle(parent.x - parent.width, parent.y,                parent.width * 2, parent.height * 2), // SW
            new Rectangle(parent.x,                parent.y - parent.height,parent.width * 2, parent.height * 2), // NE
            new Rectangle(parent.x - parent.width, parent.y - parent.height,parent.width * 2, parent.height * 2)  // NW
        };
    }
}
