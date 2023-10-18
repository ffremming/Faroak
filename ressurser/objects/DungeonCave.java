package ressurser.objects;

import ressurser.main.GamePanel;
import ressurser.worldGeneration.dungeonGenerator.Dungeon;

public class DungeonCave extends Teleport{

    public DungeonCave(GamePanel gp, ObjectManager objM,int worldX,int worldY ,String navn,String type) {
        super(gp, objM, worldX, worldY,55*32,55*32,navn,type);
    }
    @Override
    public void interact() {
        if (mapName== null){
            Dungeon dung = panel.dungeonM.getDungeon();
            this.mapName = "tm,1-"+dung.number+".txt";
            panel.mapH.activateNewMap(mapName);
            NewMapGeneration();
            while (!panel.tileM.tileMap[1][dung.number][25][25].type.equals("F")){
                dung = panel.dungeonM.getDungeon();
                this.mapName = "tm,1-"+dung.number+".txt";
                panel.mapH.activateNewMap(mapName);
                NewMapGeneration();
            }

        } else {
            panel.mapH.activateNewMap(mapName);
        }
        
        spiller.worldX = 25*32;
        spiller.worldY = 25*32;
        spiller.move = true;
        
    }
    
}
