package ressurser.objects;

import java.io.IOException;

import javax.imageio.ImageIO;

import ressurser.main.GamePanel;

public class Farmable extends Yieldable implements Changeable, Ageable{
    private int age = 0;
    protected java.awt.image.BufferedImage image2,image3,image4,image1;
    final int  maxAge = 3;
   
   

    public Farmable(GamePanel gp, ObjectManager objM,int tileX,int tileY,String name,String type) {
        super(gp, objM,tileX,tileY,name,type);
        pixlePlusYvalue = 16;
        resourceYield = true;
        yieldType = name;
        durability= 0;
        

    }
    protected void readImages(){

        try {

            System.out.println("objectSprites/plantable/"+name+1+".png");
            image1 = ImageIO.read(getClass().getResourceAsStream("objectSprites/plantable/"+name+"/"+name+1+".png"));
            image2 = ImageIO.read(getClass().getResourceAsStream("objectSprites/plantable/"+name+"/"+name+2+".png"));
            image3 = ImageIO.read(getClass().getResourceAsStream("objectSprites/plantable/"+name+"/"+name +3+".png"));
            image4 = ImageIO.read(getClass().getResourceAsStream("objectSprites/plantable/"+name+"/"+name +4+".png"));

            image = image1;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void enterInteract() {
        panel.gameState = panel.DIALOGSTATE;
        panel.textBox = true;
        if (age<maxAge){
           
            panel.textString = "looks like the plant need som time";
            
        } else {
            panel.textString = "you harvested a plant!";
            //panel.menu.resources.addItem(yieldType);
            
        }
        
    }

    public void enterContinueInteraction() {
        panel.textBox = false;
        panel.gameState = panel.PLAYSTATE ;
        smash();
    }


    public void age(){
        if (age < maxAge){
            age++;
            changeImage(age);
        }
    }

    @Override
    public void changeImage(int i) {
        if (age == 0){
            image = image1;
        } else if (age == 1){
            image = image2;
        } else if(age == 2){
            image = image3;
        } else if(age == 3){
            image = image4;
        } 
    }
}
