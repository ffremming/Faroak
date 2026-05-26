package resources.world;

import resources.app.GamePanel;
import resources.domain.entity.BaseEntity;
import resources.domain.entity.Entity;
import resources.domain.tile.Tile;
import resources.domain.tile.CliffTile;
import resources.geometry.HitBox;
import resources.geometry.Vector;
import resources.generation.factory.EntityFactory;
import resources.domain.object.GameObject;
import resources.domain.player.Moveable;
import resources.domain.inventory.Item;
import resources.domain.inventory.Stack;
import resources.presentation.camera.Camera;

import java.util.ArrayList;
import java.util.Collections;

import resources.domain.entity.Entity;

/**
 * Y-axis painter's-algorithm sort for entities. Hybrid quicksort that drops to insertion
 * sort for small partitions. Kept as a stateless utility so the sorting policy lives in
 * one place instead of bleeding into WorkingMemory.
 */
public final class EntitySorter {

    private static final int INSERTION_SORT_CUTOFF = 10;

    private EntitySorter() {}

    /** Sort the whole list in place by entity hitbox worldY ascending. */
    public static void sortByWorldY(ArrayList<Entity> list) {
        if (list == null || list.size() < 2) return;
        quicksort(list, 0, list.size() - 1);
    }

    private static void quicksort(ArrayList<Entity> list, int low, int high) {
        if (low >= high) return;
        if (high - low + 1 <= INSERTION_SORT_CUTOFF) {
            insertionSort(list, low, high);
        } else {
            int pivotIndex = partition(list, low, high);
            quicksort(list, low, pivotIndex - 1);
            quicksort(list, pivotIndex + 1, high);
        }
    }

    /**
     * Sort key is hitbox BOTTOM (y + height). Painter's algorithm wants the
     * lower-on-screen entity drawn last so it correctly overlaps anything
     * standing behind it. Sorting by top-y caused moving entities (whose
     * hitboxes are typically smaller than the sprite) to render behind objects
     * they were visually in front of.
     */
    private static double sortKey(Entity e) {
        return e.getHitBox().getWorldY() + e.getHitBox().height;
    }

    private static int partition(ArrayList<Entity> list, int low, int high) {
        double pivotY = sortKey(list.get(high));
        int i = low - 1;
        for (int j = low; j < high; j++) {
            if (sortKey(list.get(j)) < pivotY) {
                i++;
                Collections.swap(list, i, j);
            }
        }
        Collections.swap(list, i + 1, high);
        return i + 1;
    }

    private static void insertionSort(ArrayList<Entity> list, int low, int high) {
        for (int i = low + 1; i <= high; i++) {
            Entity key = list.get(i);
            double keyY = sortKey(key);
            int j = i - 1;
            while (j >= low && sortKey(list.get(j)) > keyY) {
                list.set(j + 1, list.get(j));
                j--;
            }
            list.set(j + 1, key);
        }
    }
}
