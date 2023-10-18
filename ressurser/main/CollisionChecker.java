package ressurser.main;

import ressurser.Tiles.TileManager;
import ressurser.baseEntity.HitBox;
import ressurser.entity.Entity;
import ressurser.entity.EntityHandler;
import ressurser.entity.NPC.NPC;
import ressurser.entity.spiller.Spiller;
import ressurser.objects.ObjectManager;
import ressurser.objects.SuperObject;

public class CollisionChecker {
    GamePanel panel;
    TileManager tileM;
    ObjectManager objM;
    Spiller spiller;
    EntityHandler entityH;
    NPC collisionNPC;

    public CollisionChecker(GamePanel panel){
        this.panel = panel;
        this.tileM = panel.tileM;
        this.objM = panel.objM;
        this.spiller = panel.spiller;
        
    }

    private boolean collision(Entity entity,SuperObject so){
        //System.out.println(entity.hitBox.x +"n"+so.worldX);
        return (entity.collision(so.hitBox));
    }


    public boolean collisionAllEntities(Entity entity){
        for (int  x = panel.spiller.worldX/panel.tileSize -5;x< panel.spiller.worldX/panel.tileSize+5;x++){
            for (int y  = panel.spiller.worldY/panel.tileSize -5;y< panel.spiller.worldX/panel.tileSize+5;y++){
                System.out.println(panel.spiller.worldX/64+","+panel.spiller.worldY/64 +"/"+x+","+y);
                if (panel.objM.map[0][0][y][x] != null){
                    if (collision(entity,panel.objM.map[0][0][y][x])){
                        return true;
                    }
                }
            }
        }
        return false;
    }




    public boolean playerCollision(){   //spiller får bevege seh hvis denne ikke er sann.
        int x = spiller.worldX;
        int y = spiller.worldY;
        String r = spiller.retning;

        if (spiller.cheat){
            return false;
        }

        if (spiller.boat){
            if (!checkOutOfBounds(x,y,r)){
                if (!objM.getObjCollison(spiller.retning)){
                    if (!checkCollisionEntities(x,y,r)){
                        if (panel.tileM.getTile(x,y,r).water){
                            return false;
                        }
                    }
                }
            }
        } else {
        //sjekker om den ikke er på vei ut av map.
        if (!checkOutOfBounds(x,y,r)){
            if (!objM.getObjCollison(spiller.retning)){
                if (!checkCollisionEntities(x,y,r)){
                    if (!panel.tileM.getTile(x,y,r).collision||!waterCollision()){
                        return false;
                    } else {}
                }    else{}
            }else{ }
        
        }
        return true;
        }
        return true;
    }


    private boolean waterCollision(){
        if (spiller.boat){
            return !panel.tileM.getTile(spiller.worldX,spiller.worldY,spiller.retning).water;
        }
        else return true;
        
    }
    

    public boolean checkOutOfBounds(int worldX,int worldY,String ret){  //true hvis out of bounds
        
            int tileX = worldX/panel.tileSize;
            int tileY = worldY/panel.tileSize;

            if (ret.equals("opp")){tileY-=1;}
            if (ret.equals("ned")){tileY+=1;}
            if (ret.equals("hoyre")){tileX+=1;}
            if (ret.equals("venstre")){tileX-=1;}
    
            return ( (tileX <0 ||tileX >= tileM.maxWorldCol||tileY<0 ||tileY >= tileM.maxWorldRow));
    }

    public boolean checkCollisionEntities(int worldX,int worldY,String ret){
        if (entityH == null){
            entityH = panel.entityH;
        }

        int tileX = worldX/panel.tileSize;
        int tileY = worldY/panel.tileSize;

        if (ret.equals("opp")){tileY-=1;}
        if (ret.equals("ned")){tileY+=1;}
        if (ret.equals("hoyre")){tileX+=1;}
        if (ret.equals("venstre")){tileX-=1;}


        for (int i = 0;i<10;i++){
            if (entityH.npc[panel.mapH.activeMapType][panel.mapH.activeMapNumber][i] != null){
                int entityX= (entityH.npc[panel.mapH.activeMapType][panel.mapH.activeMapNumber][i]).worldX/32;
                int entityY= (entityH.npc[panel.mapH.activeMapType][panel.mapH.activeMapNumber][i]).worldY/32;
                
                if (entityX == tileX && entityY == tileY){
                    collisionNPC = entityH.npc[panel.mapH.activeMapType][panel.mapH.activeMapNumber][i];
                    return true;
                }
            }
        }
        return false;
    }
    public NPC getNPCCollided(){
        return collisionNPC;
    }

    public boolean NPCCollidedPLayer(int x,int y,String ret){
        int tileX = x/panel.tileSize;
            int tileY = y/panel.tileSize;

            if (ret.equals("opp")){tileY-=1;}
            if (ret.equals("ned")){tileY+=1;}
            if (ret.equals("hoyre")){tileX+=1;}
            if (ret.equals("venstre")){tileX-=1;}
    
        return (spiller.worldX/32== tileX && spiller.worldY/32== tileY);
    }
    
}
