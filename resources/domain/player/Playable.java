package resources.domain.player;

import java.awt.Point;
import java.util.ArrayList;

import resources.app.GamePanel;
import resources.domain.combat.CombatService;
import resources.domain.combat.MeleeAttackSpec;
import resources.domain.combat.WeaponProfile;
import resources.domain.entity.BaseEntity;
import resources.domain.entity.component.HealthComponent;
import resources.domain.inventory.HarvestService;
import resources.domain.inventory.Inventory;
import resources.domain.inventory.Item;
import resources.domain.inventory.Stack;
import resources.domain.object.GameObject;
import resources.domain.object.GroundItem;
import resources.geometry.HitBox;
import resources.geometry.Vector;

public class Playable extends Moveable {

    private Inventory inventory;
    private Stack tempInHand;
    private final HarvestService harvest = new HarvestService();
    private final CombatService combat = new CombatService();
    private PlayerLifecycle lifecycle;

    private static final int COMBO_WINDOW_TICKS = 14;
    private static final int INPUT_BUFFER_TICKS = 4;

    /** Margin (px) added around the player's sprite bounds when testing for
     *  walk-over ground-item pickups. Keeps collection forgiving. */
    private static final int PICKUP_MARGIN_PX = 8;

    private int lightCooldownTicks;
    private int heavyCooldownTicks;
    private int rangedCooldownTicks;
    private int comboWindowTicks;
    private int comboStep;
    private int bufferedLightTicks;

    public Playable(GamePanel panel, String name, int worldX, int worldY, short width, short height,
                    short hitBoxWidth, short hitBoxHeight, short relativeXPlus, short relativeYPlus) {
        this(panel, name, worldX, worldY, width, height, hitBoxWidth, hitBoxHeight,
            relativeXPlus, relativeYPlus, true);
    }

    /**
     * @param local true for THE local player (binds its inventory to the on-screen UI);
     *              false for a remote/headless player (host-authoritative replica of a
     *              connected guest) — it still has an inventory, items, health and
     *              lifecycle, but must NOT seize the local UI. Used by the
     *              host-authoritative multiplayer lobby to give each guest a real engine
     *              actor without corrupting the host's screen.
     */
    public Playable(GamePanel panel, String name, int worldX, int worldY, short width, short height,
                    short hitBoxWidth, short hitBoxHeight, short relativeXPlus, short relativeYPlus,
                    boolean local) {
        super(panel, name, worldX, worldY, width, height, hitBoxWidth, hitBoxHeight, relativeXPlus, relativeYPlus);

        inventory = new Inventory(this);
        if (local) {
            panel.userInterface().clear();
            panel.userInterface().addInventory(inventory);
        }

        // All equipment (tools/weapons)
        addItem(new Item(panel, "hammer"));
        addItem(new Item(panel, "axe"));
        addItem(new Item(panel, "hoe"));
        addItem(new Item(panel, "watering_can"));
        addItem(new Item(panel, "shovel"));
        addItem(new Item(panel, "sword"));
        addItem(new Item(panel, "pickaxe"));
        // Fences — full stack
        addItem(new Item(panel, "fence"), 64);
        // Stone walls — full stack (autotiling, same as fence)
        addItem(new Item(panel, "stone_wall"), 64);
        // Misc starter pack
        addItem(new Item(panel, "demoHouse"));
        addItem(new Item(panel, "block"), 300);
        addItem(new Item(panel, "torch"), 10);
        addItem(new Item(panel, "barrel"), 4);
        addItem(new Item(panel, "boat"), 3);
        addItem(new Item(panel, "seeds_wheat"), 16);
        addItem(new Item(panel, "seeds_carrot"), 16);
        // Fantasy crop seeds (see FARM_CROPS.md)
        addItem(new Item(panel, "seeds_emberwheat"), 16);
        addItem(new Item(panel, "seeds_glowcap"), 16);
        addItem(new Item(panel, "seeds_manaberry"), 16);
        addItem(new Item(panel, "seeds_sungourd"), 16);
        seedHotbarForTests();

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
        tickCombatTimers();
        super.update();
        tickGroundPickup();
        // Suppress the placement ghost whenever the inventory or a menu is
        // open. Without this, the preview tracks the cursor under the UI and
        // looks like a stuck object on screen.
        boolean uiOpen = panel.userInterface() != null && panel.userInterface().isEnabled();
        if (uiOpen) {
            panel.world.addObjectPreview(null);
        } else {
            panel.world.addObjectPreview(getEquipped());
        }
    }

    private void tickCombatTimers() {
        if (lightCooldownTicks > 0) lightCooldownTicks--;
        if (heavyCooldownTicks > 0) heavyCooldownTicks--;
        if (rangedCooldownTicks > 0) rangedCooldownTicks--;
        if (comboWindowTicks > 0) comboWindowTicks--;
        if (bufferedLightTicks > 0) bufferedLightTicks--;

        if (bufferedLightTicks > 0 && lightCooldownTicks == 0) {
            bufferedLightTicks = 0;
            performLightAttack(true);
        }
    }

    /**
     * Walk-over loot collection. Each tick, any {@link GroundItem} whose hitbox
     * overlaps the player's is poured into the inventory. We add only what fits:
     * {@link Inventory#addItem(Item, int)} returns the leftover quantity, so a
     * partially-collected stack stays on the ground with its count reduced, and
     * a fully-collected one is queued for removal.
     *
     * Iterates a snapshot because picking up mutates the world's entity list
     * (via the removal queue) and {@code getEntitiesCollidedWith} returns a
     * live-derived collection.
     */
    private void tickGroundPickup() {
        // Test against a pickup region covering the player's whole sprite (plus
        // a small margin) rather than the feet-only collision hitbox, so a drop
        // is collected as soon as the player visibly walks onto it. The feet
        // box sits low on the tall sprite and would miss drops the player is
        // clearly standing over.
        HitBox pickupArea = getImageHitbox().getAlteredHitBox(
            PICKUP_MARGIN_PX, PICKUP_MARGIN_PX, PICKUP_MARGIN_PX, PICKUP_MARGIN_PX);
        ArrayList<BaseEntity> overlapping =
            new ArrayList<>(panel.world().getEntitiesCollidedWith(pickupArea));
        for (BaseEntity e : overlapping) {
            if (!(e instanceof GroundItem)) continue;
            GroundItem drop = (GroundItem) e;
            if (!drop.isPickupReady()) continue;

            int leftover = inventory.addItem(new Item(panel, drop.getItemName()),
                                             drop.getQuantity());
            if (leftover <= 0) {
                panel.world().addToRemovalQueue(drop);
            } else if (leftover < drop.getQuantity()) {
                // Took some but not all (inventory filled up) — keep the rest
                // on the ground reflecting what actually remains.
                drop.setQuantity(leftover);
            }
            // leftover == quantity → inventory full, nothing taken; leave as-is.
        }
    }

    /**
     * Try interacting with world objects within the interaction hitbox.
     */
    public void interact() {
        if (lifecycle != null && lifecycle.isDead()) return;
        // Spacebar handles only NON-targeted interactions: open/use the nearest
        // GameObject in reach (chest, crafting table, barrel, portal, ...).
        // All cursor-targeted actions (hoe, plant, place, harvest) are driven by
        // the mouse click chain instead.
        HitBox reach = getInteractionHitBox();
        // Snapshot first — interact() can swap the world (portals fire a
        // DimensionChangeEvent that replaces the entity list).
        ArrayList<BaseEntity> snapshot = new ArrayList<>(panel.world().getEntities());
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
     * Legacy attack entry-point used by probes. Performs an immediate melee
     * strike and falls back to harvest logic if no health target was hit.
     */
    public void attack() {
        if (lifecycle != null && lifecycle.isDead()) return;
        BaseEntity harvested = harvest.attack(this, panel);
        if (harvested == null) performLightAttack(false);
    }

    /** Keyboard: fast combo-able short-range strike. */
    public void requestLightAttack() {
        if (lifecycle != null && lifecycle.isDead()) return;
        performLightAttack(true);
    }

    /** Keyboard: slower high-damage close strike. */
    public void requestHeavyAttack() {
        if (lifecycle != null && lifecycle.isDead()) return;
        if (heavyCooldownTicks > 0) return;
        WeaponProfile weapon = equippedWeaponProfile();
        Vector aim = panel.input().combatAimVector();
        int hits = combat.meleeAttack(this, panel, aim, new MeleeAttackSpec(
            weapon.heavyDamage,
            weapon.heavyRangePx,
            weapon.heavyArcDegrees,
            3,
            weapon.swingSpriteName,
            weapon.swingDurationTicks + 2,
            weapon.swingArcDegrees + 10.0,
            weapon.swingRadiusPx));
        if (hits == 0) harvest.attack(this, panel);
        heavyCooldownTicks = weapon.heavyCooldownTicks;
        lightCooldownTicks = Math.max(lightCooldownTicks, weapon.lightCooldownTicks / 2);
        comboWindowTicks = 0;
        comboStep = 0;
    }

    /** Keyboard: projectile attack. */
    public void requestRangedAttack() {
        if (lifecycle != null && lifecycle.isDead()) return;
        if (rangedCooldownTicks > 0) return;
        WeaponProfile weapon = equippedWeaponProfile();
        Vector aim = panel.input().combatAimVector();
        boolean fired = combat.fireProjectile(this, panel, aim,
            weapon.rangedDamage,
            weapon.projectileSpeedPxPerTick,
            weapon.projectileLifeTicks,
            weapon.projectileSpriteName);
        if (!fired) return;
        rangedCooldownTicks = weapon.rangedCooldownTicks;
    }

    private int performLightAttack(boolean obeyCooldown) {
        WeaponProfile weapon = equippedWeaponProfile();
        if (obeyCooldown && lightCooldownTicks > 0) {
            if (lightCooldownTicks <= INPUT_BUFFER_TICKS) {
                bufferedLightTicks = INPUT_BUFFER_TICKS;
            }
            return 0;
        }

        comboStep = (comboWindowTicks > 0) ? Math.min(3, comboStep + 1) : 1;
        comboWindowTicks = COMBO_WINDOW_TICKS;

        Vector aim = panel.input().combatAimVector();
        int hits = combat.meleeAttack(this, panel, aim, new MeleeAttackSpec(
            weapon.comboDamage(comboStep),
            weapon.comboRange(comboStep),
            weapon.comboArc(comboStep),
            3,
            weapon.swingSpriteName,
            weapon.swingDurationTicks,
            weapon.swingArcDegrees + (comboStep - 1) * 8.0,
            weapon.swingRadiusPx));

        if (hits == 0) harvest.attack(this, panel);
        lightCooldownTicks = weapon.lightCooldownTicks;
        return hits;
    }

    private WeaponProfile equippedWeaponProfile() {
        Stack eq = getEquipped();
        String itemName = (eq == null || eq.isEmpty()) ? null : eq.getName();
        return WeaponProfile.forItem(itemName);
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

    /** Exposed so external callers (e.g. mouse-driven harvest in
     *  {@link resources.world.WorldInteraction#tryHarvestAtMouse}) can route
     *  through this player's own service instance, keeping RNG and any
     *  future per-player tuning consistent with the F-key path. */
    public HarvestService harvestService() {
        return harvest;
    }

    /** Exposed so {@link HarvestService} can reuse the same swing/impact VFX
     *  as melee combat when a harvest hit lands. */
    public CombatService combat() {
        return combat;
    }

    public Stack getTempInHand() {
        return tempInHand;
    }

    public void setTempInHand(Stack tempInHand) {
        this.tempInHand = tempInHand;
    }

    private void seedHotbarForTests() {
        String testItem = System.getProperty("game.test.hotbarItem", "").trim();
        if (testItem.isBlank() || "empty".equalsIgnoreCase(testItem)) return;
        if (!"true".equalsIgnoreCase(System.getProperty("game.test.autostart", "false"))) return;
        Stack slot = inventory.getHotbarStack(0);
        if (slot != null && !slot.isEmpty()) return;
        inventory.setStack(Inventory.HOTBAR_OFFSET, new Stack(panel, new Item(panel, testItem), 64));
        inventory.setIndex(0);
    }
}
