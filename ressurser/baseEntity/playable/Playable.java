package ressurser.baseEntity.playable;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import ressurser.baseEntity.Entity;
import ressurser.baseEntity.Vector;
import ressurser.main.GamePanel;

public class Playable extends Entity {

    Vector velocity = new Vector();
    ArrayList<BufferedImage> images = new ArrayList<>();

    public Playable(GamePanel panel, String name, int worldX, int worldY, short width, short height, short hitBoxWidth,
            short hitBoxHeight, short relativeXPLus, short relativeYPlus) {
        super(panel, name, worldX, worldY, width, height, hitBoxWidth, hitBoxHeight, relativeXPLus, relativeYPlus);
        //TODO Auto-generated constructor stub
    }

    public void move() {
        worldX += velocity.transferX(5);
        worldX += velocity.transferY(5);
    }

    public void setVelocity(Vector newVector){
        velocity.add(newVector);
    }

    public void hentBilde(){
        try{

           
        opp1 = ImageIO.read(getClass().getResourceAsStream("playerSprites/playerStandingBack1.png"));
        opp2 = ImageIO.read(getClass().getResourceAsStream("playerSprites/playerBackNew2.png"));
        opp3 = ImageIO.read(getClass().getResourceAsStream("playerSprites/playerBackNew3.png"));


        ned1 = ImageIO.read(getClass().getResourceAsStream("playerSprites/playerDown.png"));
        ned2 = ImageIO.read(getClass().getResourceAsStream("playerSprites/playerDown2.png"));
        ned3 = ImageIO.read(getClass().getResourceAsStream("playerSprites/playerDown3.png"));

        venstre1 = ImageIO.read(getClass().getResourceAsStream("playerSprites/playerLeftStanding1.png"));
        venstre2 = ImageIO.read(getClass().getResourceAsStream("playerSprites/revidertPlayerLeft2.png"));
        venstre3 = ImageIO.read(getClass().getResourceAsStream("playerSprites/revidertPlayerLeft3.png"));

        hoyre1 = ImageIO.read(getClass().getResourceAsStream("playerSprites/standingRight1.png"));
        hoyre2 = ImageIO.read(getClass().getResourceAsStream("playerSprites/playerRight2.png"));
        hoyre3 = ImageIO.read(getClass().getResourceAsStream("playerSprites/playerRight3.png"));

        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setImages(){
        images = panel.imageContainer.setPlayableImages(getName());
    }

    
}
