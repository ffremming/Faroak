package resources.presentation.ui;

import resources.app.GamePanel;
import resources.domain.tile.Tile;
import resources.domain.inventory.Inventory;
import resources.domain.inventory.Item;
import resources.domain.inventory.Stack;
import resources.domain.inventory.ItemManager;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import resources.app.GamePanel;

public class Button extends Component{
    String text;
    int pressValue = 0;
    private Runnable action;


    public Button(GamePanel panel,String text) {
        super(panel);
        this.text = text;
        visible = true;
        width = 100;
        height = 50;
        borderSize = 5;
        foreground = Color.white;
        background = Color.red;
    }

    // need image - 2 or three versions per

    //neds to have interaction

    /** Set the action to run when the button is clicked. */
    public void onClick(Runnable action) {
        this.action = action;
    }

    public void press(){
        pressValue = 10;
        if (action != null) action.run();
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

        setWidth(container.width-30);
        setHeight(width/4);

        if (hover){
            setBackground(new Color(160,160,160));
            setForeGround(Color.black);
        } else {
            setBackground(Color.gray);
            setForeGround(Color.black);
        }
       
        drawRect(g2);
        g2.setColor(foreground);
        g2.drawString(text,(int)(x+(width/2)-(text.length()*3.5)),y+(height/2));
    }

    public void mouseMoved(MouseEvent e){
        hover();
    }

    public void addActionListener( ){
        
    }
}
