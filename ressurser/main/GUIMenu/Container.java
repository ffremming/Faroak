package ressurser.main.GUIMenu;

import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;

import ressurser.main.GamePanel;

    


public class Container extends BaseComponent{

    final protected int STACK = 1;
    final protected int INVENTORY = 2;
    int layout;

    //INVENTORY
    int rows;
    int cols;
    int slotWidth;
    int slotHeight;


    public Container(GamePanel panel) {
        super(panel);
        
    }

    ArrayList<Component> content = new ArrayList<>();


    public void add(Component comp){
        content.add(comp);
        comp.setContainer(this);
    }

    public boolean remove(Component comp){
        return content.remove(comp);
    }

    public void draw(Graphics2D g2){
        //avhenger av hvordan jeg vil gjør dette

        //has to change to look better:
        
        if (layout == STACK){

            int compY = y + 20;
            drawRect(g2);

            for (Component comp:content){
                
                comp.setX(x+width/2-comp.width/2);
                comp.setY(compY);
                comp.draw(g2);
                compY += (int)comp.height+comp.height/2;
            }
        }

         else if (layout == INVENTORY){


            int startX = x +10;
            int startY = y +10;

            int compY = y +10;
            int compX = x + 10;
            
            drawRect(g2);
            int counter = 0;

            for (Component comp:content){

                comp.setX(compX);
                comp.setY(compY);
                comp.draw(g2);

                if (counter <cols-1){
                   
                    compX += comp.width;
                } else {
                    compX = startX;
                    compY += comp.height;
                    counter = -1;
                }

                counter ++;
            }
        }
        
        
    }
    /**
     * 1: Stack
     * 2: Inventory
     */
    void setLayout(int type){
        layout = type;
    }

    public void setCorrectedBounds() {
    }

    public void mousePressed(MouseEvent e){

        int xEvent = e.getX();
        int yEvent = e.getY();

        //System.out.println("container pressed");
        for (Component comp:content){

            //if in the component
            
            if (xEvent >= comp.x && xEvent<=comp.x+comp.width){
                if (yEvent >= comp.y && yEvent<=comp.y+comp.height){

                    comp.mousePressed(e);
                }
            }
        }
    }

    public void mouseMoved(MouseEvent e){

        hover = true;
        int xEvent = e.getX();
        int yEvent = e.getY();

        
        for (Component comp:content){
            
            //if in the component
            if (xEvent >= comp.x && xEvent<=comp.x+comp.width && yEvent >= comp.y && yEvent<=comp.y+comp.height){
                    comp.mouseMoved(e);
            } else {
                comp.hover = false;
            }
        }
    }

    public void mouseWheelMoved(MouseWheelEvent e){
       
        int xEvent = e.getX();
        int yEvent = e.getY();

        //Some containers wwant to move here, others, might need to move the components.
        
        for (Component comp:content){
            
            //if in the component
            if (xEvent >= comp.x && xEvent<=comp.x+comp.width && yEvent >= comp.y && yEvent<=comp.y+comp.height){
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
