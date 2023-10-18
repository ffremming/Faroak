package ressurser.objects;

import ressurser.main.GamePanel;

public class LadderBack extends Teleport{
    public LadderBack (GamePanel gp,ObjectManager objM,int worldX,int worldY,int newX,int newY,String navn,String type) {
        super(gp, objM,worldX,worldY,newX,newY,navn,type);
        this.newX = newX;
        this.newY = newY;
    }

    @Override
    public void interact() {
        
        
        panel.enviromentM.activeEnviroment = panel.enviromentM.normalEnviroment;

        panel.mapH.activateNewMap("tm,0-0");
       
        spiller.changeCoordinates( newX, newY);
        
    }
}
