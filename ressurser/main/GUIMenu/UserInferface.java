package ressurser.main.GUIMenu;

import java.awt.Color;
import java.awt.event.MouseEvent;
import java.awt.Graphics2D;

import ressurser.baseEntity.playable.Inventory.Inventory;
import ressurser.main.GamePanel;

public class UserInferface extends Container{

    PlayerInventory inventoryUI;
    Container menu;


    public UserInferface(GamePanel panel, int x, int y) {
        super(panel, x, y);
        width = panel.getFrameWidth();
        height = panel.getFrameHeight();

        


        System.out.println(width+height);

        setBackground(new Color(0,0,0,0));
        setForeGround(new Color(0,0,0,0));

        
        
        


        menu = new Container(panel,300,300,3,1);
        Button b1 = new Button(panel,"settings");
        
        Button b2 = new Button(panel,"audio");

        Button b3 = new Button(panel,"save and quit");
        menu.add(b1);
        menu.add(b2);
        menu.add(b3);
        add(menu);
        menu.padding = 20;
        
        
        menu.visible = false;
        menu.setBackground(Color.LIGHT_GRAY);
        menu.setForeGround(Color.DARK_GRAY);
    }

    public void addContainer(Inventory inventory){


        int rows = inventory.getSize()/9;
        int cols = 9;
        

        inventoryUI = new PlayerInventory(panel,rows,cols,400,300,inventory);
        inventoryUI.setPadding(20);
        add(inventoryUI);
        inventoryUI.visible = true;
       
        
        inventoryUI.center(getCenter());
        
    }

    public void toggleInventory(){
        if (enabled){
            if (inventoryUI.visible){
                cleanUI();
                inventoryUI.visible = false;
                inventoryUI.enabled = false;
            } else {
                cleanUI();
                inventoryUI.visible = true;
                inventoryUI.enabled = true;
            }
        }
    }

    public void toggleMenu(){
        
        if (enabled){
            
            if (menu.visible){
                cleanUI();
                menu.visible = false;
                menu.enabled = false;
            } else {
                cleanUI();
                menu.visible = true;
                menu.enabled = true;
            }
        }
    }


    @Override
    public void draw(Graphics2D g2){
        width = panel.width;
        height = panel.height;
        if (inventoryUI!= null){
            inventoryUI.center(getCenter());
        }
        
        inventoryUI.setWidth((int)(0.8*panel.width/2));

        inventoryUI.setHeight((int)inventoryUI.getWidth()/2);


        menu.width = width/6;
        menu.height = menu.width;


        menu.center(getCenter());
        super.draw(g2);

    }

    public void toggleUI() {
        if (enabled){
            enabled = false;
        } else{
            enabled = true;
        }
    }


    public void cleanUI(){
        menu.visible = false;
        menu.enabled = false;
        inventoryUI.enabled = false;
        inventoryUI.visible = false;
    }

    public boolean isEnabled(){
        for (Component comp:content){
            if(comp.enabled ||comp.visible){
                return true;
            }
        }
        return false;
    }

    

    
    
}
