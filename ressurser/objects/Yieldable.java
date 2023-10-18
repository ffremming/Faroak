package ressurser.objects;

import ressurser.main.GamePanel;

public class Yieldable extends Breakable{

    public Yieldable(GamePanel gp, ObjectManager objM, int worldX, int worldY, String name, String type) {
        super(gp, objM, worldX, worldY, name, type);
        yieldable = true;
        //TODO Auto-generated constructor stub
    }
    

     @Override
    public void smash() {
       durability --;

        if (durability <= 0){
            //connected cells disappear
            
            
            //this cell erased
            objM.map [panel.mapH.activeMapType][panel.mapH.activeMapNumber][worldY/32][worldX/32] = null;
           
            //yield
            if (materialYield){
                materialYield();
            } else if (resourceYield){
                resourceYield();
            }
        }
    }

    protected void materialYield(){
        panel.menu.materials.addItem(yieldType);
    }

    protected void resourceYield(){
        panel.menu.resources.addItem(yieldType);
    }
    
    
}
