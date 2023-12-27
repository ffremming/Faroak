package ressurser.drawing;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;

import ressurser.baseEntity.Sprite;
import ressurser.main.GamePanel;
import ressurser.main.UI;


//my new idea is to instead of draw every sprite alone, paste them all on a singular image.
public class DrawingManager {

    public boolean drawingTimer = false;

    //for drawing right FPS in panel:
    public int FPS = 1;
    public long splitTime = 1000000000/FPS;
    public long nestetid = System.nanoTime()+splitTime;

    GamePanel panel;
    public  DrawingManager(GamePanel panel){
        this.panel = panel;
    }

    public void draw(Graphics g){
        
        long startDraw = System.nanoTime();
       
        Graphics2D g2 = (Graphics2D)g;
        
        panel.tileM.draw(g2);

        //should make a combined entity and object drawing function
        panel.objM.draw(g2);
        panel.entityH.drawAllEntities(g2);
        panel.light.DrawNightSky(g2,panel.enviromentM.lightLevel());
        panel.UI.draw(g2);
        
        g2.setFont(new Font("Arial",Font.PLAIN,12));

        long endDraw = System.nanoTime();
        if (drawingTimer){
            long passed = endDraw - startDraw;
            g2.setFont(new Font("Arial",Font.PLAIN,40));
            g2.setColor(Color.white);
            g2.drawString("draw string:"+String.format("&s5",passed),20,100);
            
        }

        if (panel.gameState == panel.MENUSTATE){
            panel.menuStateUI.draw(g2);
        }
        g2.dispose();
    }

    public void draw2(Graphics g){

        
       
        Graphics2D g2 = (Graphics2D)g;
        
        
       


        
        g2.dispose();
    }

    public void drawRelative(Sprite sprite,Graphics2D g2){
        //g2.drawImage()

        int tempScreenX = (sprite.worldX)-(panel.spiller.worldX)+panel.spiller.screenX;
        int tempScreenY = (sprite.worldY)-(panel.spiller.worldY)+panel.spiller.screenY;

        g2.drawImage(sprite.getImage(),tempScreenX,tempScreenY,sprite.width,sprite.height,null);     //can remove width and height.
    }
}
