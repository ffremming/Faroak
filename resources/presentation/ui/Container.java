package ressurser.main.GUIMenu;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.awt.BasicStroke;
import java.awt.Color;

import ressurser.main.GamePanel;

    


public class Container extends Component{

    //INVENTORY
    int rows;
    int cols;

    ArrayList<Component> content = new ArrayList<>();
    public int border;

    public Container(GamePanel panel) {
        super(panel);
        
    }
    public Container(GamePanel panel,int x,int y) {
        super(panel);
        this.x = x;
        this.y = y;
    }

    public Container(GamePanel panel,int x,int y,int cols,int rows) {
        super(panel);
        this.x = x;
        this.y = y;
        this.cols = cols;
        this.rows = rows;
    }

    public void add(ItemContainerSlot comp){
        if (content.size()== 0){
            //comp.y = y+padding;
            //comp.x = x+padding;

        } else {
            Component previous = content.get(content.size()-1);
            //comp.y = y+previous.y+previous.height + padding;
            //comp.x = x + padding;
        }
        content.add(comp);
        comp.setContainer(this);
    }

    /** for adding UI elements */
    public void add(Component comp){
        content.add(comp);
        comp.setContainer(this);
    }



    public boolean remove(Component comp){
        return content.remove(comp);
    }

    public void draw(Graphics2D g2){
        
        if (visible){
            drawRect(g2);
           
          
           int count = 0;
            for (Component comp:content){
                
                if (comp instanceof ItemContainerSlot){
                    ((ItemContainerSlot)comp).draw(g2);
                   

                } else{
                    if (rows!= 0 && cols != 0){
                        comp.x = x +((width/2)-(comp.width/2));
                        if (count!= 0){
                            comp.y = content.get(count-1).y +content.get(count-1).height + padding; 
                        } else {
                            comp.y = y + comp.height/(content.size());
                        }
                        
                        System.out.println("draw container4");
                        comp.draw(g2);
                        count ++;
                    } else {
                        comp.draw(g2);
                    }
                    
                }
                
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
        setHover(true);
        for (Component comp:content){
            if (comp.contains(new Point(xEvent,yEvent))){
                comp.setHover(true);
                comp.mouseMoved(e);
            } else {
                comp.setHover(false);
                comp.hover = false;
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

    public void mouseDragged(MouseEvent e) {
       
    }

    public void mouseClicked(MouseEvent e) {
       
    }

    @Override
    public void setHover(boolean bol){
        hover = bol;
        for (Component comp:content){
            comp.setHover(bol);
        }
    }

    public Point getCenter() {
        int centerX = x + width / 2;
        int centerY = y + height / 2;
        return new Point(centerX, centerY);
    }
    
}
