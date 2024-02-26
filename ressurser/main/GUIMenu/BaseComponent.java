package ressurser.main.GUIMenu;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.image.BufferedImage;

import ressurser.main.GamePanel;

public abstract class BaseComponent extends Rectangle {
    //everything tat can be placed inside a container
    
    Color background = Color.white;
    Color foreground = Color.black;
    boolean enabled;
    boolean visible;
    boolean focus;
    boolean hover;

    GamePanel panel;

    public int padding;
    int borderSize;
    int margin;

    public BaseComponent(GamePanel panel){
        this.panel = panel;
    }

    public abstract void draw(Graphics2D g2);
    

    public void setY(int newY){
        y = newY;
    }

    public void setX(int newX){
        x = newX;
    }

    public void setHeight(int newHeight){
        height = newHeight;
    }

    public void setWidth(int newWidth){
        width = newWidth;
    }

    public void setBounds(int x,int y,int height,int width){
        this.x = x;
        this.y = y;
        this.height = height;
        this.width = width;
    }

    public void enable(){
        enabled = true;
    }

    public void disable(){
        enabled = false;
    }

    public void setVisible(boolean bol){
        visible = bol;
    }

    public void setBackground(Color color){
        background = color;
    }

    public void setForeGround(Color color){
        foreground = color;
    }


    public void drawRect(Graphics2D g2){
        g2.setColor(background);
        g2.fillRect(x,y,width,height);
        g2.setColor(foreground);
        g2.setStroke(new BasicStroke(borderSize));
        g2.drawRect(x,y,width,height);
    }

    public void setHover(boolean bol){
        hover = bol;
    }
}
