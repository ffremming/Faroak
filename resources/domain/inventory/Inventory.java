package resources.domain.inventory;

import resources.app.GamePanel;
import resources.domain.entity.BaseEntity;
import resources.geometry.HitBox;

import java.util.ArrayList;

import resources.domain.player.Playable;

public class Inventory {
    
    public ArrayList<Stack> inventory = new ArrayList<>();
    final int SIZE = 9*4;
    int index = 0;


    public Inventory(Playable player){
        setUp(player);
    }

    private void setUp(Playable player){
        for (int i = 0;i<SIZE;i++)
        inventory.add(new Stack(player.panel, "empty"));
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
    public Item getItem(){
        return inventory.get(0).getOneItem();
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
        inventory.set(number, tempInHand);
    }

    public void increseIndex(){
        
        if (index<9){
            index++;
        }
    }

    public void decreaseIndex(){
        if (index>0){
            index--;
        }
    }

    public void setIndex(int newIndex){
        this.index = newIndex;
    }

    public int getIndex(){
        return index;
    }
}
