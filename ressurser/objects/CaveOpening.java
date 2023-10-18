package ressurser.objects;

import ressurser.main.GamePanel;

public class CaveOpening extends Teleport{

    public CaveOpening(GamePanel gp, ObjectManager objM,int worldX,int worldY ,String mp, int newX, int newY,String navn,String type) {
        super(gp, objM, worldX, worldY,newX,newY,navn,type);
    }
    @Override
    public void interact(){
        panel.enviromentM.activeEnviroment = panel.enviromentM.caveEnviroment;
       
        panel.mapH.activateNewMap(mapName);
    }
}
