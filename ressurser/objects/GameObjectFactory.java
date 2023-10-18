package ressurser.objects;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;

import javax.imageio.ImageIO;

import ressurser.main.GamePanel;

public class GameObjectFactory {
    
    GamePanel panel;
    ObjectManager objM;
    HashMap <String,BufferedImage> objectSprites= new HashMap<>();

    public GameObjectFactory(GamePanel panel,ObjectManager objM){
        this.panel = panel;
        this.objM = objM;
    }
        //method takes in x,y and string. 
        //string defines what object is created and returned.
        //if just a ordinary object, return 


    private void spriteSetup(String name){
        if (!objM.objectSprites.containsKey(name)){
            try {
                System.out.println(name);
                BufferedImage newObjectImage = ImageIO.read(getClass().getResourceAsStream("objectSprites/superObjects/"+name+".png"));
                
                
                int SCALE = 2;
                BufferedImage bi = new BufferedImage(SCALE * newObjectImage.getWidth(null), SCALE
                * newObjectImage.getHeight(null), BufferedImage.TYPE_INT_ARGB);

                Graphics2D grph = (Graphics2D) bi.getGraphics();
                grph.scale(SCALE, SCALE);

                grph.drawImage(newObjectImage, 0, 0, null);
                grph.dispose();

                newObjectImage = bi;

                

                objM.objectSprites.put(name,newObjectImage);
        } catch (Exception e){
            System.out.println("fault");
        }



        }
    }



    public SuperObject createGameObject(int worldX,int worldY,String name,String type,int xPlus,int yPlus,int width,int height) {
        //SuperObject o;

        spriteSetup(name);

       
        if (type.startsWith("cliff")){
            Cliff o = new Cliff(panel, objM, worldX, worldY,name,type);
            ///o.pixlePlusYvalue = 10;
            
            o.readImages();
            return o;
        
        }
        //teleport:
        if (type.startsWith("Teleport")){
            Teleport o = new Teleport(panel, objM, worldX, worldY,55,55,name,type);
            
            o.readImages();
            return o;
        
        }
        if (type.startsWith("ladderBack")){
        
            LadderBack o = new LadderBack(panel, objM, worldX, worldY,panel.spiller.worldX, panel.spiller.worldY,name,type);
            o.readImages();
            return o;
    
        }

        if (type.startsWith("dungeonTeleport")){
            
            DungeonCave o = new DungeonCave(panel, objM, worldX, worldY,name,type);
            o.readImages();
            return o;

        }
        if (type.startsWith("farmable")){
            FirePlant o = new FirePlant(panel,objM,worldX,worldY,name,type);
            o.readImages();
            return o;
        }


        if (type.startsWith("nonCI")){
            NonCollisionInteraction o = new NonCollisionInteraction(panel,objM,worldX,worldY,name,type);
            o.readImages();
            return o;
        }

        if (type.startsWith("woodYield")){
            WoodYield o = new WoodYield(panel, objM, worldX, worldY,name,type,xPlus,yPlus);
            o.readImages();
            o.setConnectedCells(width,height);
            return o;
        }
        
        if (type.startsWith("ghost")){
            SuperObject o = new SuperObject(panel, objM, worldX, worldY,name,type);
            return o;
        
        }
        
        else if (type.startsWith("stone")){
            Stone o = new Stone(panel, objM, worldX, worldY,name,type);
            o.readImages();
            return o;
        }else if (type.startsWith("material")){
            Material o = new Material(panel, objM, worldY, worldX,name,type);
            o.readImages();
            o.changeImage(objM.getObjValue(worldX/32,worldY/32,name));
            return o;



        } else {
            SuperObject o = new SuperObject(panel, objM,worldX, worldY,name,type,width,height);
            o.readImages();
            o.pixlePlusXvalue = xPlus;
            o.pixlePlusYvalue = yPlus;
            return o;
        }
       
        
    }
}
