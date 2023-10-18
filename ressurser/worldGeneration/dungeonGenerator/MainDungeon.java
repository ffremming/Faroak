package ressurser.worldGeneration.dungeonGenerator;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

import javax.imageio.ImageIO;

public class MainDungeon {
    public static void main(String[] args) {

        Random rand = new Random();

        
        int amountRooms = rand.nextInt(1,36);

        int height = rand.nextInt(70,200);
        //int width = rand.nextInt(70,200);

        
        Dungeon dung = new Dungeon(height,height,amountRooms);



    }
}
