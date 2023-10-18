package ressurser.main;

public class MapHandler {

    GamePanel panel;


    public int mapWidth = 100;
    public int mapHeight = 100;

    
    public String activeMapName = "tm,0-0.txt";

    //type: 0 overworld, 1, dungeons, 2 caves..
    public int activeMapType = 0;
    public int activeMapNumber = 0;

    public int activeMapWidth;
    public int activeMapHeight;

    public int lastWorldXCoordinate;
    public int lastWorldYCoordinate;
    public int lastWorldNumber;
    public int lastWorldtype;

    

    public MapHandler (GamePanel panel){
        this.panel = panel;

        
    }


    /**
     * use this to activateMap - should not start tilegeneration, objectgeneration, but should change players position.
     * activemapname,typeand number is updated.
     */
    public void activateNewMap(String mapName){

        //player x y values not handlet yet
        //entityH.getNewMapEntity(mapName);
        saveLastCoord();
        newActiveMapValues(mapName);
        updateActiveMapWidthAndHeight();
       
    }


    public void newActiveMapValues(String mapName){

        //changes values of the panel global map values
        activeMapName = mapName;

        mapName = mapName.replace(".txt","");

        String [] values = mapName.split(",");
        values = values[1].split("-");

        activeMapNumber = Integer.parseInt(values[1]);
        activeMapType = Integer.parseInt(values[0]);
    }

    private void saveLastCoord(){
        lastWorldXCoordinate = panel.spiller.worldX;
        lastWorldYCoordinate = panel.spiller.worldX;
        lastWorldNumber = activeMapNumber;
        lastWorldtype = activeMapType;
    }



    public void updateActiveMapWidthAndHeight(){
        getActiveMapWidth();
        getActiveMapHeight();
    }
    
    public void getActiveMapWidth(){
        activeMapWidth = 0;
        for (int i = 0;i<mapWidth;i++){
            if (panel.tileM.tileMap[activeMapType][activeMapNumber][0][i] == null){
                activeMapWidth = i;
               
                return ;
            }
        }
        if (activeMapWidth == 0){activeMapWidth = mapWidth;}
    }

    public void getActiveMapHeight(){
        activeMapHeight = 0;
        for (int i = 0;i< mapHeight;i++){
            if (panel.tileM.tileMap[activeMapType][activeMapNumber][i][0] == null){
                activeMapHeight = i;
              
                return ;
            }
        }
        if (activeMapHeight == 0){activeMapHeight = mapHeight;}
    }

}
