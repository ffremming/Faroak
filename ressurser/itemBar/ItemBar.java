package ressurser.itemBar;

import ressurser.main.GamePanel;
import ressurser.meny.items.Item;
import ressurser.meny.items.materials.Material;

public class ItemBar {
    
    GamePanel panel;
    public Item [] itemBarContent;
    public int indexValue;
    Item activeItem;

    public boolean waitingForInput = false;


    public ItemBar(GamePanel panel){
        this.panel = panel;
        itemBarContent  =  new Item [9];
        indexValue = 0;
        activeItem = null;
    }

    public void changeIndex(int number){
        if (number >=0 && number<8){
            indexValue = number;
            updateItem();
        }
        
    }

    private void updateItem(){
        
        panel.spiller.activeItem = itemBarContent[indexValue];
        
       
        if (panel.spiller.activeItem instanceof Material){
           
        } else { }

    }

    public boolean itemBarFull(){
        for (Item i:itemBarContent){
            if (i == null){
                return false;
            }
        }
        return true;
    }

    public int getNextEmptySlot(){
        int index = 0;
        for (Item i:itemBarContent){
            if (i == null){
                return index;
            }
            index++;
        }
        return 0;
    }
    
    public void updatePlayerContent(){
        panel.spiller.activeItem = itemBarContent[indexValue];
    }

    public boolean isItemInBar(Item item){
        for (Item i:itemBarContent){
            if (i != null){
                if (i == item ){
                    return true;
                }
            }
        }
        return false;
    }

    public void removeItem(Item item){
        
        for (Item i:itemBarContent){
            if (i != null){
                if (i == item ){
                    i = null;
                }
            }
        }
        
    }
}
