package ressurser.baseEntity.playable.Inventory;

import java.util.ArrayList;

import ressurser.baseEntity.BaseEntity;
import ressurser.main.GamePanel;

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
        if (isEmpty()){
            setName(item.getName());
            setItem(item);
            if (!isFull()){
                amount++;
                return true;
            }
        } else{
            if (thisItem == null ||thisItem.getName().equals(item.getName())){
                if (!isFull()){
                    amount++;
                    return true;
                }
            }
        }
        return false;
        
    }

    private void setItem(Item item) {
       thisItem = item;
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
            System.out.println("emptyyyyyyy");
            
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

    @Override
    public boolean equals(Object o){
        if (!(o instanceof Item)){
            return false;
        }
        Item itm = (Item) o;

       return (itm.getName().equals(getName()));

        
    }

    public void removeOneItem() {
        getOneItem();
    }

    public Item getItem() {
        return thisItem;
    }
    
}
