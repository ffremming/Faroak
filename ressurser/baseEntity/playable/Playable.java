package ressurser.baseEntity.playable;

import ressurser.baseEntity.BaseEntity;
import ressurser.baseEntity.gameObject.GameObject;
import ressurser.baseEntity.playable.Inventory.Inventory;
import ressurser.baseEntity.playable.Inventory.Item;
import ressurser.baseEntity.playable.Inventory.Stack;
import ressurser.main.GamePanel;


public class Playable extends Moveable{

    
    private static final String BaseEntity = null;
    Inventory inventory;
    Stack equipped;
    private Stack tempInHand = null;

    public Playable(GamePanel panel, String name, int worldX, int worldY, short width, short height, short hitBoxWidth,
            short hitBoxHeight, short relativeXPLus, short relativeYPlus) {
        super(panel, name, worldX, worldY, width, height, hitBoxWidth, hitBoxHeight, relativeXPLus, relativeYPlus);

        inventory = new Inventory(null);
        panel.userInterface.clear();
        panel.userInterface.addContainer(inventory);


        Item hammer = new Item(panel,"hammer");
        
        addItem(hammer);
        Item house = new Item(panel,"demoHouse");
        addItem(house);
        equipped = inventory.getStack(inventory.getIndex());
        System.out.println(inventory);
    }

    public void update(){
        super.update();

        //updates for playable
        
    }



    /** 
     * method should try to interact with whatever is in front. if any tools is equiped, that tool is used*/ 
    public void interact(){
    
        for (BaseEntity ent:panel.world.getEntities()){
            if (ent instanceof GameObject){
              
                GameObject e = (GameObject) ent;
                if (e.getHitBox().intersects(getInteractionHitBox())){
                    e.interact(this);
                }
            }
        }

    }

    public void nullPath(){
        path.clear();
    }

    public void addItem(Item item){
        inventory.addItem(item);
    }
    public void addStack(Stack stack){
        inventory.addStack(stack);
    }

    public Item getItem(){
        return equipped.getItem(0);
    }



    public static String getBaseentity() {
        return BaseEntity;
    }



    public Inventory getInventory() {
        return inventory;
    }



    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }



    public Stack getEquipped() {
        return inventory.getStack(inventory.getIndex());
    }



    public void setEquipped(Stack equipped) {
        this.equipped = equipped;
    }


    public Stack getTempInHand() {
        return tempInHand;
    }


    public void setTempInHand(Stack tempInHand) {
        this.tempInHand = tempInHand;
    }

    

    

    
}
