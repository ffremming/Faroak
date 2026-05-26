package resources.domain.tile;

import resources.app.GamePanel;
import resources.domain.entity.BaseEntity;
import resources.geometry.HitBox;
import resources.presentation.image.ImageContainer;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import resources.domain.entity.BaseEntity;
import resources.app.GamePanel;
import resources.presentation.image.ImageContainer;

public class Tile extends BaseEntity{
    
    final int NORTH = 0;
    final int SOUTH = 2;
    final int WEST = 3;
    final int EAST = 1;



    public Tile [] neigbors = new Tile [4];
    public Tile north = null;
    Tile south = null;
    Tile east= null;
    Tile west = null;

  

    //midlertidig
    public BufferedImage image;
    ArrayList<BufferedImage> images = new ArrayList<>();

    int altitude;
    int floor;

    boolean cliff = false;


    public Tile(GamePanel panel,String name, int worldX, int worldY,int altitude) {
        super(panel, name, worldX, worldY, (int)panel.tileSize,(int)panel.tileSize, (int)panel.tileSize, (int)panel.tileSize, (short)0, (short)0);
        

        this.altitude = altitude;
        setup();
    }

    public Tile(GamePanel panel, String name, int worldX, int worldY, int altitude, boolean cliff) {
        super(panel, name, worldX, worldY, (short)panel.tileSize,(short)panel.tileSize, (short)panel.tileSize, (short)panel.tileSize, (short)0, (short)0);
        
        this.altitude = altitude;
        setup();
    }


    private void setup(){
        floor = altitude/300;
        if (getName().equals("ocean")){
            animated = true;
            solid = true;
            lightSource = true;
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
                Tile neighbor = getNeighbors()[i];
                if (neighbor != null && this != null){
                    if (neighbor.isHigherthan(this)){
    
                        
                        if (value == 0 ||(!ImageContainer.doesPNGFileExist("tile/"+neighbor.getName()+value+"B"+1))){
                            images.add(panel.imageContainer.getTileImage(neighbor.getName()+"B"+i));
                        }else{
                            images.add(panel.imageContainer.getTileImage(neighbor.getName()+value+"B"+i));
                        }
                       
                     
                        borders[i] = true;
                        //images.add(panel.imageContainer.getImage(neighbor.getName()+"C"+i));
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

    /**returns true if tile is higher than given tile */
    private boolean isHigherthan(Tile tile) {
        return panel.tileM.isHigher(tile,this);
    }

    private boolean isCliffDifference(Tile tile) {
        return panel.tileM.cliffDifference(tile,this);
    }

    @Override
    public ArrayList<BufferedImage> getImages(){
        if (images.size()== 0){
            setAnimatedImages(0);
        }
        return images;
    }


    

    


    

    /**@return null - if direction is given is wrong OR there is no TILE */
    private Tile getTile(int direction){
        Tile tile = null;
        if (direction == NORTH){
             tile = panel.world.getTile(new Point((int)worldX+width/2,(int)worldY-width/2));
        }
        else if (direction == EAST){
             tile = panel.world.getTile(new Point((int)worldX+width*3/2,(int)worldY+width/2));
        }
        else if (direction == SOUTH){
             tile = panel.world.getTile(new Point((int)worldX+width/2,(int)worldY+width*3/2));
        }
        else if (direction == WEST){
             tile = panel.world.getTile(new Point((int)worldX-width/2,(int)worldY+width/2));
        }
        
        return tile;
    }




    /**
     * trouble with cliffTile
     */
    public void setNeighBors(){
      
        if (isConnected()){return;}
        addNorthNeighBor(getTile(NORTH));
        addSouthNeighBor(getTile(SOUTH));
        addWestNeighBor(getTile(WEST));
        addEastNeighBor(getTile(EAST));

        intiateCliff();
    }
    private boolean isConnected(){
        return (north!=null);
    }

    private void intiateCliff() {
       if (isCliff()){
        cliff = true;
       }
    }

    private boolean isCliff() {

        for (int i = 0;i<4;i++){
            Tile neighbor = getNeighbors()[i];
            if (neighbor != null){

                //litt usikker på logikken her:
                if (neighbor.floor <floor && neighbor.floor>=0){
                    
                    return true;
                }
            }
        }

        return false;
    }

    public boolean hasCompleteNeigbors(){
        return (north !=null && south!=null && west!= null && east!=null);
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

    




    @Override
    public String toString(){
        return "class" +getClass()+"\nname: "+getName()+"\nsolid: " + solid+"\nanimated: "+animated + "\nlightSource: " + lightSource
        + "\naltitude: "+altitude + "\nfloor: "+floor +  "\ncliff: "+cliff+
        "\nnorth: "+north.getName()+ "\neast: "+east.getName() + "\nsouth: "+south.getName() + "\nwest: "+west.getName()
       
        ;

    }
    
}
