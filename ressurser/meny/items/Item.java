package ressurser.meny.items;


import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

import ressurser.meny.Meny;
import ressurser.meny.SlotSubCategories;

public abstract class Item extends SlotSubCategories {
    protected String navn;
    protected int quantity = 1;
    public int maxIndeks = 2;

    protected int maxQuantity = 999;
    public BufferedImage sprite;

    //values that has to do with placing
    public String type = "null";
    public String toolRequirement = "null";
    public boolean placeable = false;
    public String tileRequirment = "none";

    public String objectName;

    public boolean tile = false;
    public boolean object = false;

    public Item(Meny menu,String navn){
        super(menu);
        this.navn = navn;
        objectName = navn;
        
        getSprite(navn);
        
    }

    //returnerer navn;
    public String getName(){
        return navn;
    }

    public abstract void interact();
        
    public void getOption(){
        //nothing yet. Equipement alreade overrided.
    }

    
    public int getQuantity(){
        return quantity;
    }

    protected void getSprite(String navn){
        try {
            
            sprite = ImageIO.read(getClass().getResourceAsStream("../itemSprites/"+navn+".png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    protected void cancel(){
        menu.itemOptionBar = false;
    }
       
    

    public void exit() {
        menu.itemOptionBar = false;
        menu.menuBar = false;
        menu.itemBar = false;
        menu.resourcesBar = false;
        menu.materialBar = false;
        menu.panel.gameState = menu.panel.PLAYSTATE ;
    }

    public void removeQuantity(int ant){
        if (quantity>0){
            quantity -= ant;
        }
        if (quantity < 0){
            quantity = 0;
        }
    }

    public void addQuantity(int ant){
        quantity+= ant;
        if (quantity > maxQuantity){
            quantity = maxQuantity;
        }
    }

    
    public void up() {
        if (indeks >0){
            indeks--;
        }
    }
    
   
    public void down() {
       
        if (indeks <maxIndeks){
            indeks++;
        }
    }
    @Override
    public boolean equals(Object o) {
        return (((Item) o).getName() == getName());
    }

    protected String getInformationText(){
        //does nothing yet
        return null;
    }

    
    protected void equip() {

        if (menu.panel.itemB.itemBarFull()){
            //du må velge indeks
            menu.panel.itemB.waitingForInput = true;

        } else {
            int index = menu.panel.itemB.getNextEmptySlot();
            equipIndex(index);
        }
    }
    protected void unEquip(){
        menu.panel.itemB.removeItem(this);
        exit();
    }
    
    
    public void equipIndex(int index){
        menu.panel.itemB.itemBarContent[index] = this;
        menu.panel.itemB.updatePlayerContent();
        menu.panel.spiller.activeMaterial = "f";
        exit();
    }

    public String getType(){
        return type;
    }



}
