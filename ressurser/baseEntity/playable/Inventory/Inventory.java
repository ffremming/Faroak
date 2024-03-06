package ressurser.baseEntity.playable.Inventory;

import java.util.ArrayList;

import ressurser.baseEntity.playable.Playable;

public class Inventory {
    
    public ArrayList<Stack> inventory = new ArrayList<>();
    final int SIZE = 9*4;


    public Inventory(Playable player){
        setUp();
    }

    private void setUp(){
        for (int i = 0;i<SIZE;i++)
        inventory.add(new Stack(null, "empty"));
    }

    /** adds one item of given item , only takes in one item
     * @returns true if sucessful, false if not
    */
    public boolean addItem(Item item){
        for (Stack stack:inventory){
            if (stack.getName().equals(item.getName())){
                if (stack.addItem(item)){
                    return true;
                }
            }
        }
        return false;
    }

    /**adds one stack of given stack. Should be sorted and placed correctly
     * @returns Stack - if sucessfull, this should be empty, if not, the rest of the items is returned.
     */
    public Stack addStack(Stack newStack){
        for (Stack stack:inventory){
            if (stack.getName().equals( newStack.getName())){
                if (stack.addStack(newStack).isEmpty()){
                    return newStack;
                }
            }
        }
        return newStack;
        //TOOD
    }

    /**@returns one item of given item in given slot*/
    public Item getItem(int index){
        return inventory.get(index).getOneItem();
    }

    public int getSize() {
       return SIZE;
    }

    /**returns the stack in the given position */
    public Stack getStack(int i) {
        if (i<SIZE){
            return inventory.get(i);
        }
        return null;
    }
}
