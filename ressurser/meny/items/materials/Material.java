package ressurser.meny.items.materials;

import ressurser.meny.Meny;
import ressurser.meny.items.Item;

public abstract class Material extends Item {

    

    public Material(Meny menu, String navn) {
        super(menu, navn);
        toolRequirement = "none";
        placeable = true;
        object = true;
        //TODO Auto-generated constructor stub
    }

    @Override
    public void interact() {
        menu.itemOptionBar = true;
    }

    public void getOption(){
       
        if (indeks == 0){
            equip();
           
        } else if (indeks == 1){
            cancel();
           
        } else if (indeks == 2){
            unEquip();
        }
    }

    
    
}
