package ressurser.main.GUIMenu;

import java.awt.Color;
import java.awt.event.MouseEvent;
import java.awt.Graphics2D;

import ressurser.main.GamePanel;

public class UserInferface extends Container{

    ItemContainer inventory;
    Container menu;


    public UserInferface(GamePanel panel, int x, int y) {
        super(panel, x, y);
        width = panel.getFrameWidth();
        height = panel.getFrameHeight();

        


        System.out.println(width+height);

        setBackground(new Color(0,0,0,0));
        setForeGround(new Color(0,0,0,0));

        inventory = new ItemContainer(panel,3,7,400,300);
        inventory.setPadding(20);
        add(inventory);
        inventory.visible = true;
       
        
        inventory.center(getCenter());
        
        


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

    public void toggleInventory(){
        if (enabled){
            if (inventory.visible){
                cleanUI();
                inventory.visible = false;
                inventory.enabled = false;
            } else {
                cleanUI();
                inventory.visible = true;
                inventory.enabled = true;
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
        inventory.center(getCenter());



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
        inventory.enabled = false;
        inventory.visible = false;
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
