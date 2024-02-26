package ressurser.main.GUIMenu;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;

import ressurser.main.GamePanel;

    


public class Container extends BaseComponent{

    //INVENTORY
    int rows;
    int cols;
    int slotWidth;
    int slotHeight;

    ArrayList<Component> content = new ArrayList<>();
    public int border;

    public Container(GamePanel panel) {
        super(panel);
    }
    public Container(GamePanel panel,int x,int y,int width,int height) {
        super(panel);
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void add(Component comp){
        if (content.size()== 0){
            comp.y = y+padding;
            comp.x = x+padding;

        } else {
            Component previous = content.get(content.size()-1);
            comp.y = y+previous.y+previous.height + padding;
            comp.x = x + padding;
        }

        content.add(comp);
        comp.setContainer(this);
    }

    public boolean remove(Component comp){
        return content.remove(comp);
    }

    public void draw(Graphics2D g2){
        if (visible){
            drawRect(g2);
            for (Component comp:content){
                comp.draw(g2);
            }
        }
       
    }

    public void mousePressed(MouseEvent e){

        int xEvent = e.getX();
        int yEvent = e.getY();

       
        for (Component comp:content){
            if (comp.contains(new Point(xEvent,yEvent))){
                comp.mousePressed(e);
            }else{
                
            }
        }
    }

    public void mouseMoved(MouseEvent e){

        int xEvent = e.getX();
        int yEvent = e.getY();

        for (Component comp:content){
            if (comp.contains(new Point(xEvent,yEvent))){
                setHover(true);
                comp.mouseMoved(e);
            } else {
                comp.setHover(false);
            }
        }
    }

    public void mouseWheelMoved(MouseWheelEvent e){
       
        int xEvent = e.getX();
        int yEvent = e.getY();

        //Some containers wwant to move here, others, might need to move the components.
        
        for (Component comp:content){
            if (comp.contains(new Point(xEvent,yEvent))){
                    comp.mouseWheelMoved(e);
            } else {
            }
        }
    }
    

    public void mouseReleased(){
        //if an item is chosen, place item on chosen slot
    }

    public void mouseDragged(){
        //items might be spreaded
    }
    
}
