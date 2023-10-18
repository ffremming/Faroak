package ressurser.objects;

import ressurser.main.GamePanel;

public class Stone extends Yieldable {
    
    public Stone(GamePanel gp, ObjectManager objM, int worldX, int worldY, String name, String type) {
        super(gp, objM, worldX, worldY, name, type);
        axeBreakable = true;
        pickaxeBreakable = true;
        yieldType = "wood";
    }

    

    public void interact(){
        smash();
    }

    @Override
    public void smash() {
       smashCounter ++;

        if (smashCounter >= 10){
           
            objM.removeObject();
            smashCounter = 0;
            int i = panel.menu.resources.content.indexOf(panel.menu.resources.inventory.get("wood"));
            panel.menu.resources.content.get(i).addQuantity(1);
        }
        
    }
    
}
