package ressurser.main.GUIMenu;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

import ressurser.main.GamePanel;

public class BaseMenu extends Container{

    public BaseMenu(GamePanel panel) {
        super(panel);
        height = 300;
        width = 200;

        BufferedImage b= null,bP = null,bH = null;
        try {
            String folderName = "buttons";
            b = ImageIO.read(getClass().getResourceAsStream(folderName+"/Button1.png"));
            bP = ImageIO.read(getClass().getResourceAsStream(folderName+"/Button1P.png"));
            bH = ImageIO.read(getClass().getResourceAsStream(folderName+"/Button1H.png"));
            
            
           
        } catch (IOException e) {
            e.printStackTrace();
        }



        setLayout(STACK);
        setBackground(Color.white); 
        setForeGround(Color.black);
        
        Label sub = new Label(panel,"menu");
        sub.setHeight(40);sub.setWidth(100);
        Label option = new Label(panel,"option");
        option.setHeight(40);option.setWidth(100);
        Label quit = new Label(panel,"quit");
        quit.setHeight(40);quit.setWidth(100);
        
        Button button = new Button(panel);
        button.setImages(b,bH,bP);
        button.setWidth(120);
        button.setHeight(50);

        add(sub);
        add(option);
        add(quit);
        add(button);
        
    }

    @Override
    public 
    void setCorrectedBounds(){
        setBounds((int)panel.getFrameWidth()/2-width/2,panel.getFrameHeight()/2-height/2,height,width);
    }
    
}
