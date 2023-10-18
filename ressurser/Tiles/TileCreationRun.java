package ressurser.Tiles;

import java.util.concurrent.CountDownLatch;

public class TileCreationRun implements Runnable{

    TileManager tM;
    TileFactory tF;
    int start;
    CountDownLatch minBarriere ;

    public TileCreationRun(TileManager tM,TileFactory tF,int startIndeks,CountDownLatch minBarriere){
        this.tM = tM;
        this.tF = tF;
        start = startIndeks;
        this.minBarriere = minBarriere;
    }

    @Override
    public void run() {
        try{
            for (int i = start;i<start+50;i++){
                for (int l = 0;l<tM.panel.mapH.mapHeight;l++){
                   
                    tM.tileMap[tM.panel.mapH.activeMapType][tM.panel.mapH.activeMapNumber][i][l] = tM.createTile(i,l);
                  
                }
            }
        }
        finally {
            minBarriere.countDown();

        }
        
    } 
    
}
