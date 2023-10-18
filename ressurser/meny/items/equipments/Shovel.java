package ressurser.meny.items.equipments;

import ressurser.meny.Meny;

public class Shovel extends Equipment{

    public Shovel(Meny menu, String navn) {
        super(menu, navn);
        }

    @Override
    protected String getInformationText() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected void equip() {
       menu.panel.spiller.equipShovel();
       exit();
    }

    @Override
    protected void unEquip() {
        exit();
        menu.panel.spiller.unEquipShovel();
    }

    
}
