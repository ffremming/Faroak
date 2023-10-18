package ressurser.main.GUIMenu;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import ressurser.main.GamePanel;

public class Label extends Component{

    String text;
    
    

    public Label(GamePanel panel,String text) {
        super(panel);
        this.text = text;
        background = Color.white;
        foreGround = Color.black;

       
    }
    public Label(GamePanel panel,BufferedImage image) {
        super(panel);
       activeImage = image;
    }

    @Override
    public void draw(Graphics2D g2) {
        drawRect(g2);
        g2.setColor(foreGround);
        g2.drawString(text,x+width/2-text.length(),y+height/2+5);
    }
}
