package ressurser.objects;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import javax.imageio.ImageIO;

import ressurser.baseEntity.tile.Tile;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import ressurser.entity.spiller.Spiller;
import ressurser.main.GamePanel;

import java.time.LocalTime;
import static java.time.temporal.ChronoUnit.MILLIS;

public class ObjectManager {
    //skal lese inn objects
    public SuperObject [] objects;   //holder på de ulike objektene. 
    GamePanel panel;
    

    public SuperObject [][][][] map;
    int tileSize; 
    
   
    
    public GameObjectFactory factory;

    ArrayList <Farmable> plantList = new ArrayList<Farmable>();

    public SuperObject tempObject = null;
    public Tile tempTile = null;

    public BufferedImage tempTileImage;
   
    

    


    HashMap <String,Integer> objectValues = new HashMap<>();
    HashMap <String,BufferedImage> objectSprites = new HashMap<>();
    boolean newGame;
   

    public ObjectManager(GamePanel panel,boolean newGame){
        this.newGame = newGame;
        this.panel = panel;
        
        tileSize = panel.tileSize;
        factory = new GameObjectFactory(panel,this);
        int maxWorldCol = panel.mapH.mapWidth;
        int maxWorldRow = panel.mapH.mapHeight;
        this.map = new SuperObject[3][10][maxWorldRow][maxWorldCol];

        prepareObjects();
       
    }

    private void prepareObjects(){

        System.out.println("started object generation");
        LocalTime lt1 = LocalTime.now();
        readAllObjectSprites();
        readObjects();
        LocalTime lt2 = LocalTime.now();
        
        System.out.println("completed object generation: time used:"+MILLIS.between(lt1,lt2)+" ms");
        


        try {
             tempTileImage = ImageIO.read(new File("ressurser/tileSprites/temporaryTile.png"));
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    

    private void readAllObjectSprites(){
        try {
            BufferedImage warmStone = ImageIO.read(getClass().getResourceAsStream("objectSprites/superObjects/"+"stoneM1"+".png"));
        BufferedImage normalTree = ImageIO.read(getClass().getResourceAsStream("objectSprites/superObjects/"+"tree"+".png"));
        BufferedImage highNormalTree = ImageIO.read(getClass().getResourceAsStream("objectSprites/superObjects/"+"highTree"+".png"));
        BufferedImage highBush = ImageIO.read(getClass().getResourceAsStream("objectSprites/superObjects/"+"highBush"+".png"));
        BufferedImage smallNormalTree = ImageIO.read(getClass().getResourceAsStream("objectSprites/superObjects/"+"smallTree"+".png"));
        BufferedImage wildGrass = ImageIO.read(getClass().getResourceAsStream("objectSprites/superObjects/"+"wildgrassP3"+".png"));
        /* 
        objectSprites.put("warmStone",warmStone);
        objectSprites.put("tree",normalTree);
        objectSprites.put("highTree",highNormalTree);
        objectSprites.put("highbush",highBush);
        objectSprites.put("smallNormalTree",smallNormalTree);
        objectSprites.put("wildgrassP3",wildGrass);
        */

        } catch (IOException ioe){
           ioe.printStackTrace();
        }
        



        
    }
    
    //method read all objects from txt document and convert infromation to array. 
    void readObjects(){
        
        File fil = new File("ressurser/objects/objectTxtDocuments/"+panel.mapH.activeMapName);
        try {
            
            Scanner scan = new Scanner(fil);
            int lineCount = 0;
            while (scan.hasNext()){     
                
                String linje = scan.nextLine();
                String []tegn = linje.split("\\s+");    //returns array without any spaces ec.
                
                for (int i = 0;i<tegn.length;i++){      //kjører gjennom alle tegn i en rad.
                    char bokstav = tegn[i].charAt(0);
                   

                    // if not a "collisionblock", check if it is a obj.
                   if (Character.isLetter(bokstav)) {
                        if  (map[panel.mapH.activeMapType][panel.mapH.activeMapNumber][lineCount][i] == null){
                            String [] objectInfo = tegn[i].split(",");

                            if (objectInfo.length == 2){
                                map[panel.mapH.activeMapType][panel.mapH.activeMapNumber][lineCount][i]= factory.createGameObject(i*32,lineCount*32,objectInfo[0],objectInfo[1],0,0,1,1);
                            } else if (objectInfo.length >2){
                                map[panel.mapH.activeMapType][panel.mapH.activeMapNumber][lineCount][i]= factory.createGameObject(i*32,lineCount*32,objectInfo[0],objectInfo[1],Integer.parseInt(objectInfo[2])*32,Integer.parseInt(objectInfo[3])*32,Integer.parseInt(objectInfo[4]),Integer.parseInt(objectInfo[5]));
                            }
                        }
                        
                    // if not object
                    } else {
                        if (map[panel.mapH.activeMapType][panel.mapH.activeMapNumber][lineCount][i]== null){
                            map[panel.mapH.activeMapType][panel.mapH.activeMapNumber][lineCount][i]= null;
                        }
                    }
                }
                //after one entire loop, linecount is increased by one
                lineCount ++;
            }
            scan.close();
        } catch (Exception e) { e.printStackTrace(); System.out.println("Feil! readObjects()");}
    }


    

    public void draw(Graphics2D g2){
        int worldCol = 0;
        int worldRow = 0;
        
        while (worldCol < panel.tileM.maxWorldCol && worldRow < panel.tileM.maxWorldRow){
            
            //finner posisjon på skjermen som tilesene skal tegnes. 
            int screenX = (worldCol * tileSize)-(panel.spiller.worldX)+panel.spiller.screenX;
            int screenY = (worldRow * tileSize)-(panel.spiller.worldY)+panel.spiller.screenY;

            

            //tegner bare tiles som blir brukt:
            if ((screenY + tileSize*5 > 0 && screenX+tileSize*5> 0)&&((screenY - tileSize*2 < panel.frame.getBounds().getHeight())&&(screenX - tileSize*1 < panel.frame.getBounds().getWidth()))){
                SuperObject o = map[panel.mapH.activeMapType][panel.mapH.activeMapNumber][worldRow][worldCol];   //tegner ut aktiv map sitt objekt.
                
                
                if (o != null){    //hvis faktisk objekt
                   

                    drawObject(o,screenX,screenY,g2);
                }

                
                


                if (tempObject!= null){
                    SuperObject ob = tempObject;
                    int tempscreenX = (ob.worldX)-(panel.spiller.worldX)+panel.spiller.screenX;
                    int tempscreenY = (ob.worldY)-(panel.spiller.worldY)+panel.spiller.screenY;
                    g2.drawImage(ob.image,tempscreenX-ob.pixlePlusXvalue,tempscreenY-ob.pixlePlusYvalue,null);
                }
                    
                    //endring fra 32 til tilesize
                if ((((worldCol-1)*panel.tileSize< panel.spiller.worldX )&&(worldCol+1)*panel.tileSize> panel.spiller.worldX) && ((( worldRow-1) *panel.tileSize <panel.spiller.worldY)&&((worldRow+1) *panel.tileSize > panel.spiller.worldY))){
                    panel.spiller.draw(g2);
                }
                
            }
           //endrer col og row for å loope gjennom alle tiles alle steder.
            worldCol ++;
            if (worldCol >= panel.tileM.maxWorldCol){
                worldCol = 0;
                worldRow++;
            }
        }
    }

    private void drawObject(SuperObject o,int screenX,int screenY,Graphics2D g2){     //tar seg av seg å tegne hver enkelt tile. brukes i draw(). funker fint med animasjoner
        g2.drawImage(o.image,screenX-o.pixlePlusXvalue*2,screenY-o.pixlePlusYvalue*2,null);
       // g2.fillRect(screenX,screenY,o.hitBox.width,o.hitBox.height);

        //
        //
        //if (t == panel.objM.tempTile){
       
         //   g2.drawImage(panel.objM.tempTileImage,screenX-2,screenY-2,null);
        //}

        //int SX = o.hitBox.x -(panel.spiller.worldX)+panel.spiller.screenX;
        //int SY = o.hitBox.y -(panel.spiller.worldY)+panel.spiller.screenY;
        //g2.drawString(Integer.toString(SX)+","+Integer.toString(SX),50,50);
        //g2.fillRect(SX,SY,o.hitBox.width,o.hitBox.height);

        //System.out.println(SX+","+SY+","+o.hitBox.x+";"+o.hitBox.y);
    }


    public boolean getObjInteractable(String ret){
        if (isObject(ret)){
            return getObject(ret).interactable;
        }
        return false;
    }


    public boolean getObjInteractable(){
        if (isObject()){return getObject().interactable;}
        return false;
    }

    public boolean getObjCollison(String ret){
        if (isObject(ret)){
            
        if (!getObject(ret).directionCollision.equals("null")){
            if (!getObject(ret).directionCollision.equals(ret)){
                return false;
                }
           }
            
            
        }

        if (isObject(ret)){return getObject(ret).collision;}
        return false;
    }

    public boolean getObjCollisionNPC(int worldX,int worldY,String retning){
        if (isObject(worldX,worldY,retning)){return getObject(worldX,worldY,retning).collision;}
        return false;
    }

    public SuperObject getObject(String ret){
        int x = panel.spiller.worldX;
        int y = panel.spiller.worldY;
        int tileX = x/tileSize;
        int tileY = y/tileSize;

        if (ret.equals("opp")){tileY-=1;}
        if (ret.equals("ned")){tileY+=1;}
        if (ret.equals("hoyre")){tileX+=1;}
        if (ret.equals("venstre")){tileX-=1;}

        return (map[panel.mapH.activeMapType][panel.mapH.activeMapNumber][tileY][tileX]);
    }

    public SuperObject getObject(int worldX,int worldY,String retning){
        
        int tileX = worldX/tileSize;
        int tileY = worldY/tileSize;
        
        if (retning.equals("opp")){tileY-=1;}
        if (retning.equals("ned")){tileY+=1;}
        if (retning.equals("hoyre")){tileX+=1;}
        if (retning.equals("venstre")){tileX-=1;}

        if (tileY <0||tileY>panel.mapH.mapHeight||tileX<0||tileX>panel.mapH.mapWidth){
            return null;
        }

        if (map[panel.mapH.activeMapType][panel.mapH.activeMapNumber][tileY][tileX] != null){
            return (map[panel.mapH.activeMapType][panel.mapH.activeMapNumber][tileY][tileX]);
        }
        return  null;
    }

    public SuperObject getObject(){
        int x = panel.spiller.worldX;
        int y = panel.spiller.worldY;
        int tileX = x/tileSize;
        int tileY = y/tileSize;
        
        return map[panel.mapH.activeMapType][panel.mapH.activeMapNumber][tileY][tileX];
    }

    public SuperObject getObjectTileXY(int tileX,int tileY){
        
        if (tileY <0||tileY>panel.mapH.mapHeight||tileX<0||tileY>panel.mapH.mapWidth){
            return null;
        }

        if (map[panel.mapH.activeMapType][panel.mapH.activeMapNumber][tileY][tileX] != null){
            return map[panel.mapH.activeMapType][panel.mapH.activeMapNumber][tileY][tileX];
        }
        return  null;
    }

    public boolean isObject(String ret){
        int x = panel.spiller.worldX;
        int y = panel.spiller.worldY;
        int tileX = x/tileSize;
        int tileY = y/tileSize;
        
        if (ret == "opp") { 
            tileY-=1;
        } else if (ret == "ned") { 
            tileY+=1;
        } else if (ret == "hoyre") { 
            tileX+=1;
        } else if (ret == "venstre") {
            tileX-=1;}

        return (map[panel.mapH.activeMapType][panel.mapH.activeMapNumber][tileY][tileX]!= null);
    }

    public boolean isObject(int worldX,int worldY,String ret){
        
        int tileX = getUpdatetTileX(ret,worldX);
        int tileY = getUpdatetTileY(ret,worldY);
        return (map[panel.mapH.activeMapType][panel.mapH.activeMapNumber][tileY][tileX]!= null);
    }

    public boolean isObject(int worldX,int worldY){
        
        int tileX = worldX/32;
        int tileY = worldY/32;
        return (map[panel.mapH.activeMapType][panel.mapH.activeMapNumber][tileY][tileX]!= null);
    }


    public boolean isObject(){
        int x = panel.spiller.worldX;
        int y = panel.spiller.worldY;
        int tileX = x/tileSize;
        int tileY = y/tileSize;
        
        return (map[panel.mapH.activeMapType][panel.mapH.activeMapNumber][tileY][tileX]!= null);
        
    }
    public void removeObject(){
        map[panel.mapH.activeMapType][panel.mapH.activeMapNumber][getUpdatetPlayerTileY()][getUpdatetplayerTileX()] = null;
    }

    public void objectChange(SuperObject newObj){
        
        int x = panel.spiller.worldX;
        int y = panel.spiller.worldY;
        int tileX = x/panel.tileSize;
        int tileY = y/panel.tileSize;
       
        if (panel.spiller.retning.equals("opp")){tileY-=1;}
        if (panel.spiller.retning.equals("ned")){tileY+=1;}
        if (panel.spiller.retning.equals("hoyre")){tileX+=1;}
        if (panel.spiller.retning.equals("venstre")){tileX-=1;}

        map[panel.mapH.activeMapType][panel.mapH.activeMapNumber][tileY][tileX] = newObj;
    }



    public void placeOrinaryObject(String type,String name){
        
        int x = panel.spiller.worldX;
        int y = panel.spiller.worldY;
        int tileX = x/panel.tileSize;
        int tileY = y/panel.tileSize;
       
        if (panel.spiller.retning.equals("opp")){tileY-=1;}
        if (panel.spiller.retning.equals("ned")){tileY+=1;}
        if (panel.spiller.retning.equals("hoyre")){tileX+=1;}
        if (panel.spiller.retning.equals("venstre")){tileX-=1;}

        SuperObject o = factory.createGameObject(tileX*32,tileY*32,name,type,0,0,1,1); 
        if (map[panel.mapH.activeMapType][panel.mapH.activeMapNumber][tileY][tileX] == null){
            map[panel.mapH.activeMapType][panel.mapH.activeMapNumber][tileY][tileX] = o;

            if (o instanceof Material){
                updateNeighbors(tileX,tileY,name);
            } 
        }
    }
    
    public boolean placeOrinaryObjectMouse(String type,String name,int x,int y){
        int tileX =  getNearest32(x)/32;
        int tileY =  getNearest32(y)/32;
        
        SuperObject o = factory.createGameObject(tileX*32,tileY*32,name,type,0,0,1,1); 

        if (map[panel.mapH.activeMapType][panel.mapH.activeMapNumber][tileY][tileX] == null){
            
            map[panel.mapH.activeMapType][panel.mapH.activeMapNumber][tileY][tileX] = o;
           

            if (o instanceof Material){
                updateNeighbors(tileX,tileY,name);
            } 
            return true;
        }
        return false;
    }
   

    int getObjValue(int tileX,int tileY,String name){

        //return the objValue of an objekt. can be used to updating.
        boolean oppe = false;
        boolean hoyre = false;
        boolean nede = false;
        boolean venstre = false;
        
        if (getObject(tileX*tileSize,tileY*tileSize,"opp")!= null){
            if (getObject(tileX*tileSize,tileY*tileSize,"opp").name.equals(name)){ oppe = true;}
        }
       
        if (getObject(tileX*tileSize,tileY*tileSize,"ned")!= null){
            if (getObject(tileX*tileSize,tileY*tileSize,"ned").name.equals(name)){ nede = true;}
        }
        
        if (getObject(tileX*tileSize,tileY*tileSize,"hoyre")!= null){
            if (getObject(tileX*tileSize,tileY*tileSize,"hoyre").name.equals(name)){ hoyre = true;}
        }

        if (getObject(tileX*tileSize,tileY*tileSize,"venstre")!= null){
            if (getObject(tileX*tileSize,tileY*tileSize,"venstre").name.equals(name)){ venstre = true;}
        } else {System.out.println("venstre = null");}
        

         //16 alternativer
        if (!oppe && hoyre && nede && !venstre){return 1;}

        if (!oppe && hoyre && nede && venstre){return 2;}

        if (!oppe && !hoyre && nede && venstre){return 3;}

        if (oppe && hoyre && nede && !venstre){return 4;}

        if (oppe && hoyre && nede && venstre){return 5;}

        if (oppe && !hoyre && nede && venstre){return 6;}

        if (oppe && hoyre && !nede && !venstre){return 7;}

        if (oppe && hoyre && !nede && venstre){return 8;}

        if (oppe && !hoyre && !nede && venstre){return 9;}

        if (oppe && !hoyre && nede && !venstre){return 12;}
        if (oppe && !hoyre && !nede && !venstre){return 11;}
        if (!oppe && !hoyre && nede && !venstre){return 13;}

        if (!oppe && hoyre && !nede && !venstre){return 21;}
        if (!oppe && !hoyre && !nede && venstre){return 23;}
        if (!oppe && hoyre && !nede && venstre){return 22;}

        if (!oppe && !hoyre && !nede && !venstre){return 30;}

        //returnerer null hvis ingen tilfeller stemmer
        return 0;
    }


    private void updateNeighbors(int tileX,int tileY,String objectName){
        
        updateNeighborSprite(tileX-1,tileY,objectName);

        updateNeighborSprite(tileX+1,tileY,objectName);

        updateNeighborSprite(tileX,tileY-1,objectName);

        updateNeighborSprite(tileX,tileY+1,objectName);

    }

    private void updateNeighborSprite(int tileX,int tileY,String objectName){
        if (isNotOutOfBounds(tileX,tileY)){
            if (map[panel.mapH.activeMapType][panel.mapH.activeMapNumber][tileY][tileX] != null){
                if (getObjectTileXY(tileX,tileY).name.equals(objectName)){
                    getObjectTileXY(tileX,tileY).changeImage(getObjValue(tileX,tileY,objectName));
                }
            }
        }
    }
   
    private boolean isNotOutOfBounds(int x,int y){
        if (x >= 0  && x <=panel.mapH.mapWidth && y >= 0 && y <=panel.mapH.mapHeight){
            return true;
        } else {
            return false;
        }
    }

    public SuperObject getObj(int x,int y){
        int tileX =  getNearest32(x)/32;
        int tileY =  getNearest32(y)/32;

        if (isObject(tileX*32,tileY*32)){
            return map[panel.mapH.activeMapType][panel.mapH.activeMapNumber][tileY][tileX];
        }
        else return null;
        
    }

    public int getNearest32(int num) {
        int remainder = num % 32;
        if (remainder <= 16) {
            return num - remainder;
        } else {
            return num + (32 - remainder);
        }
    }


    //help functions:

        //all this should be removedw
        public int getUpdatetplayerTileX(){
            int tileX = panel.spiller.worldX/tileSize;
    
            if (panel.spiller.retning.equals("hoyre")){tileX+=1;}
            if (panel.spiller.retning.equals("venstre")){tileX-=1;}
            return tileX;
        }
    
        public int getUpdatetPlayerTileY(){
            int tileY = panel.spiller.worldY/tileSize;
    
            if (panel.spiller.retning.equals("opp")){tileY-=1;}
            if (panel.spiller.retning.equals("ned")){tileY+=1;}
            return tileY;
        }
    
        public int getUpdatetTileY(String retning,int worldY){
           
            int tileY = worldY/tileSize;
    
            if (retning.equals("opp")){tileY-=1;}
            if (retning.equals("ned")){tileY+=1;}
            
            return tileY;
        }
    
        public int getUpdatetTileX(String retning,int worldX){
            int tileX = worldX/tileSize;
    
            if (retning.equals("hoyre")){tileX+=1;}
            if (retning.equals("venstre")){tileX-=1;}
            return tileX;
        }
}



