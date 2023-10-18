package ressurser.entity.NPC;

import ressurser.main.GamePanel;

public class NPC1 extends NPC{
   
    public NPC1(GamePanel panel, int worldX, int worldY) {
        super(panel, worldX, worldY);
        //TODO Auto-generated constructor stub


        //needs a better system
        antStreng = 3;
        dialoge = new String [antStreng];
        streng1 = "hei paa deg, hvordan gaar det?";
        streng2 = "jeg heter jon";
        streng3 = "lurer paa hva slags sted dette er";
        dialoge[0] = streng1;
        dialoge[1] = streng2;
        dialoge[2] = streng3;
        
    }
    
}
