package ressurser.objects;

import ressurser.main.GamePanel;
import ressurser.worldGeneration.dungeonGenerator.Dungeon;

public class Teleport extends NonCollisionInteraction{
    protected String mapName;

    protected int newX;
    protected int newY;

    public int newMapType;
    public int newMapNumber;

    
     

    public Teleport(GamePanel gp,ObjectManager objM,int worldX,int worldY,int newX,int newY,String navn,String type) {
        super(gp, objM,worldX,worldY,navn,type);

        alwaysInteract = true;
        collision = false;
       
    }

    @Override
    public void interact() {
        //maa fiksesss
        if (panel.tileM.tileMap[1][0][0][0]== null){
            Dungeon dung = panel.dungeonM.getDungeon();
            this.mapName = "tm,1-"+dung.number+".txt";
        }

        panel.enviromentM.activeEnviroment = panel.enviromentM.caveEnviroment;
       

        panel.enviromentM.activeEnviroment = panel.enviromentM.normalEnviroment;

        panel.mapH.activateNewMap(mapName);
        panel.tileM.TileGeneration();
        spiller.worldX = 55*32;
        spiller.worldY = 55*32;
        
    }

    /**
     * genererer nye objeketer og tiles.
     * mapVerdier er allerede oppdatert.
     * skal bare kjøres en gang per objekt.
     */
    public void NewMapGeneration(){
        panel.tileM.TileGeneration();
        panel.objM.readObjects();
    }
}
