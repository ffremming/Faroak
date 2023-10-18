package ressurser.main.GUIMenu;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import ressurser.main.GamePanel;
import ressurser.meny.items.Item;

public class ItemBar extends ItemContainer{

    

    public ItemBar(GamePanel panel, int rows, int cols, int slotWidth, int slotHeight) {
        super(panel, rows, cols, slotWidth, slotHeight);
       
    }

    public Item getActiveItem(){
        return ((ItemContainerSlot)content.get(indeks)).getItem();
    }
    
    public void mouseWheelMoved(MouseWheelEvent e){
        System.out.println("mouseWheel item");
        //scroll on container, not yet implemented, skal endre indeksen.
        changeIndex(e.getWheelRotation());
        System.out.println(indeks);
    }

    private void changeIndex(int rotation){
        if (rotation ==-1&& indeks >0){
            indeks--;
        } else if (rotation ==1 && indeks <maxIndeks){
            indeks++;
        }
        System.out.println("rotation:"+rotation);
    }

    

    
}
