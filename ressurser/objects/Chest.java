package ressurser.objects;

import ressurser.main.GamePanel;

public class Chest extends InteractableObj {
    
    public Chest(GamePanel gp,ObjectManager objM,int worldX,int worldY,String name,String type) {
        super(gp, objM,worldX,worldY,name,type);
       
    }

    public void interact(){
        panel.textBox = true;
        panel.textString = ("you have found a "+name+"!");
        panel.gameState = panel.DIALOGSTATE;
        
        panel.menu.items.addItem(name);     //put a item in items, if already have one, add quantity.
        
    }

    public void continueInteraction(){
        panel.textBox = false;
        panel.gameState = panel.PLAYSTATE ;
        panel.objM.removeObject();
        
    }


}