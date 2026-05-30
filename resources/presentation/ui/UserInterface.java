package resources.presentation.ui;

import resources.app.GamePanel;
import resources.domain.inventory.Inventory;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;

public class UserInterface extends Container{

    private PlayerInventory inventoryUI;
    private final EscapeMenu menu;
    private final HealthHUD hud;

    /**
     * Container overlays opened by world objects (chest / crafting table /
     * barrel). Tracked here so the input layer knows a modal UI is up — without
     * this {@link #isModalUIOpen()} returned false for these, so clicks fell
     * through to the world instead of the UI and the slots were dead. Also gives
     * Escape a single place to close the topmost overlay.
     */
    private final java.util.List<Container> overlays = new java.util.ArrayList<>();

    /** Transient message shown at the top of the screen (e.g. "can't dismount
     *  in open water"). Cleared automatically once {@link #toastUntilMs} has
     *  passed. */
    private String toastText;
    private long   toastUntilMs;


    public UserInterface(GamePanel panel, int x, int y) {
        super(panel, x, y);
        width = panel.getFrameWidth();
        height = panel.getFrameHeight();
        hud = new HealthHUD(panel);

        setBackground(new Color(100,0,0,50));
        setForeGround(new Color(0,0,0,100));

        menu = new EscapeMenu(panel);
        menu.hide();
        add(menu);
    }

    public void addInventory(Inventory inventory){

        int rows = inventory.getSize()/9;
        int cols = 9;
        

        inventoryUI = new PlayerInventory(panel,rows,cols,400,300,inventory);
        // Padding inherited from ItemContainer.PANEL_PADDING so the player
        // inventory matches the chest baseline; no per-call override.
        add(inventoryUI);
        
        
        
        inventoryUI.center(getCenter());
        
    }

    public void toggleInventory(){
        if (enabled){
            if (inventoryUI.visible){
                returnTempInHand();
                cleanUI();
                inventoryUI.visible = false;
                inventoryUI.enabled = false;
            } else {
                cleanUI();
                inventoryUI.visible = true;
                inventoryUI.enabled = true;
            }
        }
    }

    /**
     * If the player closes the inventory while still carrying a stack on the
     * cursor, drop that stack back into the first slot that will accept it.
     * Without this the icon kept tracking the cursor and the items were
     * effectively held in limbo — visible but inaccessible.
     */
    private void returnTempInHand() {
        if (panel.player == null) return;
        resources.domain.inventory.Stack held = panel.player.getTempInHand();
        if (held == null || held.isEmpty()) {
            panel.player.setTempInHand(null);
            return;
        }
        panel.player.getInventory().addStack(held);
        panel.player.setTempInHand(null);
    }

    /**
     * Show the player inventory alongside an open chest (or other container)
     * overlay so stacks can be dragged between the two grids. Uses the inventory
     * exactly as the standalone {@code E} view renders it — no restyling, no
     * repositioning — just makes it visible while the chest is open.
     */
    public void openInventoryPaired(Container anchor){
        if (inventoryUI == null) return;
        returnTempInHand();
        inventoryUI.visible = true;
        inventoryUI.enabled = true;
    }

    /** Hide the inventory again when the paired container closes. */
    public void closeInventoryPaired(){
        if (inventoryUI == null) return;
        returnTempInHand();
        inventoryUI.visible = false;
        inventoryUI.enabled = false;
    }

    public void toggleMenu(){
        if (menu.isOpen()) {
            menu.hide();
        } else {
            cleanUI();
            if (panel.inputHandlingSystem != null) {
                panel.inputHandlingSystem.clearHeldInput();
            }
            menu.show();
        }
    }

    /**
     * Register a freshly opened container overlay (chest / crafting / barrel).
     * The bridge has already added the UI to {@code content} and set it
     * visible/enabled; this only records it so the input layer treats it as
     * modal and Escape can close it.
     */
    public void openOverlay(Container overlay){
        if (overlay == null || overlays.contains(overlay)) return;
        overlays.add(overlay);
    }

    /** Drop a closed overlay from the modal-tracking list. */
    public void closeOverlay(Container overlay){
        overlays.remove(overlay);
    }

    /** True while any container overlay is open. */
    public boolean hasOpenOverlay(){ return !overlays.isEmpty(); }

    /**
     * Close the most recently opened overlay via its registered closer.
     * Returns true if one was closed — lets Escape consume the keystroke for
     * "close the chest" before falling back to "open the pause menu".
     */
    public boolean closeTopOverlay(){
        if (overlays.isEmpty()) return false;
        Container top = overlays.get(overlays.size() - 1);
        Runnable closer = overlayClosers.remove(top);
        if (closer != null) closer.run();   // bridge removes UI + calls closeOverlay
        else { overlays.remove(top); remove(top); }
        return true;
    }

    /** Per-overlay close callbacks supplied by the opening bridge. */
    private final java.util.Map<Container, Runnable> overlayClosers = new java.util.IdentityHashMap<>();

    /** Register an overlay together with the action that fully closes it. */
    public void openOverlay(Container overlay, Runnable closer){
        openOverlay(overlay);
        if (closer != null) overlayClosers.put(overlay, closer);
    }


    @Override
    public void draw(Graphics2D g2){
        width = panel.width;
        height = panel.height;
        if (inventoryUI!= null){
            inventoryUI.center(getCenter());
            inventoryUI.setWidth((int)(0.8*panel.width/2));
            inventoryUI.setHeight((int)inventoryUI.getWidth()/2);
        }

        super.draw(g2);

        // Always-on HUD overlay (HP bar + death banner).
        hud.draw(g2);

        //temporary item in hand — only draw if it's a real, non-empty stack
        if (panel.player != null) {
            resources.domain.inventory.Stack held = panel.player.getTempInHand();
            if (held != null && !held.isEmpty() && !"empty".equals(held.getName())) {
                BufferedImage tempImg = panel.imageContainer.getItemImage(held.getName());
                g2.drawImage(tempImg, panel.mouse.getX() - 25, panel.mouse.getY() - 25, 50, 50, null);
            }
        }

        drawToast(g2);
    }

    /** Show a transient HUD message for {@code durationMs} milliseconds. */
    public void showToast(String text, long durationMs) {
        this.toastText = text;
        this.toastUntilMs = System.currentTimeMillis() + Math.max(0L, durationMs);
    }

    private void drawToast(Graphics2D g2) {
        if (toastText == null) return;
        if (System.currentTimeMillis() > toastUntilMs) {
            toastText = null;
            return;
        }
        java.awt.FontMetrics fm = g2.getFontMetrics();
        int textW = fm.stringWidth(toastText);
        int padX = 10;
        int padY = 6;
        int boxW = textW + padX * 2;
        int boxH = fm.getHeight() + padY;
        int boxX = (panel.width - boxW) / 2;
        int boxY = 24;
        g2.setColor(new Color(0, 0, 0, 170));
        g2.fillRoundRect(boxX, boxY, boxW, boxH, 8, 8);
        g2.setColor(new Color(240, 240, 240));
        g2.drawString(toastText, boxX + padX, boxY + fm.getAscent() + padY / 2);
    }

    public void toggleUI() {
        if (enabled){
            enabled = false;
        } else{
            enabled = true;
        }
    }


    public void cleanUI(){
        menu.hide();
        if (inventoryUI != null) {
            inventoryUI.enabled = false;
            inventoryUI.visible = false;
        }
    }

    /** True when the escape menu is currently shown — used to pause input. */
    public boolean isMenuOpen() { return menu.isOpen(); }

    /**
     * True when a modal UI that wants the mouse wheel for its own scrolling is
     * open (the full inventory grid, or the escape menu). The hotbar wheel-cycle
     * is suppressed only in these cases — {@link #isEnabled()} can't be used for
     * this because the UI container is enabled for the whole session, which would
     * permanently swallow hotbar scrolling.
     */
    public boolean isModalUIOpen() {
        if (menu.isOpen()) return true;
        if (!overlays.isEmpty()) return true;
        return inventoryUI != null && inventoryUI.visible;
    }

    public boolean isEnabled(){
        for (Component comp:content){
            if(comp.enabled){
                return true;
            }
        }
        return false;
    }

    /**
     * Wipe the UI back to its baseline. The escape menu is a permanent fixture
     * (added in the constructor) so it must survive a clear — otherwise the
     * player setup path ({@link resources.domain.player.Playable}) calls
     * {@code clear()} then re-adds only the inventory, dropping the menu out of
     * the draw list and leaving it invisible even though {@code isOpen()} reads
     * true. Re-register it after clearing so it keeps rendering.
     */
    public void clear() {
        content.clear();
        add(menu);
    }

    /** Test/probe hook: number of child components currently registered. */
    public int debugContentCount() { return content.size(); }

    

    
    
}
