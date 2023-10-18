package ressurser.Tiles;

import java.io.File;
import java.util.HashMap;
import java.util.Scanner;
import java.awt.Font;
import java.awt.Graphics2D;
import ressurser.main.GamePanel;
import ressurser.baseEntity.HitBox;
import ressurser.baseEntity.tile.Tile;

import java.time.LocalTime;
import static java.time.temporal.ChronoUnit.MILLIS;


public class TileManager {
    GamePanel panel;
    
    public String stringMap[][][][];
    public ressurser.baseEntity.tile.Tile tileMap[][][][];

   
    public int maxWorldCol,maxWorldRow,worldHeight,worldWidth,tileSize;
    public String mapName;

    public  HashMap <String,Integer> tileValues = new HashMap<String,Integer>();
    public TileFactory tileF;
    boolean newGame;


    public Tile []tile;    //lagrer de ulike tilesene i array.
    public TileManager(GamePanel panel,boolean newGame){
        this.newGame = newGame;
        this.panel = panel;
        this.tileF = new TileFactory(panel);
        
        this.worldHeight= maxWorldRow *panel.tileSize;
        this.worldWidth = maxWorldCol *panel.tileSize;
        tileSize = this.panel.tileSize;
        
       
        this.tileMap = new Tile[3][10][panel.mapH.mapHeight][panel.mapH.mapWidth];
        this.stringMap = new String[3][10][panel.mapH.mapHeight][panel.mapH.mapWidth];
        TileGeneration();
    }


    //is called upon when a new map needs loading..
    public void TileGeneration(){
        getStringMap();
        getTileMap();

    }

    public void getStringMap(){

        File fil = new File("ressurser/Tiles/tileMaps/"+panel.mapH.activeMapName);

        try {
            System.out.printf("filnavn: %s\n", panel.mapH.activeMapName);
            Scanner scan = new Scanner(fil);
            int lineCount = 0;
            while (scan.hasNext()){    
                
                String linje = scan.nextLine();
                
                String []tegn = linje.split("\\s+");
                  //for drawing
                if (tegn.length > 1){
                    maxWorldCol = tegn.length;
                for (int i = 0;i<tegn.length;i++){      //kjører gjennom alle tegn i en rad.
                    String tileType = tegn[i];
                    
                    stringMap[panel.mapH.activeMapType][panel.mapH.activeMapNumber][lineCount][i]= tileType;  
                }
                lineCount ++;
                }
            }
            maxWorldRow = lineCount;
            System.out.println(maxWorldCol+","+maxWorldRow);
            //maxWorldRow = stringMap[panel.activeMapType][panel.activeMapNumber].length;    //for drawing
            //maxWorldCol = stringMap[panel.activeMapType][panel.activeMapNumber][0].length;

            scan.close();

        } catch (Exception e) { e.printStackTrace(); System.out.println("Feil!:"+panel.mapH.activeMapType+","+panel.mapH.activeMapNumber);}
    }
    


    
    private void getTileMap(){
        System.out.println("started tile generation:");
        LocalTime lt1 = LocalTime.now();
            System.out.println(maxWorldRow);
            System.out.println(maxWorldCol);
        
            for (int j = 0;j<stringMap[panel.mapH.activeMapType][panel.mapH.activeMapNumber].length;j++){
                for (int l = 0;l<stringMap[panel.mapH.activeMapType][panel.mapH.activeMapNumber][j].length;l++){

                    tileMap[panel.mapH.activeMapType][panel.mapH.activeMapNumber][j][l] = createTile(j,l);
                }
            }
        

        LocalTime lt2 = LocalTime.now();
        System.out.println("completed tile generation: time used: "+MILLIS.between(lt1,lt2)+" ms");
        

    }


    


    //må endre out of bounds opplegget.
    ressurser.baseEntity.tile.Tile createTile(int j,int l){

        String upper;
        String lower;
        String left;
        String right;
        String background;

        background = stringMap[panel.mapH.activeMapType][panel.mapH.activeMapNumber][j][l];
        System.out.println(background);
        
        if (isNotOutOfBounds(j+1,l)){
            lower = stringMap[panel.mapH.activeMapType][panel.mapH.activeMapNumber][j+1][l];
        } else {lower = background;}
        if (isNotOutOfBounds(j-1,l)){
            upper = stringMap[panel.mapH.activeMapType][panel.mapH.activeMapNumber][j-1][l];
            } else{upper = background;}
        if (isNotOutOfBounds(j,l+1)){
            right = stringMap[panel.mapH.activeMapType][panel.mapH.activeMapNumber][j][l+1];
        } else {right = background;}
        if (isNotOutOfBounds(j,l-1)){
            left = stringMap[panel.mapH.activeMapType][panel.mapH.activeMapNumber][j][l-1];
            } else {left = background;}
       
        return tileF.createTile(background,j*64,l*64);
    }

    public void draw(Graphics2D g2){
        int worldCol = 0;
        int worldRow = 0;
        panel.spiller.screenX = (int)(panel.frame.getBounds().getWidth()/2);
        panel.spiller.screenY = (int)(panel.frame.getBounds().getHeight()/2);
       
        while (worldCol < maxWorldCol && worldRow < maxWorldRow){
            
            //finner posisjon på skjermen som tilesene skal tegnes. 
            int screenX = (worldCol * tileSize)-(panel.spiller.worldX)+panel.spiller.screenX;
            int screenY = (worldRow * tileSize)-(panel.spiller.worldY)+panel.spiller.screenY;

            //if ((screenY + tileSize > panel.spiller.worldY-panel.frame.getBounds().getHeight() && screenX+tileSize> panel.spiller.worldY-panel.frame.getBounds().getWidth())&&((screenY - tileSize < panel.spiller.screenY+panel.frame.getBounds().getHeight()/2)&&(screenX - tileSize < panel.spiller.screenX +panel.frame.getBounds().getWidth()/2))){
           
            //tegner bare tiles som blir brukt:
            if ((screenY + tileSize > 0 && screenX+tileSize> 0)&&((screenY - tileSize < panel.frame.getBounds().getHeight())&&(screenX - tileSize < panel.frame.getBounds().getWidth()))){
                Tile t = tileMap[panel.mapH.activeMapType][panel.mapH.activeMapNumber][worldCol][worldRow];
               
                drawTile(t,screenX,screenY,g2);
                
            }
            worldCol ++;
            if (worldCol >= maxWorldCol){
                worldCol = 0;
                worldRow++;
            }

            
            
        }
    }
    public void drawTile(Tile t,int screenX,int screenY,Graphics2D g2){     //tar seg av seg å tegne hver enkelt tile. brukes i draw(). funker fint med animasjoner
        
        
        //should be altered to add a function. should hava a seperate animationmanager. 
        //animation gone

            //each tile could have seperate drawing functions. This could be usefull to draw interface, but might be probelmatic because the tile needs to know its borders, ant the value of the borders.
        g2.drawImage(t.getImage(),screenX,screenY,64,64,null);
        g2.setFont(new Font("Arial",Font.PLAIN,8));
        g2.drawString((t.getWorldX())+","+t.getWorldY(),screenX,screenY);


        HitBox hb = t.getHitBox();
        hb.draw(g2,panel.spiller);

       
        
       
    }


    private void drawTileBorders(Tile t,int screenX,int screenY,Graphics2D g2){

        //the border sprites needs to be stored in this entity

        //getBorderSprite(t)

        //g2.drawImage();
    }


    //implemening chanig of tiles as well as updating them

    public void placeTile(String objectType){
        // finner først verdien til nytt objekt. 

        int x = panel.spiller.worldX;
        int y = panel.spiller.worldY;
        int tileX = x/panel.tileSize;
        int tileY = y/panel.tileSize;
       
        if (panel.spiller.retning.equals("opp")){tileY-=1;}
        if (panel.spiller.retning.equals("ned")){tileY+=1;}
        if (panel.spiller.retning.equals("hoyre")){tileX+=1;}
        if (panel.spiller.retning.equals("venstre")){tileX-=1;}


        stringMap[panel.mapH.activeMapType][panel.mapH.activeMapNumber][tileY][tileX]= objectType;
        tileMap[panel.mapH.activeMapType][panel.mapH.activeMapNumber][tileY][tileX]= createTile(tileY,tileX);

        updateNeighbors(tileX,tileY,objectType);
        
    }

    //is used to change the tileSprites of the neightbor of a new tile placed. maby a problem with resetting the tile. a solution may be a method to change tilesprite inside tile.
    private void changeNeighbor(int tileX,int tileY,String tileString){

        if (isNotOutOfBounds(tileX,tileY)){
                
            if (stringMap[panel.mapH.activeMapType][panel.mapH.activeMapNumber][tileY][tileX].equals(tileString)){

                stringMap[panel.mapH.activeMapType][panel.mapH.activeMapNumber][tileY][tileX] = tileString;
                tileMap[panel.mapH.activeMapType][panel.mapH.activeMapNumber][tileY][tileX]= createTile(tileY,tileX);
            }
        }
    }



    private void updateNeighbors(int tileX,int tileY,String tileString){
        
        changeNeighbor(tileX,tileY-1,tileString);

        changeNeighbor(tileX,tileY+1,tileString);

        changeNeighbor(tileX-1,tileY,tileString);

        changeNeighbor(tileX+1,tileY-1,tileString);
    }

   

    private boolean isNotOutOfBounds(int x,int y){
        if (x >= 0  && x <panel.mapH.mapWidth && y >= 0 && y <panel.mapH.mapHeight){
            return true;
        } else {return false;}
        
    }

    public Tile getTile(int worldX,int worldY,String ret){
        
        int tileX = worldX/tileSize;
        int tileY = worldY/tileSize;
        
        if (ret.equals("opp")){tileY-=1;}
        if (ret.equals("ned")){tileY+=1;}
        if (ret.equals("hoyre")){tileX+=1;}
        if (ret.equals("venstre")){tileX-=1;}

        return tileMap[panel.mapH.activeMapType][panel.mapH.activeMapNumber][tileY][tileX];
    }

    public Tile getTile(String ret){
        
        int tileX = panel.spiller.worldX/tileSize;
        int tileY = panel.spiller.worldY/tileSize;
        
        if (ret.equals("opp")){tileY-=1;}
        if (ret.equals("ned")){tileY+=1;}
        if (ret.equals("hoyre")){tileX+=1;}
        if (ret.equals("venstre")){tileX-=1;}

        return tileMap[panel.mapH.activeMapType][panel.mapH.activeMapNumber][tileY][tileX];
    }

    public Tile getTile(){
        int x = panel.spiller.worldX;
        int y = panel.spiller.worldY;
        int tileX = x/tileSize;
        int tileY = y/tileSize;
        
        return tileMap[panel.mapH.activeMapType][panel.mapH.activeMapNumber][tileY][tileX];
    }

    public Tile getTile(int x,int y){

        if (x<0 ||x>panel.mapH.mapWidth*64+64||y<0 ||y>panel.mapH.mapHeight*64+64);

        int tileX = panel.objM.getNearest32(x)/panel.tileSize;
        int tileY = panel.objM.getNearest32(y)/panel.tileSize;

        return tileMap[panel.mapH.activeMapType][panel.mapH.activeMapNumber][tileY][tileX];
    }

    

}