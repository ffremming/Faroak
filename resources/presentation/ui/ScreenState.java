package resources.presentation.ui;

import resources.app.GamePanel;
import resources.domain.tile.Tile;
import resources.domain.inventory.Inventory;
import resources.domain.inventory.Item;
import resources.domain.inventory.Stack;
import resources.domain.inventory.ItemManager;

import java.awt.Graphics2D;
import java.awt.MouseInfo;
import java.awt.PointerInfo;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;

import resources.app.GamePanel;

public class ScreenState extends BaseComponent{

    
    PointerInfo pointerI;
    
   
    ArrayList<Container> containers = new ArrayList<>();

    public ScreenState(GamePanel panel) {
        super(panel);
        
    }

    public void add(Container container){
        containers.add(container);
    }

    public boolean remove(Container container){
        return containers.remove(container);
    }
    
    @Override
    public void draw(Graphics2D g2) {
       if (visible){

        for (Container con: containers){
            con.draw(g2);
        }
        
       }
    }

    

    public void mousePressed(MouseEvent e){
        int xEvent = e.getX();
        int yEvent = e.getY();

        for (Container con:containers){
            
            //if in the containter
            if (xEvent >= con.x && xEvent<=con.x+con.width){
                if (yEvent >= con.y && yEvent<=con.y+con.height){
                    con.mousePressed(e);
                }
            }
        }
    }

    public void mouseMoved(MouseEvent e){
        int xEvent = e.getX();
        int yEvent = e.getY();
       // System.out.println(e.getX()+","+e.getY()+"Screen hovered");
        
        for (Container con:containers){
            
            //if in the containter
            if (xEvent >= con.x && xEvent<=con.x+con.width){
                if (yEvent >= con.y && yEvent<=con.y+con.height){
                    con.mouseMoved(e);
                }
            }
        }
    }

    public void mouseWheelMoved(MouseWheelEvent e){
        int xEvent = e.getX();
        int yEvent = e.getY();
       // System.out.println(e.getX()+","+e.getY()+"Screen hovered");
        
        for (Container con:containers){
            
            //if in the containter
            if (xEvent >= con.x && xEvent<=con.x+con.width){
                if (yEvent >= con.y && yEvent<=con.y+con.height){
                    con.mouseWheelMoved(e);
                }

            }
        }
    }
    

    public  void closeScreenState(){

    }

    
    
}
