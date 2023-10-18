package ressurser.meny.items.equipments;

import ressurser.meny.Meny;

public class Axe extends Equipment {

    public Axe(Meny menu, String navn) {
        super(menu, navn);
        
    }


    @Override
    protected String getInformationText() {
        
        return null;
    }

    @Override
    protected void equip() {
        menu.panel.spiller.equipAxe();
        exit();
    }

    @Override
    public void unEquip(){
        menu.panel.spiller.unEquipAxe();
        exit();
    }
}
