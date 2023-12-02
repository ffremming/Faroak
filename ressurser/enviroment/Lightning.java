package ressurser.enviroment;

import ressurser.main.GamePanel;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.RadialGradientPaint;
import java.awt.image.BufferedImage;
import java.awt.AlphaComposite;


public class Lightning {
    RadialGradientPaint gradient;
    
    GamePanel panel;
    Point2D center;
    int radius;
    
    
    public Lightning(GamePanel panel){
        this.panel = panel;
        
        
            
        
            
        
        
    }

   


    public void DrawPlayerLightning(Graphics2D g2){
        

        g2.setPaint(gradient);
        g2.fillRect(0, 0, panel.screenWidth, panel.screenHeight);
       
        
    }

    public void DrawNightSky(Graphics2D g2,int verdi){
        if (panel.spiller.activeTool.equals("hammer")){
            int delverdi = verdi/5;
            Color[] colors = {  new Color(0, 2, 20, 0) , new Color(0, 2, 20, delverdi*1),new Color(0, 2, 20, delverdi*3),new Color(0, 2, 20, delverdi*4),new Color(0, 2, 20, delverdi*5)};
            float[] fractions = { 0.0f,0.25f,0.5f,0.75f, 1.0f };
            int radius = 200; 

            center = new Point2D.Float(panel.spiller.screenX+panel.tileSize/2, panel.spiller.screenY+panel.tileSize/2);
            gradient = new RadialGradientPaint(center, radius, fractions, colors);

            g2.setPaint(gradient);
            g2.fillRect(0,0,panel.screenWidth,panel.screenHeight);
        } else {
            g2.setColor(new Color(0,2,20,verdi));
            //g2.setPaint(gradient);
            g2.fillRect(0, 0, panel.screenWidth, panel.screenHeight);
        }
        
        

    }

    public void newWithHoles(){
            // Create a BufferedImage with an alpha channel
            BufferedImage filter = new BufferedImage(panel.screenWidth, panel.screenHeight, BufferedImage.TYPE_INT_ARGB);

            // Fill the BufferedImage with a semi-transparent black color
            Graphics2D g2d = filter.createGraphics();
            g2d.setColor(new Color(0, 0, 0, 128));
            g2d.fillRect(0, 0, panel.screenWidth, panel.screenHeight);
            g2d.dispose();

            // Draw the "holes" in the filter
            //Graphics2D g2d = (Graphics2D) image.getGraphics();
            g2d.drawImage(filter, 0, 0, null); // draw the filter on top of the background
            g2d.setColor(Color.BLACK);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR));
            //g2d.setComposite(AlphaComposite.Clear);
            g2d.fillOval(50, 50, 100, 100);
            g2d.fillOval(200, 200, 150, 75);
            g2d.fillOval(400, 100, 75, 150);
            g2d.dispose();
            



    }

}
