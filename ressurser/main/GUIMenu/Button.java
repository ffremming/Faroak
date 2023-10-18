package ressurser.main.GUIMenu;

import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import ressurser.main.GamePanel;

public class Button extends Component{
    
    public Button(GamePanel panel) {
        super(panel);
       
    }
    BufferedImage image1, image2,image3;
    // need image - 2 or three versions per

    //neds to have interaction

    public void press(){
        activeImage = image3;
    }

    public void hover(){
        hover = true;
        activeImage = image2;
    }
    
    public void setImages(BufferedImage newImage1,BufferedImage newImage2,BufferedImage newImage3){
        image1 = newImage1;
        image2 = newImage2;
        image3 = newImage3;
        activeImage = image1;
    }

    public void mousePressed(MouseEvent e){
        //nothing
        System.out.println("button pressed");
        press();
    }
    
    public void draw(Graphics2D g2){
        if (hover){
            activeImage = image2;
        }else {
            activeImage = image1;
        }
        g2.drawImage(activeImage,x,y,width,height,null);
    }

    public void mouseMoved(MouseEvent e){
        hover();
    }
}
