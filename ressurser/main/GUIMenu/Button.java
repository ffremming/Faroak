package ressurser.main.GUIMenu;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import ressurser.main.GamePanel;

public class Button extends Component{
    String text;
    int pressValue = 0;
    
    
    public Button(GamePanel panel,String text) {
        super(panel);
        this.text = text;
        visible = true;
        width = 100;
        height = 50;
        borderSize = 5;
        foreground = Color.white;
        background = Color.lightGray;
    }
   
    // need image - 2 or three versions per

    //neds to have interaction

    public void press(){
        pressValue = 10;
    }

    public void hover(){
        hover = true;
    }
    
    public void setImages(BufferedImage newImage1,BufferedImage newImage2,BufferedImage newImage3){
      
    }

    public void mousePressed(MouseEvent e){
        press();
    }
    
    public void draw(Graphics2D g2){
        if (hover){background = Color.DARK_GRAY;} else {
            background = Color.lightGray;
        }
       
        drawRect(g2);
        g2.setColor(foreground);
        g2.drawString(text,x+width/2-(30-text.length()*2),y+(height/2));
    }

    public void mouseMoved(MouseEvent e){
        hover();
    }

    public void addActionListener( ){
        
    }
}
