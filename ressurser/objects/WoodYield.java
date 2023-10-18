package ressurser.objects;

import ressurser.main.GamePanel;

public class WoodYield extends Yieldable{

    public WoodYield(GamePanel gp, ObjectManager objM, int worldX, int worldY, String name, String type, int xPlus, int yPlus) {
        super(gp, objM, worldX, worldY, name, type);
        axeBreakable = true;
        yieldType = "wood";
        pixlePlusXvalue = xPlus;
        pixlePlusYvalue = yPlus;
        resourceYield = true;
        type = "wood";
    }

    @Override
    public void interact() {
        System.out.println("interact woodyield");
        durability--;

        if (durability<= 0){
            smash();
        }
        
    }
    @Override
    public void enterInteract(){
        System.out.println("enterinteract woodYield");
    }
    
    
}
