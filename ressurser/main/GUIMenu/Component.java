package ressurser.main.GUIMenu;

import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import ressurser.main.GamePanel;
import ressurser.meny.items.Item;
import java.awt.BasicStroke;
import java.awt.Point;

public class Component extends BaseComponent{

    Container container = null;

    public Component(GamePanel panel) {
        super(panel);
       
       
    }

    @Override
    public void draw(Graphics2D g2) {
        g2.setStroke(new BasicStroke(borderSize));;
        if (visible){
            
            drawRect(g2);
        }
    }

    public void setContainer(Container thisContainer){
        container = thisContainer;
    }

    public void mousePressed(MouseEvent e){
        //nothing
    }

    public void mouseMoved(MouseEvent e){
        hover = true;
    }

    public void mouseWheelMoved(MouseWheelEvent e){

    }

    public void mouseReleased(){

    }

    public void mouseDragged(){

    }

    public void addItem(Item item){
       
    }
    public void setPadding(int value){
        padding = value;
    }

    public void center(Point p){
        int newX = p.x - this.width / 2;
        int newY = p.y - this.height / 2;
        this.setLocation(newX, newY);
    }
   
    
    
}
