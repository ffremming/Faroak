package ressurser.baseEntity.tile;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import ressurser.baseEntity.BaseEntity;
import ressurser.baseEntity.gameObjects.GameObject;
import ressurser.baseEntity.sprite.Sprite;
import ressurser.main.GamePanel;
import ressurser.main.ImageContainer;

public class Tile extends BaseEntity{
    
    final int NORTH = 0;
    final int SOUTH = 2;
    final int WEST = 3;
    final int EAST = 1;



    Tile [] neigbors = new Tile [4];
    public Tile north;
    Tile south;
    Tile east;
    Tile west;

    int zone;

    public Sprite sprite;

    //midlertidig
    public BufferedImage image;
    ArrayList<BufferedImage> images = new ArrayList<>();

    int altitude;



    public Tile(GamePanel panel,String name, int worldX, int worldY,int altitude) {
        super(panel, name, worldX, worldY, (short)panel.tileSize,(short)panel.tileSize, (short)panel.tileSize, (short)panel.tileSize, (short)0, (short)0);
        
        this.altitude = altitude;
        if (name.equals("ocean")){
            animated = true;
            solid = true;
        }
        
    }

    public boolean compareTo(Tile tile2){
        return this.getName() == tile2.getName();
    }

    @Override
    public void animate(int value){
        setAnimatedImages(value);
        
    }

    /**value can be set between 0 and 2 */
    private BufferedImage getAnimatedImage(int value){
        
        BufferedImage image = null;
        image = panel.imageContainer.getTileImage(getName()+value);
        if (image == null){
            if (panel.camera.testData){
                System.out.println("animation of "+getName()+" went wrong - value:"+value);
            }
            
            // TODO Auto-generated catch block
            image = panel.imageContainer.getTileImage(getName());
        }
            
        
        return image;
    }
    /**sets the main(backgroudn tile image to another version. ) */
    private void setAnimatedImage(int value){
        try{
            images.set(0,getAnimatedImage(value));
        } catch(IndexOutOfBoundsException e){
            images.add(getAnimatedImage(value));
        }
        
    }

    /**sets all images based on animation cycle  */
    private void setAnimatedImages(int value){

            images.clear();
            setAnimatedImage(value);
            boolean[] borders = {false,false,false,false};
            for (int i = 0;i<4;i++){
                Tile neightbor = getNeighbors()[i];
                if (neightbor != null){
                    if (neightbor.isHigherthan(this)){
    
                        
                        if (value == 0 ||(!ImageContainer.doesPNGFileExist("tile/"+neightbor.getName()+1+"B"+1))){
                            images.add(panel.imageContainer.getTileImage(neightbor.getName()+"B"+i));
                        }else{
                            images.add(panel.imageContainer.getTileImage(neightbor.getName()+value+"B"+i));
                        }
                       
                        
                        borders[i] = true;
                        //images.add(panel.imageContainer.getImage(neightbor.getName()+"C"+i));
                    }
                    
                }
    
            }
            if (value == 0 )
            {if ((borders[0] && borders[1]  ) &&(getNeighbors()[0].getName() == getNeighbors()[1].getName() )){
                images.add(panel.imageContainer.getTileImage(getNeighbors()[0].getName()+"C"+1));
            }if ((borders[1] && borders[2] ) &&(getNeighbors()[1].getName() == getNeighbors()[2].getName() )){
                images.add(panel.imageContainer.getTileImage(getNeighbors()[1].getName()+"C"+2));
            }if ((borders[2] && borders[3] ) &&(getNeighbors()[2].getName() == getNeighbors()[3].getName() )){
                images.add(panel.imageContainer.getTileImage(getNeighbors()[2].getName()+"C"+3));
            }if ((borders[3] && borders[0] ) &&(getNeighbors()[3].getName() == getNeighbors()[0].getName() )){
                images.add(panel.imageContainer.getTileImage(getNeighbors()[3].getName()+"C"+4));
            }

            }else{

                if ((borders[0] && borders[1] ) &&(getNeighbors()[0].getName() == getNeighbors()[1].getName() )){

                    //there is corner - find out if the file exist.
                    if (ImageContainer.doesPNGFileExist("tile/"+getNeighbors()[0].getName()+1+"C"+0)){
                        images.add(panel.imageContainer.getTileImage(getNeighbors()[0].getName()+value+"C"+1));
                    } else{
                        images.add(panel.imageContainer.getTileImage(getNeighbors()[0].getName()+"C"+1));
                    }

                    
                }
        
                if ((borders[1] && borders[2] ) &&(getNeighbors()[1].getName() == getNeighbors()[2].getName() )){

                    //there is corner - find out if the file exist.
                    if (ImageContainer.doesPNGFileExist("tile/"+getNeighbors()[1].getName()+1+"C"+0)){
                        images.add(panel.imageContainer.getTileImage(getNeighbors()[1].getName()+value+"C"+2));
                    } else{
                        images.add(panel.imageContainer.getTileImage(getNeighbors()[1].getName()+"C"+2));
                    }

                    
                }
        
                if ((borders[2] && borders[3] ) &&(getNeighbors()[2].getName() == getNeighbors()[3].getName() )){
                    //there is corner - find out if the file exist.
                    if (ImageContainer.doesPNGFileExist("tile/"+getNeighbors()[2].getName()+1+"C"+0)){
                        images.add(panel.imageContainer.getTileImage(getNeighbors()[2].getName()+value+"C"+3));
                    } else{
                        images.add(panel.imageContainer.getTileImage(getNeighbors()[2].getName()+"C"+3));
                    }
                }
        
                if ((borders[3] && borders[0] ) &&(getNeighbors()[3].getName() == getNeighbors()[0].getName() )){
                    //there is corner - find out if the file exist.
                    if (ImageContainer.doesPNGFileExist("tile/"+getNeighbors()[3].getName()+1+"C"+0)){
                        images.add(panel.imageContainer.getTileImage(getNeighbors()[3].getName()+value+"C"+4));
                    } else{
                        images.add(panel.imageContainer.getTileImage(getNeighbors()[3].getName()+"C"+4));
                    }
                }
            }
           
    }

    private void setImages(){
        images.add(getImage());
        boolean[] borders = {false,false,false,false};
        for (int i = 0;i<4;i++){
            Tile neightbor = getNeighbors()[i];
            if (neightbor != null){
                if (neightbor.isHigherthan(this)){

                    //unsure if this will work.
                    images.add(panel.imageContainer.getTileImage(neightbor.getName()+"B"+i));
                    
                    borders[i] = true;
                    //images.add(panel.imageContainer.getImage(neightbor.getName()+"C"+i));
                }
                
            }

        }
            
        if ((borders[0] && borders[1] ) &&(getNeighbors()[0].getName() == getNeighbors()[1].getName() )){
            images.add(panel.imageContainer.getTileImage(getNeighbors()[0].getName()+"C"+1));
        }

        if ((borders[1] && borders[2] ) &&(getNeighbors()[1].getName() == getNeighbors()[2].getName() )){
            images.add(panel.imageContainer.getTileImage(getNeighbors()[1].getName()+"C"+2));
        }

        if ((borders[2] && borders[3] ) &&(getNeighbors()[2].getName() == getNeighbors()[3].getName() )){
            images.add(panel.imageContainer.getTileImage(getNeighbors()[2].getName()+"C"+3));
        }

        if ((borders[3] && borders[0] ) &&(getNeighbors()[3].getName() == getNeighbors()[0].getName() )){
            images.add(panel.imageContainer.getTileImage(getNeighbors()[3].getName()+"C"+4));
        }

        
    

    }

    /**returns true if tile is higher than given tile */
    private boolean isHigherthan(Tile tile) {
        return panel.tileM.isHigher(tile,this);
    }

    public ArrayList<BufferedImage> getImages(){
        if (images.size()== 0){
            setImages();
        }
        return images;
    
    }


    

    // not sure if i need this.
    public void addSprite(Sprite sprite){
        this.sprite = sprite;
    }


    //idea that every tile contain gameobject, but not implementet. should not be done yet.
    //well, could work for static objects, but not so good with non-statics. would not do this.
    public void addGameObject(GameObject go){
        
    }

    /**@return null - if direction is given is wrong OR there is no TILE */
    private Tile getTile(int direction){
        if (direction == NORTH){
            return panel.chunkSystem.getTile(new Point(worldX+width/2,worldY-width/2));
        }
        else if (direction == EAST){
            return panel.chunkSystem.getTile(new Point(worldX+width*3/2,worldY+width/2));
        }
        else if (direction == SOUTH){
            return panel.chunkSystem.getTile(new Point(worldX+width/2,worldY+width*3/2));
        }
        else if (direction == WEST){
            return panel.chunkSystem.getTile(new Point(worldX-width/2,worldY+width/2));
        }

        return null;
    }




    /**
     * not yet implemented, implement if this will be used.
     */
    public void setNeighBors(){
        addNorthNeighBor(getTile(NORTH));
        addSouthNeighBor(getTile(SOUTH));
        addWestNeighBor(getTile(WEST));
        addEastNeighBor(getTile(EAST));
    }
    public Tile[] getNeighbors(){
        Tile[] neighbors ={north,east,south,west};
        return neighbors;
    }


    private void addNorthNeighBor(Tile tile){
        north = tile;
    }
    private void addSouthNeighBor(Tile tile){
        south = tile;
    }
    private void addWestNeighBor(Tile tile){
        west = tile;
    }
    private void addEastNeighBor(Tile tile){
        east = tile;
    }

    

    public void setAnimated(Boolean boolean1) {
        animated = boolean1;
    }

    public void setZone(Integer thisZone) {
        zone = thisZone;
    }



    
}
