package resources.input;

import resources.app.GamePanel;
import resources.domain.entity.BaseEntity;
import resources.domain.inventory.Stack;
import resources.domain.object.Boat;
import resources.world.placement.PlacementRegistry;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;


public class Mouse implements MouseListener, MouseMotionListener,MouseWheelListener  {

    /** Minimum wall-clock gap between two placement actions (ms). Prevents
     *  rapid-fire clicks (and any AWT double-press quirks) from dropping
     *  multiple entities at the cursor. */
    private static final long PLACE_COOLDOWN_MS        = 150L;
    /** Shorter throttle in multiplayer: the local click is optimistic and the
     *  server may reject it, so we don't want to lock the player out of
     *  legitimate retries. */
    private static final long PLACE_COOLDOWN_MS_ONLINE = 60L;

    int x = 0;
    int y = 0;
    GamePanel panel;
    private long lastPlaceMs;

    public Mouse(GamePanel panel){
        this.panel = panel;
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        // Wheel cycles the hotbar — but only when no modal UI is taking focus.
        // Cycling through the hotbar while the player is browsing the full
        // inventory grid was visually confusing and the UI also wanted the
        // wheel event for its own scrolling.
        if (panel.userInterface != null && panel.userInterface.isModalUIOpen()) {
            panel.userInterface.mouseWheelMoved(e);
            return;
        }
        if (e.getWheelRotation() < 0) {
            panel.player.getInventory().decreaseIndex();
        } else if (e.getWheelRotation() > 0) {
            panel.player.getInventory().increseIndex();
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        panel.userInterface.mouseDragged(e);
       
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        this.x = e.getX();
        this.y = e.getY();
        panel.UI.mouseMoved(e);
        panel.userInterface.mouseMoved(e);
        
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        panel.userInterface.mouseClicked(e);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        // Clicking the game area reclaims keyboard focus so Escape / WASD keep
        // working even if focus drifted to the window chrome or another component.
        panel.requestFocusInWindow();

        // UI takes precedence: if a modal UI is open (inventory, menu), the
        // click belongs to the UI and never reaches the world. Must be the modal
        // check, not isEnabled() — the UI container is enabled for the whole
        // session, which would swallow every world click.
        if (panel.userInterface.isModalUIOpen()) {
            panel.userInterface.mousePressed(e);
            return;
        }
        // Left button = interact with the world. Other buttons currently
        // do nothing on the world layer.
        if (e.getButton() != MouseEvent.BUTTON1) return;

        // Boat-first: if the click landed on a Boat that we're standing next
        // to, board it. Skipped automatically when the player is holding a
        // boat item — placement wins in that case (handled inside the helper).
        if (tryBoardClickedBoat(e)) return;

        boolean offline = panel.multiplayer() == null || !panel.multiplayer().isOnline();
        long cooldown = offline ? PLACE_COOLDOWN_MS : PLACE_COOLDOWN_MS_ONLINE;
        long now = System.currentTimeMillis();
        if (now - lastPlaceMs < cooldown) return;

        // Only route to the local placement path when we are the authority
        // (single-player or offline). In online mode the server applies the
        // intent. The local cooldown stamp is set regardless so a rejected
        // server intent doesn't open the door to rapid-fire retries that
        // would saturate the link.
        if (offline) {
            handleOfflineLeftClick(now);
        } else {
            panel.inputHandlingSystem.enqueueAction(InputAction.PLACE);
            lastPlaceMs = now;
        }
    }

    /**
     * Offline left-click routing tree:
     *   1. If the equipped item is registered as placeable (fence, farmland,
     *      seed, etc.) → attempt placement at the snapped/free position.
     *      Don't fall through on failure; the player's intent was to place.
     *   2. Otherwise → attempt harvest of the entity directly under the
     *      cursor. Tools (axe, pickaxe) fall through to this branch because
     *      they're deliberately not in the placement registry.
     *   3. Otherwise → no-op.
     */
    private void handleOfflineLeftClick(long now) {
        Stack equipped = panel.player == null ? null : panel.player.getEquipped();
        String itemName = (equipped != null && !equipped.isEmpty()) ? equipped.getName() : null;

        if (itemName != null && PlacementRegistry.isPlaceable(itemName)) {
            if (panel.world.tryPlaceEntity(equipped)) lastPlaceMs = now;
            return;
        }
        if (panel.world.tryHarvestAtMouse(panel.player, panel)) {
            lastPlaceMs = now;
            return;
        }
        // No placeable equipped and nothing harvestable under the cursor:
        // legacy fallback for items whose physical representation exists in
        // ItemManager but isn't registered (e.g. "hammer"/"demoHouse"/"block"
        // until they migrate). Preserves the old behaviour.
        if (itemName != null && panel.world.tryPlaceEntity(equipped)) {
            lastPlaceMs = now;
        }
    }

    /**
     * Return true if the click landed on an existing boat's hitbox AND the
     * player is close enough to board. Skipped when the player is currently
     * holding a boat item — placing wins over boarding in that case, since
     * "place" needs to be doable on open water near other boats. Iterating
     * the world index here would have been enough for the click-hits-boat
     * check, but {@link resources.world.WorldInteraction#getEntitiesCollidedWith(Point)}
     * also returns Tile hits at that point — we filter for Boat explicitly.
     */
    private boolean tryBoardClickedBoat(MouseEvent e) {
        if (panel.player == null) return false;
        if (heldItemIsBoat()) return false;
        int worldX = (int) panel.camera.getWorldX() + e.getX();
        int worldY = (int) panel.camera.getWorldY() + e.getY();
        Point at = new Point(worldX, worldY);
        for (BaseEntity ent : panel.world.getEntitiesCollidedWith(at)) {
            if (!(ent instanceof Boat)) continue;
            Boat boat = (Boat) ent;
            // getEntitiesCollidedWith already vetted the hitbox hit, but be
            // defensive: re-check to make sure the click landed on the boat
            // itself rather than a tile underneath the same point.
            if (!boat.getHitBox().collision(at)) continue;
            return boat.tryBoardFromClick(panel.player);
        }
        return false;
    }

    private boolean heldItemIsBoat() {
        if (panel.player == null) return false;
        var equipped = panel.player.getEquipped();
        if (equipped == null || equipped.isEmpty()) return false;
        return "boat".equals(equipped.getName());
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void mouseExited(MouseEvent e) {
        // TODO Auto-generated method stub
       
        
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public double getMouseWorldX(){
        return panel.camera.getWorldX()+getX();
    }
    public double getMouseWorldY(){
        return panel.camera.getWorldY()+getY();
    }
    
}
