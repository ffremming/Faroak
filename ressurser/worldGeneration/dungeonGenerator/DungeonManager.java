package ressurser.worldGeneration.dungeonGenerator;

import java.util.ArrayList;
import java.util.Random;

import ressurser.main.GamePanel;

public class DungeonManager {
    
    int dungeonsGenerated = 0;

    ArrayList<Dungeon> dungeons = new ArrayList<>();
    public DungeonManager(GamePanel panel){
        
    }

    public Dungeon getDungeon(){
        Random rand = new Random();

        
        int amountRooms = rand.nextInt(1,30);
        int height = rand.nextInt(70,250);
        int width = rand.nextInt(70,250);
        System.out.println(height+";"+amountRooms);
        Dungeon dung = new Dungeon(height,height,amountRooms);

        //idk if i need this:
        dungeonsGenerated++;
        return dung;
    }
}
