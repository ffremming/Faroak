package ressurser.meny;

import java.util.ArrayList;
import java.util.HashMap;

import ressurser.meny.items.*;
import ressurser.meny.items.equipments.Axe;
import ressurser.meny.items.equipments.Bike;
import ressurser.meny.items.equipments.Boat;
import ressurser.meny.items.equipments.Equipment;
import ressurser.meny.items.equipments.Hammer;
import ressurser.meny.items.equipments.Hoe;
import ressurser.meny.items.equipments.Shovel;

public class Items extends SlotCategories{
    private final String name = "Items";
    public int indeks;
    public ArrayList<Equipment> content;
    public HashMap<String,Equipment> contentLibrary;
    private Equipment bike,boat,axe,shovel,bucket,hoe,hammer;

    public Items(Meny menu){
        super(menu);
        indeks = 0;
        content = new ArrayList<Equipment>();
        contentLibrary = new HashMap<String,Equipment>();
        getItems();
        addItem("boat");
        addItem("shovel");
        addItem("axe");
        addItem("bike");
        addItem("hoe");
        addItem("hammer");

       
        
    }
    private void getItems(){
        bike = new Bike(menu,"bike");
        boat = new Boat(menu,"boat");
        axe = new Axe(menu,"axe");
        shovel = new Shovel(menu,"shovel");
        hoe = new Hoe(menu,"hoe");
        hammer = new Hammer(menu,"hammer");


        contentLibrary.put("bike",bike);
        contentLibrary.put("boat",boat);
        contentLibrary.put("axe",axe);
        contentLibrary.put("shovel",shovel);
        contentLibrary.put("hoe",hoe);
        contentLibrary.put("hammer",hammer);
        
    }

    
    
    public void interact() {
        content.get(indeks).interact();
    }

    @Override
    public
    void exit() {
        menu.itemBar = false;
        menu.menuBar = true;
    }

    @Override
    public String getName() {
        return name;
    }

    public void up(){
        if (indeks > 0){
            indeks --;
        }
    }

    public void down(){
        if (indeks < content.size()-1){
            indeks ++;
        }
    }




    public void addItem(String s){
        
        Equipment item = contentLibrary.get(s);
                
                if (!content.contains(item)){
                    content.add(content.size(),item);
                   System.out.println(content.size());
                } else {
                    item.addQuantity(1);      //does not let item add more than maximum.
                    
               }
        }  


    @Override
    public int getSize(){
        
        return content.size();
        
        
    }
    @Override
    public void use() {
        // TODO Auto-generated method stub
        
    }
    @Override
    public void open() {
        menu.menuBar = false;menu.itemBar = true;
        
    }
}
