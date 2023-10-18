package ressurser.baseEntity.inventory;

import ressurser.baseEntity.gameObjects.Item;

public abstract class Inventory {
    
    final int ROWSIZE = 8;
    final int MAXSLOTSIZE;
    Item [][] content;

    
    /**
     * this is where the player can acess items.
     */
    public Inventory(int maxSlotSize){
        this.MAXSLOTSIZE = maxSlotSize;
        content = new Item [(int) maxSlotSize/8][ROWSIZE];

    }

    /**
     * adds item to first avalable space.if no more space, the item will not be added.
     * if no item can be added,return false
     * 
     * @return successful
     */
    public boolean add(Item item){
        if (!AddToItem(item)){
            if (!addNewToInventory(item)){
                return false;
            }
        }
        return true;
    }

    /**
     * tries to add an item to an already existing item, returns the results of it has managed to do so.
     */
    private boolean AddToItem(Item item){
        for (int col = 0;col< content.length;col++){
            for (int row = 0;row<content[0].length;row++ ){
                if (content[col][row]!= null){
                    Item slotItem = content[col][row];

                    if (slotItem == item&& !slotItem.isFull()){
                        if (slotItem.increaseAmount()){
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
    /**
     * adds an additional item to the inventory, that is stored in a unused slot.
     */
    private boolean addNewToInventory(Item item){

        for (int col = 0;col< content.length;col++){
            for (int row = 0;row<content[0].length;row++ ){

                if( content[col][row]== null){
                    content[col][row] = item;
                    item.setAmount(1);
                    return true;
                }
            }
        }
        return false;
    }

    public void add(Item item,int row,int col){
        if (true);
    }

}
