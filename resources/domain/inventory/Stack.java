package resources.domain.inventory;

import resources.app.GamePanel;
import resources.domain.entity.BaseEntity;
import resources.geometry.HitBox;

import java.util.ArrayList;

import resources.domain.entity.BaseEntity;
import resources.app.GamePanel;

public class Stack extends BaseEntity{

   
    int stackLimit = 99;
    int amount = 0;
    Item thisItem;


    public Stack(GamePanel panel, String name) {
        super(panel, name);
        thisItem = new Item(panel,name);
    }

    public Stack(GamePanel panel, Item item, int amount2) {
        super(panel,item.getName());
        this.amount = amount2;
        thisItem = item;
    }

    /** 
     * adds one item of given item
     * @returns true if successfull, false if failure
     */
    public boolean addItem(Item item){
        if (item == null) return false;
        if (isFull()) return false;
        if (isEmpty()) {
            // First item into an empty slot: claim it.
            setName(item.getName());
            setItem(item);
            amount++;
            return true;
        }
        if (thisItem != null && !thisItem.getName().equals(item.getName())) {
            return false;
        }
        amount++;
        return true;
    }

    private void setItem(Item item) {
       thisItem = item;
    }

    /**
     * Pour as many items as fit from {@code newStack} into this stack. Only
     * compatible if this stack is empty or carries the same item name —
     * otherwise we return immediately without removing anything from
     * {@code newStack}. Returns the (possibly emptied) source stack.
     *
     * Previously this looped {@code getOneItem} unconditionally; on a name
     * mismatch the items were drained from the source but rejected by
     * {@link #addItem}, which silently destroyed them. The guard below
     * eliminates that path.
     */
    public Stack addStack(Stack newStack){
        if (newStack == null || newStack.isEmpty()) return newStack;
        if (!isEmpty() && !getName().equals(newStack.getName())) return newStack;
        if (isEmpty()) {
            setName(newStack.getName());
            setItem(newStack.thisItem);
        }
        while (!isFull() && !newStack.isEmpty()){
            Item taken = newStack.getOneItem();
            if (taken == null) break;
            if (!addItem(taken)) {
                // Stack rejected the item (shouldn't happen now we name-guarded,
                // but be defensive): push it back into the source so we never
                // destroy items in transit.
                newStack.addItem(taken);
                break;
            }
        }
        return newStack;
    }

    

    /** if not empty, return last item, removes the item*/
    public Item getOneItem() {
        
        Item item = null;

        //if not empty, return last item
        if (!isEmpty()){
            item =  thisItem;
            amount--;
        }

        //at the end, check if empty
        if (isEmpty()){
            //stack name is set to the first item put.
            setName("empty");
            thisItem = null;
        }
        return item;
    }


    

    public int getAmount() {
        return amount;
    }
    public int getStackLimit() {
        return stackLimit;
    }




    protected boolean isFull(){
        return amount >= stackLimit;
    }

    public boolean isEmpty(){
        return amount <= 0;
    }

    // (No equals/hashCode override on Stack — identity equality is correct;
    // a previous override compared Stack to Item, which was broken.)

    public void removeOneItem() {
        getOneItem();
    }

    public Item getItem() {
        return thisItem;
    }
    
}
