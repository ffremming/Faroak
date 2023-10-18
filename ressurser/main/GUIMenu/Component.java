package ressurser.main.GUIMenu;

import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import ressurser.main.GamePanel;
import ressurser.meny.items.Item;

public class Component extends BaseComponent{

    Container container = null;

    public Component(GamePanel panel) {
        super(panel);
        setHeight(40);setWidth(100);
        //TODO Auto-generated constructor stub
    }

    @Override
    public void draw(Graphics2D g2) {
        
    }

    
    public void draw(Graphics2D g2, int x,int y) {
        
    }

    public void setContainer(Container thisContainer){
        this.container = thisContainer;
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
   
    
    
}
