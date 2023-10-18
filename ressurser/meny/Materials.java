package ressurser.meny;

import java.util.ArrayList;
import java.util.HashMap;

import ressurser.meny.items.materials.BFence;
import ressurser.meny.items.materials.Material;

public class Materials extends SlotCategories{

    private final String name = "materials";
    public int indeks;
    public ArrayList<Material> content;
    public HashMap<String,Material> contentLibrary;
    private Material bFence;

    public Materials(Meny menu) {
        super(menu);
        content = new ArrayList<Material>();
        contentLibrary = new HashMap<String,Material>();
        
        getMaterials();
        addItem("brownFence");
    }

    private void getMaterials(){
        bFence = new BFence(menu,"brownFence");
       
        contentLibrary.put("brownFence",bFence);
        
    }

    @Override
    public void interact() {
        content.get(indeks).interact();
        
    }

    @Override
    public void open() {
        menu.menuBar = false;menu.materialBar = true;
    }

    @Override
    public void exit() {
        menu.materialBar = false;
        menu.menuBar = true;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void up() {
        if (indeks > 0){
            indeks --;
        }
    }

    @Override
    public void down() {
        if (indeks < content.size()-1){
            indeks ++;
        }
    }

    @Override
    public void use() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'use'");
    }

    @Override
    public int getSize() {
        return content.size();
    }
    
    public void addItem(String s){
        Material item = contentLibrary.get(s);
                
        if (!content.contains(item)){
            content.add(content.size(),item);
        System.out.println(content.size());
        } else {
            item.addQuantity(1);      //does not let item add more than maximum.
        }
    }
}
