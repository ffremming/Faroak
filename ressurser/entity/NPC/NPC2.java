package ressurser.entity.NPC;


import ressurser.main.GamePanel;

public class NPC2 extends NPC{
   
    public NPC2(GamePanel panel, int worldX, int worldY) {
        super(panel, worldX, worldY);
        
        antStreng = 1;
        dialoge = new String[antStreng];
        dialoge [0]= "hei hvordan går det";
        option = true;
        questionString = "vil du ha rare candy";

        
    }
}
