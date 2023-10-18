package ressurser.meny;

import java.util.ArrayList;
import java.util.HashMap;

import ressurser.meny.items.Item;
import ressurser.meny.items.resources.*;


public class Resources extends SlotCategories {
    
    public HashMap <String,Item> inventory;
    private String name;

    
    
    public ArrayList<Item> content;

    public Resources(Meny menu){
        super(menu);
        indeks = 0;
        inventory = new HashMap<String,Item>();
        this.content = new ArrayList<Item>();
        name = "resources";
        setup();
        addItem("wood");
        addItem("dirt");
        addItem("fireFruit");
        addItem("fireSeed");
    }

    public void addItem(String resource){
        Item r = inventory.get(resource);
                
        if (!content.contains(r)){
            content.add(content.size(),r);
        } else {
            System.out.println("add quantity");
            r.addQuantity(1);      //does not let item add more than maximum.
       }
} 
       

    private void setup(){
        Item wood = new Wood(menu,"wood");
        Item dirt = new Dirt(menu,"dirt");
        Item fireFruit = new FireFruit(menu,"fireFruit");
        Item fireSeed = new FireSeed(menu,"fireSeed");

        inventory.put("wood",wood);
        inventory.put("dirt",dirt);
        inventory.put("fireFruit",fireFruit);
        inventory.put("fireSeed",fireSeed);

    }

    public void removeContent(String ressurs){
        inventory.get(ressurs).removeQuantity(1);
    }

    @Override
    public
    void interact() {
        menu.itemOptionBar = true;
    }

    @Override
    public
    void exit() {
       menu.menuBar = true;
        
        menu.itemBar = false;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void up() {
        indeks --;
        if (indeks <0){
            indeks = 0;
        }
        
    }

    @Override
    public void down() {
        System.out.println("resoruce"+indeks);
        indeks ++;
        if (indeks > content.size()-1){
            indeks = content.size()-1;
        }
        
    }

    @Override
    public void use() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public int getSize() {
        // TODO Auto-generated method stub
        return content.size();
    }

    @Override
    public void open() {
        System.out.println("open");
        menu.menuBar = false;
        menu.resourcesBar = true;
        
    }
}
