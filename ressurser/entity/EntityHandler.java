package ressurser.entity;

import ressurser.entity.NPC.NPC;
import ressurser.entity.NPC.NPC1;
import ressurser.entity.NPC.NPC2;
import ressurser.entity.spiller.Spiller;
import ressurser.main.GamePanel;
import java.awt.Graphics2D;

public class EntityHandler {
    public NPC npc [][][];
    GamePanel panel;
    public int mapIndeks;
    Spiller spiller;
    

    public EntityHandler(GamePanel panel){
        this.panel = panel;
        npc = new NPC [3][30][10];   //oppbevare verdier for kart og entity,.
        
       // npc[1][0][0]= new NPC2(panel,10*panel.tileSize,4*panel.tileSize);
        //npc[1][1][0]= new NPC1(panel,1*panel.tileSize,1*panel.tileSize);
       // npc[2][0][0]= new NPC(panel,1*panel.tileSize,1*panel.tileSize);
        spiller = panel.spiller;
    }

    

    public void drawAllEntities(Graphics2D g2){
            
            
            
            spiller.draw(g2);
    }
    
    public void update(){
        for (int i = 0;i<10;i++){
            if (npc[panel.mapH.activeMapType][panel.mapH.activeMapNumber][i] != null){
                (npc[panel.mapH.activeMapType][panel.mapH.activeMapNumber][i]).updateNPC();
                

                
                
                
            } 
        }
    }

      
}
