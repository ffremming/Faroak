package ressurser.main.GUIMenu;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;

import ressurser.main.GamePanel;
import ressurser.meny.items.Item;

public class ItemContainerSlot extends Component{

    Item item;

    public ItemContainerSlot(GamePanel panel) {
        super(panel);

        setBackground(Color.LIGHT_GRAY);
        setForeGround(Color.DARK_GRAY);
        height = 64;
        width = 64;
    }


    public void addItem(Item item){
        this.item = item;
    }


    public void removeItem(){
        item = null;
    }   

    public void hover(){
    }

    public void press(){
    }

    public void draw(Graphics2D g2){
        drawRect(g2);
        if (item!= null){
            g2.drawImage(item.sprite,x,y,null);
        }
        if (hover){
            
        }
        
    }

    public void mousePressed(MouseEvent e){
        switchItems();
        System.out.println(" slot pressed");
    }

    private void switchItems(){
        Item newItem = null;
        newItem = panel.menuStateUI.chosenItem;

        panel.menuStateUI.chosenItem = item;
        item = newItem;

        System.out.println(item);
        System.out.println(panel.menuStateUI.chosenItem);
    }

    public Item getItem(){
        
        return item;
        
    }
    //getPointerInfo().getLocation()

    public void mouseMoved(MouseEvent e){
        hover();
    }
}
