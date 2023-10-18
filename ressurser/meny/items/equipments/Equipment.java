package ressurser.meny.items.equipments;

import ressurser.meny.Meny;
import ressurser.meny.items.Item;

public abstract class Equipment extends Item {

    public Equipment(Meny menu, String navn) {
        super(menu, navn);
        maxQuantity = 1;
    }
   

    protected abstract void equip();
    protected abstract void unEquip();

    public void interact(){
        menu.itemOptionBar = true;
    }

    public void getOption(){
        System.out.println("indeks"+indeks);
        if (indeks == 0){
            equip();
        } else if (indeks == 1){
            cancel();
        } else if (indeks == 2){
            unEquip();
        }
    } 
}
