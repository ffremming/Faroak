package ressurser.meny.items.equipments;

import ressurser.meny.Meny;

public class Boat extends Equipment{

    public Boat(Meny menu, String navn) {
        super(menu, navn);
        
        
    }

    protected void equip() {
        menu.panel.spiller.equipBoat();
        exit();
    }

    @Override
    public void unEquip() {
        menu.panel.spiller.unEquipBoat();
        exit();
    }

    

    @Override
    protected String getInformationText() {
        // TODO Auto-generated method stub
        return null;
    }
}