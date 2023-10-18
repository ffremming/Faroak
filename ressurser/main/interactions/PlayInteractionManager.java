package ressurser.main.interactions;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import ressurser.main.GamePanel;
import ressurser.meny.Items;
import ressurser.meny.Materials;
import ressurser.meny.Resources;
import ressurser.meny.items.Item;
import ressurser.meny.items.equipments.Equipment;
import ressurser.meny.items.materials.Material;
import ressurser.objects.SuperObject;

public class PlayInteractionManager implements Interaction{
    GamePanel panel;
    public PlayInteractionManager(GamePanel panel){
    this.panel = panel;
    }
    
    public void enterPressed(){
        //if not out of bounds:
        if (!panel.collisionC.checkOutOfBounds(panel.spiller.worldX,panel.spiller.worldY,panel.spiller.retning)){

            //Gamestate = PLAYSTATE :
            if (panel.gameState == panel.PLAYSTATE ){

                //if object is in front, and object interactable - interact()
                if ( panel.objM.isObject(panel.spiller.retning)) {
                    ressurser.objects.SuperObject o =panel.objM.getObject(panel.spiller.retning);
                    o.enterInteract();
                
                // if npc is in front - interact()
                } else if (panel.collisionC.checkCollisionEntities(panel.spiller.worldX,panel.spiller.worldY,panel.spiller.retning)){
                    panel.collisionC.getNPCCollided().interact();
                }
            
            
            //Gamestate = dialogeState:
            } else if (panel.gameState == panel.DIALOGSTATE){
                //if you are in a yes/no option
                if (panel.gameOption){
                    //if NPC option
                    
                    if (panel.collisionC.checkCollisionEntities(panel.spiller.worldX,panel.spiller.worldY,panel.spiller.retning)){
                        if (panel.arrowYes){
                            //yes choses
                            panel.collisionC.getNPCCollided().answer(true);
                        } else if (!panel.arrowYes){
                            panel.collisionC.getNPCCollided().answer(false);
                            //if no chosen
                        }
                        
                }
                } else if (!panel.gameOption){

                //if npc collision, continueInteraction()
                if (panel.collisionC.checkCollisionEntities(panel.spiller.worldX,panel.spiller.worldY,panel.spiller.retning)){
                        panel.collisionC.getNPCCollided().continueInteraction();
                        

                //if object infront, continueInteraction()
                } else if ( panel.objM.getObjInteractable(panel.spiller.retning)) {
                        ressurser.objects.SuperObject o =panel.objM.getObject(panel.spiller.retning);
                        o.enterContinueInteraction();
                        
                    }
                }
           
                
                
                
            //Gamestate = menustate:
            } else if (panel.gameState == panel.MENUSTATE){
                if (panel.menu.menuBar){
                    //åpner en kategori
                    panel.menu.meny[panel.menu.slotPick].open();
                } else if (panel.menu.itemBar){
                    if (!panel.menu.itemOptionBar){
                        Items i = panel.menu.items;
                        //velger en item
                        i.interact();
                        }   else {
                            // gjør et valg. valgt mellom bruke eller ikke.
                            Equipment i = panel.menu.items.content.get(panel.menu.items.indeks);
                            i.getOption();
                        }

                } else if (panel.menu.resourcesBar){
                    if (!panel.menu.itemOptionBar){
                        Resources r = panel.menu.resources;
                        //velger en item
                       
                        r.interact();
                        }   else {
                            // gjør et valg. valgt mellom bruke eller ikke.
                            Item r = panel.menu.resources.content.get(panel.menu.resources.indeks);
                            r.getOption();
                            
                            
                        }
                }  else if (panel.menu.materialBar){
                    if (!panel.menu.itemOptionBar){
                        Materials m = panel.menu.materials;
                        //velger en item
                        m.interact();
                        }   else {
                            // gjør et valg. valgt mellom bruke eller ikke.
                            Material mat = panel.menu.materials.content.get(panel.menu.materials.indeks);
                            mat.getOption();
                           
                        }
                }
            }
        }
    }

    public void upPressed(){

            if (panel.gameState == panel.PLAYSTATE ){
            }


            if (panel.gameState == panel.DIALOGSTATE){
                if (panel.gameOption){
                    panel.arrowYes = true;
                }
            } else if (panel.gameState == panel.MENUSTATE){
                if (panel.menu.menuBar){
                    panel.menu.up();
                    //opp i meny
                } else if (panel.menu.itemBar){
                    if  (panel.menu.itemOptionBar){
                        //opp i itemoptionBar
                        panel.menu.items.content.get(panel.menu.items.indeks).up();

                    } else if (!panel.menu.itemOptionBar) {
                        //opp i item-menybar
                        panel.menu.items.up();
                    }
                    
                //if resources
                }else if (panel.menu.resourcesBar){
                    if (panel.menu.itemOptionBar){
                        //opp i itemoptionBar
                        panel.menu.resources.content.get(panel.menu.resources.indeks).up();
                    } else if (!panel.menu.itemOptionBar){
                        //opp i resources
                        panel.menu.resources.up();
                    }
                    
                // if materials
                } else if (panel.menu.materialBar){
                    if (panel.menu.itemOptionBar){
                        panel.menu.materials.content.get(panel.menu.materials.indeks).up();
                    } else if (!panel.menu.itemOptionBar){
                        panel.menu.materials.up();
                    }

                   
                } 
        }
    }

    public void downPressed(){
        if (panel.gameState == panel.PLAYSTATE){
           
        }

        
        if (panel.gameState == panel.DIALOGSTATE){
            if (panel.gameOption){
                panel.arrowYes = false;
            }
        } else if (panel.gameState == panel.MENUSTATE){
            if (panel.menu.menuBar){
                //ned menu
                panel.menu.down();

            } else if (panel.menu.itemBar){
                if  (panel.menu.itemOptionBar){
                    //ned i itemoptionBar
                    panel.menu.items.content.get(panel.menu.items.indeks).down();

                } else if (!panel.menu.itemOptionBar) {
                    //ned i item-menybar
                    panel.menu.items.down();
                }
                
            //if resources
            }else if (panel.menu.resourcesBar){
                if (panel.menu.itemOptionBar){
                    //ned i itemoptionBar
                    panel.menu.resources.content.get(panel.menu.resources.indeks).down();
                } else if (!panel.menu.itemOptionBar){
                    //ned i resources
                   
                    panel.menu.resources.down();
                }
                
            // if materials
            } else if (panel.menu.materialBar){
                if (panel.menu.itemOptionBar){
                    panel.menu.materials.content.get(panel.menu.materials.indeks).down();
                } else if (!panel.menu.itemOptionBar){
                    panel.menu.materials.down();
                }

               
            } 
        }
    }

    public void rightPressed(){
        if (panel.gameState == panel.PLAYSTATE){
           
        }
    }


    public void leftPressed(){
        if (panel.gameState == panel.PLAYSTATE){
        
        }
    }


    public void mPressed(){
        if (panel.gameState == panel.PLAYSTATE ){
            panel.gameState = panel.MENUSTATE;
            panel.menu.menuBar = true;
        } else {
            panel.gameState = panel.PLAYSTATE ;
            panel.menu.menuBar = false;
            panel.menu.itemOptionBar = false;
            panel.menu.itemBar = false;
            panel.menu.resourcesBar = false;
        }
        

    }




    public void shiftPressed() {
        
        if (removeObjectWithTool()){}
        
        else if ( placeTileWithTool()){}

        else if (placeObjectWithTool()){}
        
         
    }

    public void onePressed(){

        changeIndex(0);
    }
    public void twoPressed(){
        changeIndex(1);
    }
    public void threePressed(){
        changeIndex(2);
    }
    public void fourPressed(){
        changeIndex(3);
    }
    public void fivePressed(){
        changeIndex(4);
    }
    public void sixPressed(){
        changeIndex(5);
    }
    public void sevenPressed(){
        changeIndex(6);
    }
    public void eightPressed(){
        changeIndex(7);
    }
    public void ninePressed(){
        changeIndex(8);
    }


    public boolean removeObjectWithTool(){
        //string for the activeTool

        


        String tool = panel.spiller.activeTool;
        if (tool== null){
            tool = "null";
        }

        //if object in front
        if (panel.objM.isObject(panel.spiller.retning)){
            ressurser.objects.SuperObject o = panel.objM.getObject(panel.spiller.retning);

            //if object can be broken by chosen tool
            if (o.hammerBreakable && tool.equals("hammer")){
                return breakObjectAndYield(o);

            } else if (o.shovelBreakable && tool.equals("shovel")){
                return breakObjectAndYield(o);

            } else if (o.axeBreakable && tool.equals("axe")){
                return breakObjectAndYield(o);

            } else if (o.hoeBreakable && tool.equals("hoe")){
                return breakObjectAndYield(o);

            } else if (o.pickaxeBreakable && tool.equals("pickaxe")){
                return breakObjectAndYield(o);
            }
        }
        return false;
    }

    public boolean breakObjectAndYield(ressurser.objects.SuperObject o){

        //call smash on object- this should destroy, but might not also.
        o.smash();

        
        return true;
    }

    //generell bygge metode:

    public boolean placeObjectWithTool(){
    
        //if players material is not nothing
        if (panel.spiller.activeItem != null){
            Item i = panel.spiller.activeItem;
            //if not a object in front of player
            if (i.placeable && i.object){

                //checks if the player has the right tool, or it is not a requirment.
                    if (i.toolRequirement.equals("none")||i.toolRequirement.equals(panel.spiller.activeTool)){
                    if (!panel.objM.isObject(panel.spiller.retning)){
                        if (!panel.tileM.getTile(panel.spiller.retning).collision && (panel.tileM.getTile(panel.spiller.retning).type.equals(i.tileRequirment)||i.tileRequirment.equals(("none")))){

                            //if amount of material is above 0
                            if (i.getQuantity()>0){
                                panel.objM.placeOrinaryObject(i.getType(),i.objectName);
                                //remove after placing one.
                                i.removeQuantity(1);
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    public boolean placeTileWithTool(){
        if (panel.spiller.activeItem != null){
            Item r = panel.spiller.activeItem;
            //if not a object in front of player
            if (r.placeable && r.tile){
                if ( r.placeable){
                    
                        if (r.toolRequirement.equals("none")||r.toolRequirement.equals(panel.spiller.activeTool)){
                            if (r.getQuantity()>0){
                            panel.tileM.placeTile(r.type);
                            r.removeQuantity(1);
                            return true;
                            }
                        }
                    
                }
            }
            
        }
        return false;
    }

   

    private void changeIndex(int nyIndex){
        if (panel.gameState == panel.PLAYSTATE ){
            panel.itemB.changeIndex(nyIndex);
        } else if (panel.gameState == panel.MENUSTATE){

            if (panel.menu.resourcesBar){

                if (panel.itemB.waitingForInput){
                    panel.menu.resources.content.get(panel.menu.resources.indeks).equipIndex(nyIndex);
                }  

            } else if (panel.menu.materialBar){
                panel.menu.materials.content.get(panel.menu.materials.indeks).equipIndex(nyIndex);
                if (panel.itemB.waitingForInput){

                }
            }
        }
    }
    public void changeIndexScroll(int value){
        panel.itemB.changeIndex( panel.itemB.indexValue+value);
    }

    public boolean placeObjectWithToolMouse(int x,int y){
        
        //if players material is not nothing
        if (panel.spiller.activeItem != null){

            Item i = panel.spiller.activeItem;
            
            //if not a object in front of player
            if (i.placeable && i.object){System.out.println("placeobject "+3);

                //checks if the player has the right tool, or it is not a requirment.
                    if (i.toolRequirement.equals("none")||i.toolRequirement.equals(panel.spiller.activeTool)){
                    if (!panel.objM.isObject(x,y)){System.out.println("placeobject "+43);
                        if (!panel.tileM.getTile(x,y).collision && (panel.tileM.getTile(x,y).type.equals(i.tileRequirment)||i.tileRequirment.equals(("none")))){

                            //if amount of material is above 0
                            if (i.getQuantity()>0){System.out.println("placeobject "+6);
                                if (panel.objM.placeOrinaryObjectMouse(i.getType(),i.objectName,x,y)){i.removeQuantity(1);}
                                //remove after placing one.w
                                System.out.println("placeobject "+7);
                                //i.removeQuantity(1);
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }


    public boolean placeTempObjectWithToolMouse(int x,int y){
        
        //if players material is not nothing
        if (panel.spiller.activeItem != null){

            Item i = panel.spiller.activeItem;
            System.out.println("placeobject "+2);
            //if not a object in front of player
            if (i.placeable && i.object){

                //checks if the player has the right tool, or it is not a requirment.
                    if (i.toolRequirement.equals("none")||i.toolRequirement.equals(panel.spiller.activeTool)){System.out.println("placeobject "+4);
                    if (!panel.objM.isObject(x,y)){System.out.println("placeobject "+43);
                        if (!panel.tileM.getTile(x,y).collision && (panel.tileM.getTile(x,y).type.equals(i.tileRequirment)||i.tileRequirment.equals(("none")))){

                            //if amount of material is above 0
                            if (i.getQuantity()>0){System.out.println("placeobject "+6);

                                //changed --!
                                //if (panel.objM.placeTemoraryObjectMouse(i.getType(),i.objectName,x,y)){i.removeQuantity(1);}
                                //remove after placing one.w
                                System.out.println("placeobject "+7);
                                //i.removeQuantity(1);
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    public SuperObject getObjectPlaceable(int x,int y){
        
        //if players material is not nothing
        if (panel.spiller.activeItem != null){

            Item i = panel.spiller.activeItem;
            
            //if not a object in front of player
            if (i.placeable && i.object){

                //checks if the player has the right tool, or it is not a requirment.
                    if (i.toolRequirement.equals("none")||i.toolRequirement.equals(panel.spiller.activeTool)){
                        if (!panel.tileM.getTile(x,y).collision && (panel.tileM.getTile(x,y).type.equals(i.tileRequirment)||i.tileRequirment.equals(("none")))){

                            //if amount of material is above 0
                            if (i.getQuantity()>0){

                            int tileX =  panel.objM.getNearest32(x)/32;
                            int tileY =  panel.objM.getNearest32(y)/32;
                           
                            SuperObject o = panel.objM.factory.createGameObject(tileX*32,tileY*32,i.objectName,i.getType(),0,0,1,1); 
                            return o;
                              
                            }
                        }
                    }
                }
            }
            return null;
        }
        
    

    public void getHighlightedTile(int x,int y){
        int tileX =  panel.objM.getNearest32(x);
        int tileY =  panel.objM.getNearest32(y);
        panel.objM.tempTile = panel.tileM.getTile(tileX,tileY);
        
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'mouseClicked'");
    }

    @Override
    public void mousePressed(MouseEvent e) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'mousePressed'");
    }


    @Override
    public void mouseReleased(MouseEvent e) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'mouseReleased'");
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'mouseEntered'");
    }

    @Override
    public void mouseExited(MouseEvent e) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'mouseExited'");
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'mouseDragged'");
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'mouseMoved'");
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'mouseWheelMoved'");
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();

        if (code == KeyEvent.VK_W){panel.spiller.resetYMovement();panel.input.upPressed = false;} 

        if (code == KeyEvent.VK_A){panel.spiller.resetXMovement();panel.input.leftPressed = false;}

        if (code == KeyEvent.VK_S){panel.spiller.resetYMovement();panel.input.downPressed = false;}
            
        if (code == KeyEvent.VK_D){panel.spiller.resetXMovement();panel.input.rightPressed = false;}
        
    }

    @Override
    public void keyPressed(KeyEvent e) {

        int code = e.getKeyCode();


        if (code == KeyEvent.VK_W){panel.input.upPressed = true;}

        if (code == KeyEvent.VK_A){panel.input.leftPressed = true;}

        if (code == KeyEvent.VK_S){panel.input.downPressed = true;}
            
        if (code == KeyEvent.VK_D){panel.input.rightPressed = true;}
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'keyTyped'");
    }


   
}

        