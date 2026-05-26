package resources.domain.player;

import resources.app.GamePanel;
import resources.domain.entity.BaseEntity;
import resources.domain.inventory.HarvestService;
import resources.domain.inventory.Inventory;
import resources.domain.inventory.Item;
import resources.domain.inventory.Stack;
import resources.domain.object.GameObject;

public class Playable extends Moveable {

    private Inventory inventory;
    private Stack equipped;
    private Stack tempInHand;
    private final HarvestService harvest = new HarvestService();

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
        equipped = inventory.getStack(inventory.getIndex());
    }

    @Override
    public void update() {
        super.update();
        panel.world.addObjectPreview(getEquipped());
    }

    /**
     * Try interacting with world objects within the interaction hitbox.
     */
    public void interact() {
        for (BaseEntity ent : panel.world.getEntities()) {
            if (!(ent instanceof GameObject)) continue;
            GameObject object = (GameObject) ent;
            if (object.getHitBox().intersects(getInteractionHitBox())) object.interact(this);
        }
    }

    /**
     * Swing the equipped tool at whatever harvestable sits inside the
     * interaction box.
     */
    public void attack() {
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
        return equipped == null || equipped.isEmpty() ? null : equipped.getItem();
    }

    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public Stack getEquipped() {
        Stack selected = inventory.getStack(27 + inventory.getIndex());
        equipped = selected;
        return selected;
    }

    public void setEquipped(Stack equipped) {
        this.equipped = equipped;
    }

    public Stack getTempInHand() {
        return tempInHand;
    }

    public void setTempInHand(Stack tempInHand) {
        this.tempInHand = tempInHand;
    }
}
