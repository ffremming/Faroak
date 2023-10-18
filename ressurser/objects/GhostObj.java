package ressurser.objects;

import ressurser.main.GamePanel;

public class GhostObj extends InteractableObj{
    SuperObject alpha;
    public GhostObj(GamePanel gp, ObjectManager objM, int worldX, int worldY, String name, String type,SuperObject alpha) {
        super(gp, objM, worldX, worldY, name, type);
        this.alpha = alpha;
        interactable = alpha.interactable;
        axeBreakable= alpha.axeBreakable;
        shovelBreakable = alpha.shovelBreakable;
        hoeBreakable = alpha.hoeBreakable;
        pickaxeBreakable = alpha.pickaxeBreakable;
        hammerBreakable = alpha.hammerBreakable;

        alwaysInteract = alpha.alwaysInteract;
        name = alpha.name;

    

        yieldable = alpha.yieldable;
        materialYield = alpha.materialYield;
        resourceYield = alpha.resourceYield;

        yieldType = alpha.yieldType;
        smashCounter = alpha.smashCounter;
        type = alpha.type;
        durability = alpha.durability;

        type = alpha.type;

    }
    @Override
    public void  enterInteract(){
        alpha.enterInteract();
    }

    @Override
    public void  enterContinueInteraction(){
        alpha.enterContinueInteraction();
    }

    @Override
    public void interact() {
        alpha.interact();
    }
    
    @Override
    public void smash(){
        System.out.println("alpha");
        alpha.durability--;
        if (alpha.durability<= 0){
            for (SuperObject o:alpha.connectedCells){
                if (o!= null){
                    ((GhostObj)o).remove();
                }
               
                
            }
    
            alpha.smash();
        }
        
        
    }
    public void remove(){
        objM.map [panel.mapH.activeMapType][panel.mapH.activeMapNumber][worldY/32][worldX/32] = null;
    }
    
}
