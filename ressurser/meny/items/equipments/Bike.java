package ressurser.meny.items.equipments;

import ressurser.meny.Meny;

public class Bike extends Equipment {

    
    public Bike(Meny menu, String navn) {
        super(menu, navn);

        
        

    }
    public void unEquip(){
        menu.panel.spiller.unEquipBike();
        exit();
    }

    @Override
    protected String getInformationText() {
        return null;
    }

    @Override
    protected void equip() {
        menu.panel.spiller.equipBike();
        exit();
      }
   
    
    
}
