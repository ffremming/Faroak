package ressurser.meny.items.equipments;

import ressurser.meny.Meny;

public class Hoe  extends Equipment{
    
    public Hoe(Meny menu, String navn) {
        super(menu, navn);
    }

    @Override
    protected void equip() {
        exit();
        menu.panel.spiller.equipHoe();
    }

    @Override
    protected void unEquip() {
        exit();
        menu.panel.spiller.unEquipHoe();
    }

    
}
