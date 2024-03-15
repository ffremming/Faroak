package ressurser.main;

import ressurser.baseEntity.Vector;

public class InputHandlingSystem {
    
    boolean up,left,down,right = false;
    GamePanel panel;


    public InputHandlingSystem(GamePanel panel){
        this.panel = panel;
    }

    public void update(){
        if (up){
            panel.player.addVelocity(new Vector(0,-2));
        }

        if (left){
            panel.player.addVelocity(new Vector(-2,0));
        }
            
        if (down){
            panel.player.addVelocity(new Vector(0,2));
        }

        if (right){
            panel.player.addVelocity(new Vector(2,0));
        }

        //setting the hovered entities from mouse input
        panel.world.setHoveredEntity(panel.mouse.x,panel.mouse.y);
        
        

    }

    public void setDown(boolean val){
        down = val;
    }
    public void setUp(boolean val){
        up = val;
    }
    public void setLeft(boolean val){
        left = val;
    }
    public void setRight(boolean val){
        right = val;
    }
}
