package ressurser.meny.items.equipments;

import ressurser.meny.Meny;

public class Hammer extends Equipment {

    public Hammer(Meny menu, String navn) {
        super(menu, navn);
        
       
    }

    @Override
    protected String getInformationText() {
        return null;
    }

    @Override
    protected void equip() {
        exit();
        menu.panel.spiller.equipHammer();
    }

    @Override
    protected void unEquip() {
        exit();
        menu.panel.spiller.unEquipHammer();
    }  
}
