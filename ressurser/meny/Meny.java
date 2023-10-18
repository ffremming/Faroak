package ressurser.meny;

import ressurser.main.GamePanel;

public class Meny {

    public GamePanel panel;
    public SlotCategories [] meny;
    public int slotPick;
    public int antSlot;
    public Resources resources;
    public Items items;
    public Materials materials;

    public boolean menuBar = false;
    public boolean itemBar = false;
    public boolean materialBar = false;
    public boolean itemOptionBar = false;
    public boolean craftingBar = false;
    public boolean resourcesBar = false;

    public Meny(GamePanel panel){
        
        antSlot = 3;
        meny = new SlotCategories[antSlot];
        slotPick = 0;
        this.panel = panel;
        resources = new Resources(this);
        items = new Items(this);
        materials = new Materials(this);
        
        
        meny[0] = resources;
        meny[1] = items;
        meny[2] = materials;
        

        

    }

    public void open(){
        panel.gameState = panel.MENUSTATE;
    }

    public void up(){
        //endrer slotpick variablen for å gå oppover array
        if (slotPick >0){
            slotPick--;
        }
    }

    public void down(){
        //endrer slotpick variablen for å gå nedover array
        
        if (slotPick < antSlot-1){
            slotPick++;
        }
        
    }
    
    public void interact(){
        meny[slotPick].open();
    }

    public void exit(){
        panel.gameState = panel.PLAYSTATE ;
    }
}
