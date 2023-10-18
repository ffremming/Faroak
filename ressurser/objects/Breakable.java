package ressurser.objects;

import ressurser.main.GamePanel;

public class Breakable extends InteractableObj{

    public Breakable(GamePanel gp, ObjectManager objM, int worldX, int worldY, String name, String type) {
        super(gp, objM, worldX, worldY, name, type);
        durability = 10;
    }

    @Override
    public void interact() {
        durability--;

        if (durability<= 0){
            smash();
        }
    }

    @Override
    public void smash() {
       durability --;

        if (durability <= 0){
            //connected cells disappear
    

            //this cell erased
            objM.map [panel.mapH.activeMapType][panel.mapH.activeMapNumber][worldY/32][worldX/32] = null;
        }
    }
}
