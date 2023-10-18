package ressurser.main;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import ressurser.main.interactions.InventoryInteraction;
import ressurser.main.interactions.MenuInteraction;
import ressurser.main.interactions.OptionInteraction;
import ressurser.main.interactions.PlayInteractionManager;

public class KeyHandler implements KeyListener{
    private GamePanel gp;
    private PlayInteractionManager interactionP;
    private InventoryInteraction interactionI;
    private MenuInteraction interactionM;
    private OptionInteraction interactionO;
    

    public KeyHandler(GamePanel gp){
        this.gp = gp;
        this.interactionP = gp.interactionPlay;
        this.interactionO = gp.interactionOption;
        this.interactionI = gp.interactionInventory;
        this.interactionM = gp.interactionMenu;
    }

    
    public boolean upPressed;
    public boolean downPressed;
    public boolean leftPressed;
    public boolean rightPressed;
    public boolean enterPressed;
    public boolean mPressed;
    boolean pPressed;
    int mVerdi = 0;
    boolean enterBlock = false;
    
    @Override
    public void keyTyped(KeyEvent e) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void keyPressed(KeyEvent e) {
        
        if (gp.gameState == gp.PLAYSTATE){
            interactionP.keyPressed(e);
        }

        enterPressed = false;
        // TODO Auto-generated method stub
        int code = e.getKeyCode();

        if (code == KeyEvent.VK_W){
            
            interactionP.upPressed();} 
            

        if (code == KeyEvent.VK_A){interactionP.leftPressed();}

        if (code == KeyEvent.VK_S){interactionP.downPressed();}
            
        if (code == KeyEvent.VK_D){interactionP.rightPressed();}

        if (code == KeyEvent.VK_ENTER){interactionP.enterPressed();}
            

        if (code == KeyEvent.VK_P){
            
               if (gp.gameState == gp.MENUSTATE){
                gp.gameState = gp.PLAYSTATE;
               } else {
                gp.gameState = gp.MENUSTATE;
               }
        }

        if (code == KeyEvent.VK_M){interactionP.mPressed();}
        
        if (code == KeyEvent.VK_SHIFT){interactionP.shiftPressed();}


        //drawingTimer
        if (code == KeyEvent.VK_T){gp.drawingM.drawingTimer = true;}



        if (code == KeyEvent.VK_1){interactionP.onePressed();}
        if (code == KeyEvent.VK_2){interactionP.twoPressed();}
        if (code == KeyEvent.VK_3){interactionP.threePressed();}
        if (code == KeyEvent.VK_4){interactionP.fourPressed();}
        if (code == KeyEvent.VK_5){interactionP.fivePressed();}
        if (code == KeyEvent.VK_6){interactionP.sixPressed();}
        if (code == KeyEvent.VK_7){interactionP.sevenPressed();}
        if (code == KeyEvent.VK_8){interactionP.eightPressed();}
        if (code == KeyEvent.VK_9){interactionP.ninePressed();}
    }

    @Override
    public void keyReleased(KeyEvent e) {

        if (gp.gameState == gp.PLAYSTATE){
            interactionP.keyReleased(e);
        }
       

        int code = e.getKeyCode();

        if (code == KeyEvent.VK_W){upPressed = false;} 

        if (code == KeyEvent.VK_A){leftPressed = false;}

        if (code == KeyEvent.VK_S){downPressed = false;}
            
        if (code == KeyEvent.VK_D){rightPressed = false;}
        
        if (code == KeyEvent.VK_ENTER){enterPressed = false;}

        if (code == KeyEvent.VK_M){mPressed = false;}

        if (code == KeyEvent.VK_P){pPressed = false;}

        if (code == KeyEvent.VK_T){gp.drawingM.drawingTimer = false;}

    }
    
}
