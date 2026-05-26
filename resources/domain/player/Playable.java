package resources.domain.player;

import resources.app.GamePanel;
import resources.domain.entity.BaseEntity;
import resources.domain.entity.Entity;
import resources.geometry.HitBox;
import resources.geometry.Vector;
import resources.domain.inventory.Inventory;
import resources.domain.inventory.ItemManager;

import resources.domain.entity.BaseEntity;
import resources.domain.object.GameObject;
import resources.domain.inventory.Inventory;
import resources.domain.inventory.Item;
import resources.domain.inventory.Stack;
import resources.app.GamePanel;


public class Playable extends Moveable{

    
    private static final String BaseEntity = null;
    Inventory inventory;
    Stack equipped;
    private Stack tempInHand = null;

    public Playable(GamePanel panel, String name, int worldX, int worldY, short width, short height, short hitBoxWidth,
            short hitBoxHeight, short relativeXPLus, short relativeYPlus) {
        super(panel, name, worldX, worldY, width, height, hitBoxWidth, hitBoxHeight, relativeXPLus, relativeYPlus);

        inventory = new Inventory(this);
        panel.userInterface.clear();
        panel.userInterface.addInventory(inventory);


        Item hammer = new Item(panel,"hammer");
        Item block = new Item(panel,"block");
        Item axe = new Item(panel,"axe");
        addItem(hammer);
        Item house = new Item(panel,"demoHouse");
        addItem(house);
        addItem(block,300);
        addItem(axe);
        equipped = inventory.getStack(inventory.getIndex());
       
    }

    public void update(){
        super.update();
        panel.world.addObjectPreview(equipped);
        //updates for playable
        //eSystem.out.println(inventory);
        
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
    public void addItem(Item item,int amount){
        inventory.addStack(new Stack(panel,item,amount));
    }

    public void addStack(Stack stack){
        inventory.addStack(stack);
    }

    public Item getItem(){
        return equipped.getItem();
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
        return inventory.getStack(27+inventory.getIndex());
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
