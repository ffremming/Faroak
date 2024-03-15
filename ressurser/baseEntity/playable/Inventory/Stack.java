package ressurser.baseEntity.playable.Inventory;

import java.util.ArrayList;

import ressurser.baseEntity.BaseEntity;
import ressurser.main.GamePanel;

public class Stack extends BaseEntity{

   
    int stackLimit = 99;
    ArrayList<Item> items = new ArrayList<>();


    public Stack(GamePanel panel, String name) {
        super(panel, name);
    }

    /** 
     * adds one item of given item
     * @returns true if successfull, false if failure
     */
    public boolean addItem(Item item){
        if (isEmpty()){
            //stack name is set to the first item put.
            setName(item.getName());
        }

        if (!isFull()){
            items.add(item);
            return true;
        }
        return false;
    }

    /** 
     * adds as much of a stack as possible if the type(name) is the same as given stack
     * @returns stack-if succesfull this is empty.
     */
    public Stack addStack(Stack newStack){
        if (isEmpty()){
            //stack name is set to the first item put.
            setName(newStack.getName());
        }

        
        while (!isFull() && !newStack.isEmpty()){
            addItem(newStack.getOneItem());
        }

        return newStack;
    }

    private  ArrayList<Item> getItems() {
       return items;
    }

    /** if not empty, return last item, removes the item*/
    public Item getOneItem() {
        
        Item item = null;

        //if not empty, return last item
        if (!isEmpty()){
            item =  items.get(-1);
            items.remove(-1);
        }

        //at the end, check if empty
        if (isEmpty()){
            //stack name is set to the first item put.
            setName("empty");
        }
        return item;
    }


    public Item getItem(int index){
        if (!isEmpty()){
            return items.get(index);
        } else {
            return null;
        }
        
    }

    public int getAmount() {
        return items.size();
    }
    public int getStackLimit() {
        return stackLimit;
    }




    protected boolean isFull(){
        return getAmount() >= stackLimit;
    }

    public boolean isEmpty(){
        return items.size() <= 0;
    }

    @Override
    public boolean equals(Object o){
        if (!(o instanceof Item)){
            return false;
        }
        Item itm = (Item) o;

       return (itm.getName().equals(getName()));

        
    }

    public void removeOneItem() {
        if (!isEmpty()){
            items.remove(items.size()-1);
        }
    }
    
}
