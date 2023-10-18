package ressurser.Tiles;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import javax.imageio.ImageIO;
import ressurser.baseEntity.tile.Tile;

import ressurser.main.GamePanel;
public class TileFactory {
    static int teller;

    HashMap<String,BufferedImage> BasicTileSet = new HashMap<String,BufferedImage>();
    HashMap<String,BufferedImage> dungeonTiles = new HashMap<String,BufferedImage>();

    GamePanel panel;

    public TileFactory(GamePanel panel){
        this.panel = panel;
        getBufferedIMages();
        
    }


    private void getBufferedIMages(){
        try{

        BufferedImage grass = ImageIO.read(new File("ressurser/tileSprites/"+"g"+"/"+"ggggg.png"));
        BufferedImage mud = ImageIO.read(new File("ressurser/tileSprites/"+"m"+"/"+"mmmmm.png"));
        BufferedImage moss = ImageIO.read(new File("ressurser/tileSprites/"+"Mo"+"/"+"MoMoMoMoMo.png"));
        BufferedImage sand = ImageIO.read(new File("ressurser/tileSprites/"+"s"+"/"+"sssss.png"));
        BufferedImage water = ImageIO.read(new File("ressurser/tileSprites/"+"w"+"/"+"wwwww.png"));

        BufferedImage wall1 = ImageIO.read(new File("ressurser/tileSprites/dungeon/dungeonWall1.png"));
        BufferedImage wall2 = ImageIO.read(new File("ressurser/tileSprites/dungeon/dungeonWall2.png"));
        BufferedImage wall3 = ImageIO.read(new File("ressurser/tileSprites/dungeon/dungeonWall3.png"));
        BufferedImage wall4 = ImageIO.read(new File("ressurser/tileSprites/dungeon/dungeonWall4.png"));
        BufferedImage wall6 = ImageIO.read(new File("ressurser/tileSprites/dungeon/dungeonWall6.png"));
        BufferedImage wall8 = ImageIO.read(new File("ressurser/tileSprites/dungeon/dungWall8.png"));

        BufferedImage dungWall = ImageIO.read(new File("ressurser/tileSprites/dungeon/dungeonWall.png"));

        BufferedImage dungFloor = ImageIO.read(new File("ressurser/tileSprites/dungeon/dungFloor.png"));
        BufferedImage dungDoor = ImageIO.read(new File("ressurser/tileSprites/dungeon/dungDoor.png"));

        


        dungeonTiles.put("wall1",wall1);
        dungeonTiles.put("wall2",wall2);
        dungeonTiles.put("wall3",wall3);
        dungeonTiles.put("wall4",wall4);
        dungeonTiles.put("wall6",wall6);
        dungeonTiles.put("wall8",wall8);
        dungeonTiles.put("dungFloor",dungFloor);
        dungeonTiles.put("dungDoor",dungDoor);
        dungeonTiles.put("dungWall",dungWall);

        BasicTileSet.put("g",grass);
        BasicTileSet.put("m",mud);
        BasicTileSet.put("M",moss);
        BasicTileSet.put("s",sand);
        BasicTileSet.put("w",water);
        } catch(Exception e){
            e.printStackTrace();

        }
        //BufferedImage waterB = ImageIO.read(new File("tileSprites/"+background+"/"+background+upper+lower+right+left+".png"));
    }

    public BufferedImage getTileSprite(String background,String upper,String lower,String right,String left){
        //System.out.println("ikmage painter"+panel.imageP);

        //System.out.println("tilesprites"+panel.imageP.tileSprites);

        //return panel.imageP.tileSprites.get(background+upper+lower+right+left);
        BufferedImage image;
        
            if (background.equals("W")){
                image = dungeonTiles.get("dungWall");
            } else if (background.equals("F")){
                image = dungeonTiles.get("dungFloor");
            }
            else if (background.equals("W2")){
                image = dungeonTiles.get("wall8");
            } else if (background.equals("F2")){
                image = dungeonTiles.get("m");

            } else if (background.equals("D")){
                image = dungeonTiles.get("dungDoor");
            
                
            } else {
                if (!panel.imageP.tileSprites.containsKey(background+upper+lower+right+left)){
                    panel.imageP.getSpecificHashmap(background,upper,lower,right,left);
                }
                
                image = panel.imageP.tileSprites.get(background+upper+lower+right+left);
            }


            

            return image;
       

        
        
         
        
        /*
        try {



            //System.out.println("tileSprites/"+background+"/"+background+upper+lower+right+left+".png");
            if (background.equals(upper) &&background.equals(lower) && background.equals(right) && background.equals(left)){
                image = BasicTileSet.get(background);

                if (image == null){//System.out.println(background);}
                }
            } else {
                image = ImageIO.read(new File("ressurser/tileSprites/"+background+"/"+background+upper+lower+right+left+".png"));
            }

            return image;

        } catch (IOException e) {
            System.out.println("tileSprites/"+background+"/"+background+upper+lower+right+left+".png");
            e.printStackTrace();
            teller ++;
            System.out.println(teller);
        } */
    }
    

    /*
     
    public Tile createTile(String background,String upper,String lower,String right,String left){

        if (background == null){
            background = "g";
        }

        if (lower== null){
            lower = background;
        }
        if (upper== null){
            upper = background;
        }
        if (right== null){
            right = background;
        }
        if (left== null){
            left = background;
        }
        


        Tile tile = new Tile(background);
        tile.image = getTileSprite(background,upper,lower,right,left);

        if (setCollision(background)){tile.collision = true;}
        setType(background,tile);

       

        return tile;

    }
     */

     //new createTile
    public Tile createTile(String background,int worldX, int worldY){
        
        Tile tile = new Tile(panel,background,worldX,worldY);

        //should be changed to setImage(), but just testing
        tile.image = getTileSprite(background,background,background,background,background);
        return tile;

    }



    private boolean setCollision(String background){
        if (background.equals("W")){
            return true;
        } else if (background.equals("W2")){
            return true;
        } 

        return (background.equals("w"));
    }

    private void setType(String background,Tile t){
        if (background.equals("w")){
            t.water = true;
        } else if (background.equals("g")){
            t.grass = true;
        }
    }
    
}
