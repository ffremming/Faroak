package ressurser.main;
import java.awt.Font;
import java.awt.Color;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

import ressurser.meny.Items;
import ressurser.meny.Materials;
import ressurser.meny.Meny;
import ressurser.meny.Resources;
import ressurser.meny.SlotCategories;
import ressurser.meny.items.materials.Material;

import java.awt.BasicStroke;
public class UI {

    BufferedImage arrowSprite,boardSprite,smallBoardSprite,axeSprite,shovelSprite,iconFrame,itemBar,itemBarFrame;
    public String dialogueString = "hei på deg";
    
    GamePanel panel;
    Graphics2D g2;

    int avstand = 35;
    int menuX,menuY;
    int menuTextOffsetX = 40;
    int menuTextOffsetY = 40;
    int menuArrowOffsetX = 20;
    int menuArrowOffsetY = 40;

    
    
    
    //Graphics2D g2;
    public UI(GamePanel gp){
        this.panel = gp;
        panel.textString = "yoyo";
        getArrowSprite();
        menuX = panel.skjermBredde-6*panel.tileSize;
        menuY= panel.skjermHoyde+panel.tileSize*2;
        
    }

    private void getArrowSprite(){
        try {
            String folderName = "UISprites";
            arrowSprite = ImageIO.read(getClass().getResourceAsStream(folderName+"/arrow4.png"));
            boardSprite = ImageIO.read(getClass().getResourceAsStream(folderName+"/menyBoard1.png"));
            smallBoardSprite = ImageIO.read(getClass().getResourceAsStream(folderName+"/smallBoard4.png"));
            iconFrame = ImageIO.read(getClass().getResourceAsStream(folderName+"/iconBoard1.png"));
            itemBar = ImageIO.read(getClass().getResourceAsStream(folderName+"/itemBar.png"));
            itemBarFrame = ImageIO.read(getClass().getResourceAsStream(folderName+"/itemBarFrame2.png"));

            axeSprite = ImageIO.read(getClass().getResourceAsStream("../meny/items/itemSprites/axe.png"));
            shovelSprite = ImageIO.read(getClass().getResourceAsStream("../meny/items/itemSprites/shovel.png"));
            
           
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }

    public void draw(Graphics2D g2){
        g2.setColor(Color.white);
        g2.drawString("x: "+panel.spiller.worldX+", y: "+panel.spiller.worldY,20,20);

        this.g2 = g2;
        drawItemBar();
        drawActiveTool();
        if (panel.gameState == panel.DIALOGSTATE){
            if (panel.dialoge ){
                drawDialoge();
            } else if (panel.textBox){
                drawTextBox();
            
            } if (panel.gameOption){
                drawOptionBox();
                drawArrow();
            }

        }
        if (panel.gameState == panel.MENUSTATE){
            
            if (panel.menu.menuBar){
                
                drawMenu();
            } else if (panel.menu.itemBar){
                if (!panel.menu.itemOptionBar){
                    drawSubMenuItems(true);
                } else if (panel.menu.itemOptionBar){
                    drawSubMenuItems(false);
                    drawItemOptionBar();
                }
            } else if (panel.menu.resourcesBar){
                if (!panel.menu.itemOptionBar){
                    drawSubMenuResources(true);
                   
                } else if (panel.menu.itemOptionBar){
                    drawSubMenuResources(false);
                    drawItemOptionBarResources();
                }
            } else if (panel.menu.materialBar){
                if (panel.menu.itemOptionBar){
                    drawSubMenuMaterials(false);
                    drawItemOptionBarMaterials();
                } else {
                    drawSubMenuMaterials(true);
                }
            }   
        }
    }



    public void drawDialoge(){
        Color c = new Color(122,176,255,200);
        g2.setColor(c);
        g2.fillRoundRect(45,panel.skjermHoyde-105,  panel.skjermBredde - 45*2, 60,  10, 10);
        g2.setColor(Color.white); 
        g2.setStroke(new BasicStroke(5));
        g2.drawRoundRect(50,panel.skjermHoyde-100,  panel.skjermBredde - 50*2, 50,  10, 10);
        g2.setFont(new Font("Arial",Font.PLAIN,20));
        g2.drawString(dialogueString,50+30,panel.skjermHoyde-100+30+2);


    }
    public void drawOptionBox(){
        Color c = new Color(122,176,255);
        g2.setColor(c);
        int x = panel.skjermBredde - 150;
        int y = panel.skjermHoyde-190;
        g2.fillRoundRect(x,y,  100, 80,  10, 10);
        g2.setColor(Color.white); 
        g2.setStroke(new BasicStroke(5));
        g2.drawRoundRect(x,y,  100, 80,  10, 10);
        g2.setFont(new Font("Arial",Font.PLAIN,20));
        g2.drawString("yes",x+50,y +30);
        g2.drawString("no",x +50,y+60);

    }

    private void drawYesArrow(){
        g2.setColor(Color.blue);
                int x = panel.skjermBredde-150+20;
                int y = panel.skjermHoyde-190+25;
        
                int x2 = x+5;
                int y2 = y;
                
                g2.drawLine(x,y,x2,y2);
    }
    
    private void drawNoArrow(){
        g2.setColor(Color.blue);
                int x = panel.skjermBredde-150+20;
                int y = panel.skjermHoyde-190+55;
        
                int x2 = x+5;
                int y2 = y;
            
                g2.drawLine(x,y,x2,y2);
    }

    public void drawArrow(){
        //if game-option = true
        if (panel.gameOption){
            //if arrow == yes
            if (panel.arrowYes){
                drawYesArrow();
            } else if (!panel.arrowYes){
                //array = no
                drawNoArrow();
            }
    }

        

    }
    public void drawTextBox(){
        
        Color c = new Color(122,176,255);
        
        g2.setColor(c);
        g2.fillRoundRect(45,panel.skjermHoyde-105,  panel.skjermBredde - 45*2, 60,  10, 10);
        g2.setColor(Color.white); 
        

        g2.fillRoundRect(50,panel.skjermHoyde-100,  panel.skjermBredde - 50*2, 50,  10, 10);
        g2.setColor(Color.blue);
        g2.drawRoundRect( 50,panel.skjermHoyde-100,  panel.skjermBredde - 50*2, 50,  10, 10);
        g2.setColor(Color.black);
        g2.drawRoundRect(45,panel.skjermHoyde-105,  panel.skjermBredde - 45*2, 60,  10, 10);
        

        g2.setFont(new Font("Arial",Font.PLAIN,20));
        g2.drawString(panel.textString,50+30,panel.skjermHoyde-100+30+2);

    }


    public void drawMenu(){

        

        drawMenuBox(menuX,menuY);
        Meny m = panel.menu; 

        drawMenuArrow(menuX+menuArrowOffsetX,menuY+menuArrowOffsetY,m.slotPick,avstand);

        drawMenuText(menuX+menuTextOffsetX,menuY+menuArrowOffsetY,m,avstand);

    }


    public void drawMenuBox(int x,int y){
        
        g2.drawImage(boardSprite,x,y,null);

        //g2.drawImage(boardSprite,panel.skjermBredde-200,panel.skjermHoyde-panel.tileSize*12,null);
    }

    public void drawSubMenuItems(boolean arrow){
        int startX = menuX;
        int startY= menuY;

        drawMenuBox(startX,startY);
        Items slot = panel.menu.items;
        if (arrow){
            drawMenuArrow(startX+menuArrowOffsetX,startY+menuArrowOffsetY,slot.indeks,avstand);
        }
        
        drawMenuTextItem(startX+menuTextOffsetX,startY+menuTextOffsetY,slot,avstand,slot.indeks);
    }

    public void drawSubMenuResources(boolean arrow){
        int startX = menuX;
        int startY= menuY;
        drawMenuBox(startX,startY);
        
        Resources slot = panel.menu.resources;
        if (arrow){
            drawMenuArrow(startX+menuArrowOffsetX,startY+menuArrowOffsetY,slot.indeks,avstand);
        }
        drawMenuTextResorces(startX+menuTextOffsetX,startY+menuTextOffsetY,slot,avstand);
    }

    public void drawSubMenuMaterials(boolean arrow){
        int startX = menuX;
        int startY= menuY;

        drawMenuBox(startX,startY);
        Materials slot = panel.menu.materials;
        if (arrow){
            drawMenuArrow(startX+menuArrowOffsetX,startY+menuArrowOffsetY,slot.indeks,avstand);
        }
        
        drawMenuTextMaterials(startX+menuTextOffsetX,startY+menuTextOffsetY,slot,avstand,slot.indeks);
    }

    private void drawMenuText(int startX,int startY,Meny m,int avstand){
        g2.setColor(Color.white);
        g2.setFont(new Font("Chalkduster",1,20));
        for (int i = 0;i < m.antSlot;i++){
            g2.drawString(m.meny[i].getName(),startX,startY);
            startY += avstand;
        }
    }

    private void drawMenuTextItem(int startX,int startY,Items sc,int avstand,int indeks){
        int j = indeks-6;
        if (j<0){
            j = 0;
        }

        g2.setFont(new Font("Chalkduster",1,18));
        g2.setColor(Color.white);
        for (int i = j;i < sc.content.size();i++){
            if (sc.content.get(i).getQuantity()>0){
                
                g2.drawString(sc.content.get(i).getName(),startX,startY);
                g2.drawImage(sc.content.get(i).sprite,startX+80,startY-20,null);
                
                startY += avstand;
            }
        }
    }
    

    private void drawMenuTextResorces(int startX,int startY,Resources sc,int avstand){
        
        g2.setColor(Color.white);
        g2.setFont(new Font("Chalkduster",1,18));

        for (int i = 0;i < sc.content.size();i++){
            if (i<sc.content.size()){
                g2.drawString(sc.content.get(i).getName(),startX,startY);
                g2.drawImage(sc.content.get(i).sprite,startX+50,startY-20,null);
                g2.drawString(Integer.toString(sc.content.get(i).getQuantity()),startX+100,startY);
                startY += avstand;
            }
                
        }
    }

    private void drawMenuTextMaterials(int startX,int startY,Materials sc,int avstand,int indeks){
        int j = indeks-6;
        if (j<0){
            j = 0;
        }
        
        g2.setFont(new Font("Chalkduster",1,18));
        g2.setColor(Color.white);
        for (int i = j;i < sc.content.size();i++){
            
            g2.drawString(sc.content.get(i).getName(),startX,startY);
            g2.drawImage(sc.content.get(i).sprite,startX+65,startY-20,null);
            g2.drawString(Integer.toString(sc.content.get(i).getQuantity()),startX+100,startY);
            startY += avstand;
            
        }
    }

    private void drawMenuArrow(int startX,int startY,int indeks,int avstand){
        g2.setStroke(new BasicStroke(5));
        g2.setColor(Color.blue);
        if (indeks > 6){
            indeks = 6;
        }
        g2.drawImage(arrowSprite,startX-15,startY-15+avstand*indeks,null);
        /* 
        //g2.drawImage(arrowSprite,startX,startY,null);
        switch (indeks){
            case 2:
                
                int x = startX;
                int y = startY += avstand*2;
        
                g2.drawImage(arrowSprite,startX-15,startY+avstand*indeks,null);

                break;
            case 1:
                x = startX;
                y = startY += avstand;
                g2.drawImage(arrowSprite,startX-15,startY-avstand/2,null);
                break;

            case 0:
                x = startX;
                y = startY ;
                g2.drawImage(arrowSprite,startX-15,startY-avstand/2,null);
                break;
        }
        */

        
    }

    private void drawItemOptionBar(){
        drawItemOptionBox();
        drawItemOptionText();
        drawMenuArrow(330,300-63,panel.menu.items.content.get(panel.menu.items.indeks).indeks,30);
    }

    private void drawItemOptionBarMaterials(){
        drawItemOptionBox();
        drawItemOptionText();
        drawMenuArrow(330,300-63,panel.menu.materials.content.get(panel.menu.materials.indeks).indeks,30);
    }

    private void drawItemOptionBarResources(){
        drawItemOptionBox();
        drawItemOptionText();
        drawMenuArrow(330,300-63,panel.menu.resources.content.get(panel.menu.resources.indeks).indeks,30);
    }


    private void drawItemOptionBox(){
        g2.drawImage(smallBoardSprite,300,200,null);
    }
    private void drawItemOptionText(){
        g2.setFont(new Font("Chalkduster",1,20));
        g2.setColor(Color.black);
        
        g2.drawString("cancel",360,300-50+20);
        g2.drawString("equip",360,300-80+20);

        if (panel.menu.items.content.get(panel.menu.items.indeks).maxIndeks == 2){
            g2.drawString("unequip",360,300-20+20);
        }
    }

    private void drawActiveTool(){
        if (!panel.spiller.activeTool.equals("null")){
        g2.drawImage(iconFrame,10,10,70,70,null);
        g2.drawImage(panel.menu.items.contentLibrary.get(panel.spiller.activeTool).sprite,20,20,48,48,null);
        }
    }
    private void drawItemBar(){
        int x = (panel.skjermBredde-475)/2;
        int y = panel.skjermHoyde-100;
        g2.setFont(new Font("Chalkduster",1,12));
        g2.setColor(Color.black);

        g2.drawImage(itemBar,x,y,475,60,null);
       

        int avstand = 51;
        for (int i = 0;i<9;i++){
            if (panel.itemB.itemBarContent[i] != null){
                g2.drawImage(panel.itemB.itemBarContent[i].sprite,x+20+avstand*i,y+20,null);
                g2.drawString(Integer.toString(panel.itemB.itemBarContent[i].getQuantity()),x+40+avstand*i,y+20);
            }
        }
        g2.drawImage(itemBarFrame,x+6+(avstand*panel.itemB.indexValue),y+2,null);
            
       
            
        
    }
}
