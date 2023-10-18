package ressurser.enviroment;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import ressurser.main.GamePanel;

import java.awt.geom.Rectangle2D;
import java.awt.geom.Area;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.RadialGradientPaint;

public class LightingFilter {
    private BufferedImage filterImage;
    private Graphics2D filterGraphics;
    private GamePanel panel;
    private int width,height;

    Color[] colors = {  new Color(0, 0, 0, 50) , new Color(0, 0, 0, 100),new Color(0, 0, 0, 150),new Color(0, 0, 0, 200),new Color(0, 0, 0, 250)};
    float[] fractions = { 0.0f,0.25f,0.5f,0.75f, 1.0f };

    public LightingFilter(int width, int height,GamePanel panel) {
        this.width = width;
        this.height = height;
        this.panel = panel;

        
    }

    public void drawImage(Graphics2D g2){
        filterImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = (Graphics2D) filterImage.getGraphics();
        
        Area screenArea = new Area(new Rectangle2D.Double(0,0,width,height));

        int sircleSize = 120;

        double x = panel.spiller.screenX-sircleSize/2;
        double y = panel.spiller.screenY-sircleSize/2;

        //lager formen lyset er i
        Shape circleShape = new Ellipse2D.Double(x,y,sircleSize,sircleSize);

        Shape circleShape2 = new Ellipse2D.Double(panel.spiller.worldX,panel.spiller.worldY,sircleSize,sircleSize);
        Shape circleShape21 = new Ellipse2D.Double(panel.spiller.worldX,panel.spiller.worldY,sircleSize-30,sircleSize-30);
        Shape circleShape22 = new Ellipse2D.Double(panel.spiller.worldX,panel.spiller.worldY,sircleSize-60,sircleSize-60);


        Area lightArea = new Area(circleShape);
        Area lightArea2 = new Area(circleShape2);
        Area lightArea21 = new Area(circleShape21);
        Area lightArea22 = new Area(circleShape22);

        
        
        RadialGradientPaint gradient2 = new RadialGradientPaint(panel.spiller.worldX,panel.spiller.worldY, 120, fractions, colors);
        //g2d.setPaint(gradient2);
        //g2d.fill(lightArea2);
       //g2d.fill(lightArea21);
        //g2d.fill(lightArea22);


        screenArea.subtract(lightArea);
        
        screenArea.subtract(lightArea2);
        RadialGradientPaint gradient = new RadialGradientPaint(panel.spiller.screenX,panel.spiller.screenY, 200, fractions, colors);
        g2d.setPaint(gradient);
        //RadialGradientPaint gradient2 = new RadialGradientPaint(panel.spiller.screenX-100,panel.spiller.screenY-100, 200, fractions, colors);
        //g2d.setPaint(gradient2);
        //g2d.setColor(new Color(0,0,0,0.95f));

        g2d.fill(screenArea);


        lightArea2.subtract(lightArea21);
        lightArea21.subtract(lightArea22);
        g2d.setPaint(gradient2);
        g2d.fill(lightArea2);
        g2d.fill(lightArea21);
        g2d.fill(lightArea22);

        g2d.dispose(); 
        g2.drawImage(filterImage,0,0,null);
    }



    public void drawLightElement(int x, int y, int radius) {
        filterGraphics.setColor(new Color(1f, 1f, 1f, 0.5f)); // set the color to white with alpha 0.5
        filterGraphics.setComposite(AlphaComposite.SrcOver); // set the blending mode to "source over"
        filterGraphics.fillOval(x - radius, y - radius, radius * 2, radius * 2); // draw a filled circle with the given radius
    }

    public BufferedImage getFilteredImage() {
        return filterImage;
    }
}

