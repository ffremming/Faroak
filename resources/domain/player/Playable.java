package resources.domain.player;

import java.awt.Point;
import java.util.ArrayList;

import resources.app.GamePanel;
import resources.domain.entity.BaseEntity;
import resources.domain.entity.component.HealthComponent;
import resources.domain.farming.FarmingService;
import resources.domain.inventory.HarvestService;
import resources.domain.inventory.Inventory;
import resources.domain.inventory.Item;
import resources.domain.inventory.Stack;
import resources.domain.object.GameObject;
import resources.geometry.HitBox;

public class Playable extends Moveable {

    private Inventory inventory;
    private Stack tempInHand;
    private final HarvestService harvest = new HarvestService();
    private PlayerLifecycle lifecycle;

    public Playable(GamePanel panel, String name, int worldX, int worldY, short width, short height,
                    short hitBoxWidth, short hitBoxHeight, short relativeXPlus, short relativeYPlus) {
        super(panel, name, worldX, worldY, width, height, hitBoxWidth, hitBoxHeight, relativeXPlus, relativeYPlus);

        inventory = new Inventory(this);
        panel.userInterface.clear();
        panel.userInterface.addInventory(inventory);

        addItem(new Item(panel, "hammer"));
        addItem(new Item(panel, "demoHouse"));
        addItem(new Item(panel, "axe"));
        addItem(new Item(panel, "block"), 300);
        // Phase-4 toolkit + farming starter pack
        addItem(new Item(panel, "hoe"));
        addItem(new Item(panel, "watering_can"));
        addItem(new Item(panel, "shovel"));
        addItem(new Item(panel, "sword"));
        addItem(new Item(panel, "pickaxe"));
        addItem(new Item(panel, "torch"), 10);
        addItem(new Item(panel, "fence"), 32);
        addItem(new Item(panel, "barrel"), 4);
        addItem(new Item(panel, "boat"), 3);
        addItem(new Item(panel, "seeds_wheat"), 16);
        addItem(new Item(panel, "seeds_carrot"), 16);

        // Health + spawn tracking. Spawn point captured here so respawn returns
        // the player to where they entered the world.
        addComponent(new HealthComponent(PlayerLifecycle.DEFAULT_MAX_HEALTH));
        this.lifecycle = new PlayerLifecycle(panel, new Point(worldX, worldY));
    }

    /** Hit-points / spawn / respawn manager. Never null once the player is built. */
    public PlayerLifecycle lifecycle() { return lifecycle; }

    @Override
    public void update() {
        // While dead, freeze movement and pause the placement preview so the
        // world stops responding until the player respawns via the ESC menu.
        if (lifecycle != null && lifecycle.isDead()) return;
        super.update();
        // Suppress the placement ghost whenever the inventory or a menu is
        // open. Without this, the preview tracks the cursor under the UI and
        // looks like a stuck object on screen.
        boolean uiOpen = panel.userInterface != null && panel.userInterface.isEnabled();
        if (uiOpen) {
            panel.world.addObjectPreview(null);
        } else {
            panel.world.addObjectPreview(getEquipped());
        }
    }

    /**
     * Try interacting with world objects within the interaction hitbox.
     */
    public void interact() {
        if (lifecycle != null && lifecycle.isDead()) return;
        // Tool-aware fast paths first; if no handler claims the action, fall back
        // to generic GameObject.interact() so portals, barrels, etc., still work.
        if (FarmingService.tryHoeTile(this, panel))           return;
        if (FarmingService.tryPlantOnFarmland(this, panel))   return;

        HitBox reach = getInteractionHitBox();
        // Snapshot first — interact() can swap the world (portals fire a
        // DimensionChangeEvent that replaces the entity list).
        ArrayList<BaseEntity> snapshot = new ArrayList<>(panel.world.getEntities());
        // Sort candidates by distance from the player's center so the nearest
        // object wins instead of whichever the chunk happened to add first.
        double pcx = getWorldX() + getWidth()  / 2.0;
        double pcy = getWorldY() + getHeight() / 2.0;
        snapshot.removeIf(e -> !(e instanceof GameObject)
                            || !e.getHitBox().intersects(reach));
        snapshot.sort((a, b) -> {
            double da = sqDist(a, pcx, pcy);
            double db = sqDist(b, pcx, pcy);
            return Double.compare(da, db);
        });
        for (BaseEntity ent : snapshot) {
            ((GameObject) ent).interact(this);
            return;
        }
    }

    private static double sqDist(BaseEntity e, double pcx, double pcy) {
        double ex = e.getWorldX() + e.getWidth()  / 2.0;
        double ey = e.getWorldY() + e.getHeight() / 2.0;
        double dx = ex - pcx;
        double dy = ey - pcy;
        return dx * dx + dy * dy;
    }

    /**
     * Swing the equipped tool at whatever harvestable sits inside the
     * interaction box.
     */
    public void attack() {
        if (lifecycle != null && lifecycle.isDead()) return;
        harvest.attack(this, panel);
    }

    public void nullPath() {
        path.clear();
    }

    public void addItem(Item item) {
        inventory.addItem(item);
    }

    public void addItem(Item item, int amount) {
        inventory.addStack(new Stack(panel, item, amount));
    }

    public void addStack(Stack stack) {
        inventory.addStack(stack);
    }

    public Item getItem() {
        Stack stack = getEquipped();
        return (stack == null || stack.isEmpty()) ? null : stack.getItem();
    }

    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    /** Currently-selected hotbar stack. Recomputed each call; there is no
     *  cached field to keep in sync. */
    public Stack getEquipped() {
        return inventory.getHotbarStack(inventory.getIndex());
    }

    public Stack getTempInHand() {
        return tempInHand;
    }

    public void setTempInHand(Stack tempInHand) {
        this.tempInHand = tempInHand;
    }
}
