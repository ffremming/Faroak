package ressurser.objects;

import ressurser.main.GamePanel;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

public class Material extends Yieldable implements Changeable{

    BufferedImage image1,image2,image3,image4,image5,image6,image7,image8,image9,image10,image11,image12,image13,image14,image15,image16;

    public Material(GamePanel gp, ObjectManager objM,int worldX,int worldY,String name ,String type) {
        super(gp, objM,worldX,worldY,name,type);
        this.name = name;
        yieldType = name;
        hammerBreakable = true;
        
        
    }

    @Override
    public void interact() {
       smash();

    }

    @Override
    public void smash(){
        objM.removeObject();
        panel.menu.materials.addItem(name);
    }


    @Override
    protected void readImages(){
        try {
            image1 = ImageIO.read(getClass().getResourceAsStream("objectSprites/material/"+name+"/"+name+"1"+".png"));
       
            image2 = ImageIO.read(getClass().getResourceAsStream("objectSprites/material/"+name+"/"+name+2+".png"));
            image3 = ImageIO.read(getClass().getResourceAsStream("objectSprites/material/"+name+"/"+name+3+".png"));
            image4 = ImageIO.read(getClass().getResourceAsStream("objectSprites/material/"+name+"/"+name +4+".png"));
            image5 = ImageIO.read(getClass().getResourceAsStream("objectSprites/material/"+name+"/"+name +5+".png"));
            image6 = ImageIO.read(getClass().getResourceAsStream("objectSprites/material/"+name+"/"+name+ 6+".png"));
       
            image7 = ImageIO.read(getClass().getResourceAsStream("objectSprites/material/"+name+"/"+name+7+".png"));
            image8 = ImageIO.read(getClass().getResourceAsStream("objectSprites/material/"+name+"/"+name+8+".png"));
            image9 = ImageIO.read(getClass().getResourceAsStream("objectSprites/material/"+name+"/"+name +9+".png"));
            image10 = ImageIO.read(getClass().getResourceAsStream("objectSprites/material/"+name+"/"+name +11+".png"));
            image11 = ImageIO.read(getClass().getResourceAsStream("objectSprites/material/"+name+"/"+name+"12"+".png"));
       
            image12 = ImageIO.read(getClass().getResourceAsStream("objectSprites/material/"+name+"/"+name+13+".png"));
            image13 = ImageIO.read(getClass().getResourceAsStream("objectSprites/material/"+name+"/"+name+21+".png"));
            image14 = ImageIO.read(getClass().getResourceAsStream("objectSprites/material/"+name+"/"+name +22+".png"));
            image15 = ImageIO.read(getClass().getResourceAsStream("objectSprites/material/"+name+"/"+name +23+".png"));
            image16 = ImageIO.read(getClass().getResourceAsStream("objectSprites/material/"+name+"/"+name+"30"+".png"));
            
            image = image16;

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void changeImage(int i) {
        switch (i){
            
        case (1): image = image1;break;
            
        case (2):image = image2;break;

        case (3):image = image3;break;

        case (4):image = image4;break;

        case (5):image = image5;break;

        case (6):image = image6;break;

        case (7):image = image7;break;

        case (8):image = image8;break;

        case (9):image = image9;break;

        case (11):image = image10;break;

        case (12):image = image11;break;

        case (13):image = image12;break;

        case (21):image = image13;break;

        case (22):image = image14;break;

        case (23):image = image15;break;

        case (30):image = image16;break;
        }

    }


    
}
