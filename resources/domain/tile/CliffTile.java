package resources.domain.tile;

import resources.app.GamePanel;
import resources.domain.entity.BaseEntity;
import resources.geometry.HitBox;
import resources.presentation.image.ImageContainer;

import java.util.ArrayList;
import java.awt.image.BufferedImage;



import resources.app.GamePanel;

public class CliffTile extends Tile{

    public CliffTile(GamePanel panel, String name, int worldX, int worldY, int altitude) {
        super(panel, name, worldX, worldY, altitude);
        //TODO Auto-generatesd constructor stub
    }
    @Override
    public boolean hasCompleteNeigbors(){
        return (north !=null && south!=null && west!= null && east!=null);
    }


    public ArrayList<BufferedImage> getImages(){

        

        if (images.size()== 0){
            setImages();
           
        }
        return images;
    }

    private void setImages(){

        int value = 0;
        images.clear();
        images.add(getImage());
       
        if (!(neigbors[0] instanceof CliffTile) && neigbors[3] instanceof CliffTile &&neigbors[1] instanceof CliffTile ) {
            value = 2;
            //UP
        } else if(((neigbors[0] instanceof CliffTile) && neigbors[1] instanceof CliffTile )){
            //CORNER DOWN LEFT
            value = 7;

        } else if ((neigbors[0] instanceof CliffTile) && neigbors[3] instanceof CliffTile  ){
            value = 9;
            //CORNER DOWNRIGHT
        } else if ((neigbors[2] instanceof CliffTile) && neigbors[1] instanceof CliffTile  ){
            value = 1;
            //CORNER UPPERLEFT
        }else if ((neigbors[2] instanceof CliffTile) && neigbors[3] instanceof CliffTile  ){
            value = 3;
            //CORNER UPPERRIGHT
        }else if((!(neigbors[2] instanceof CliffTile) && neigbors[1] instanceof CliffTile && neigbors[3] instanceof CliffTile )){
            // DOWN 
            value = 8;

        } else if((!(neigbors[3] instanceof CliffTile) && neigbors[0] instanceof CliffTile && neigbors[2] instanceof CliffTile )){
            // LEFT
            value = 4;

        } else if((!(neigbors[1] instanceof CliffTile) && neigbors[0] instanceof CliffTile && neigbors[2] instanceof CliffTile )){
            // RIGHT
            value = 6;
        }
        System.out.println(value);
        images.add(panel.imageContainer.getTileImage("cliff"+value));
    }
}
