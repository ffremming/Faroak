package ressurser.meny.items.resources;

import ressurser.meny.Meny;
import ressurser.meny.items.Item;

public class Resource extends Item{
    
    public Resource(Meny menu, String navn) {
        super(menu, navn);
        
        //TODO Auto-generated constructor stub
    }



    @Override
    protected String getInformationText() {
        return null;
    }

    @Override
    public void interact() {
        menu.itemOptionBar = true;
    }

    public void getOption(){
        
        if (indeks == 0){
             if (!menu.panel.itemB.isItemInBar(this)){
                equip();
             }
            
        } else if (indeks == 1){
            cancel();
           
        } else if (indeks == 2){
            unEquip();
        }
    }
}
