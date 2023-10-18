package ressurser.objects;

import ressurser.main.GamePanel;

public  class Dirt extends Yieldable {

    public Dirt(GamePanel gp, ObjectManager objM,int worldX,int worldY,String name,String type) {
        super(gp, objM,worldX,worldY,name,type);
        collision = false;
        shovelBreakable = true;
        yieldable = true;
        yieldType = "d";
        
    }

    @Override
    public void interact() {
       panel.gameState = panel.DIALOGSTATE;
       panel.textString = "looks like something happened here";
       panel.textBox = true;
    }

    public void continueInteraction(){
        panel.gameState = panel.PLAYSTATE ;
       panel.textBox = false;
    }

    
    
}
