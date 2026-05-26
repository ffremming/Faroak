package resources.domain.inventory;

import resources.app.GamePanel;

import java.util.ArrayList;

import resources.domain.player.Playable;

/**
 * Fixed-size slot container. The grid is laid out as {@link #HOTBAR_COLS}
 * columns × 4 rows = 36 slots; the bottom row (slots {@code SIZE - HOTBAR_COLS
 * .. SIZE - 1}) is the hotbar. The hotbar selection {@link #index} is owned
 * here — UI components must read/write it through {@link #getIndex()} /
 * {@link #setIndex(int)} rather than caching their own copy.
 */
public class Inventory {

    public static final int HOTBAR_COLS = 9;
    public static final int ROWS        = 4;
    public static final int SIZE        = HOTBAR_COLS * ROWS;
    /** First slot index of the hotbar row. */
    public static final int HOTBAR_OFFSET = SIZE - HOTBAR_COLS;

    public ArrayList<Stack> inventory = new ArrayList<>();
    private int index = 0;


    public Inventory(Playable player){
        setUp(player.panel);
    }

    /** Panel-only constructor for inventories that exist before the player is built. */
    public Inventory(GamePanel panel){
        setUp(panel);
    }

    private void setUp(GamePanel panel){
        for (int i = 0;i<SIZE;i++)
        inventory.add(new Stack(panel, "empty"));
    }

    /** adds one item of given item , only takes in one item
     * @returns true if sucessful, false if not
    */
    public boolean addItem(Item item){
        for (Stack stack:inventory){
          
            
                if (stack.addItem(item)){
                    return true;
                
            } 
        }
        
        return false;
    }

    /** adds given amount of item , only takes in one item
     * @returns amount left.
    */
    public int addItem(Item item,int amount){

        for (int i = 0;i<amount;i++){
            if (!(addItem(item))){
                return amount -i;
            } 
        }
        return 0;
    }

    /**
     * Pour items from newStack into existing same-named stacks first (so
     * partial stacks fill up before new slots are claimed), then into the
     * first empty slot. Returns newStack — drained to empty on success,
     * still holding the overflow if the inventory ran out of room.
     *
     * Prior behaviour skipped empty slots entirely, which meant the very
     * first stack of a new item type was silently discarded on a fresh
     * inventory. That broke every starter item added via
     * Playable.addItem(Item, int).
     */
    public Stack addStack(Stack newStack){
        if (newStack == null) return newStack;
        // Phase 1: top up existing stacks of the same name.
        for (Stack stack : inventory){
            if (newStack.isEmpty()) return newStack;
            if (!"empty".equals(stack.getName())
                && stack.getName().equals(newStack.getName())){
                stack.addStack(newStack);
            }
        }
        // Phase 2: spill remaining items into empty slots.
        for (Stack stack : inventory){
            if (newStack.isEmpty()) return newStack;
            if ("empty".equals(stack.getName())){
                stack.addStack(newStack);
            }
        }
        return newStack;
    }

    public int getSize() {
       return SIZE;
    }

    /** Returns the stack at slot {@code i}, or null if out of range. */
    public Stack getStack(int i) {
        if (i < 0 || i >= SIZE) return null;
        return inventory.get(i);
    }

    /** Hotbar stack at column {@code col} (0..HOTBAR_COLS-1), null if out of range. */
    public Stack getHotbarStack(int col) {
        if (col < 0 || col >= HOTBAR_COLS) return null;
        return inventory.get(HOTBAR_OFFSET + col);
    }

    @Override
    public String toString() {
        String s = "";
        for (Stack stack:inventory){
            if (stack != null){
                s+=stack.getName()+"\n";
            }
            
        }
        return s;
    }

    public void setStack(int number, Stack tempInHand) {
        if (number < 0 || number >= SIZE || tempInHand == null) return;
        inventory.set(number, tempInHand);
    }

    /** Cycle hotbar selection right with wrap. */
    public void increseIndex(){
        index = (index + 1) % HOTBAR_COLS;
    }

    /** Cycle hotbar selection left with wrap. */
    public void decreaseIndex(){
        index = (index - 1 + HOTBAR_COLS) % HOTBAR_COLS;
    }

    public void setIndex(int newIndex){
        if (newIndex < 0 || newIndex >= HOTBAR_COLS) return;
        this.index = newIndex;
    }

    public int getIndex(){
        return index;
    }
}
