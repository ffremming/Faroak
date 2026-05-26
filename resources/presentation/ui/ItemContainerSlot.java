package resources.presentation.ui;

import resources.app.GamePanel;
import resources.domain.tile.Tile;
import resources.domain.inventory.Inventory;
import resources.domain.inventory.Item;
import resources.domain.inventory.Stack;
import resources.domain.inventory.ItemManager;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import resources.app.GamePanel;
import java.awt.BasicStroke;

import resources.domain.inventory.Inventory;
import resources.domain.inventory.Item;
import resources.domain.inventory.Stack;

public class ItemContainerSlot extends Component{

    Item item;
    int col;
    int row;
    Inventory inventory;
    int number; 

    public ItemContainerSlot(GamePanel panel,int x,int y,int width,int height,int col,int row,int number,Inventory inventory) {
        super(panel);


        
        borderSize = 1;
        
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        setBackground(Color.green);
        setForeGround(Color.black);
        this.col = col;
        this.row = row;
        this.inventory = inventory;
        this.number = number;
    }



    public void addItem(Item item){
        this.item = item;
    }


    public void removeItem(){
        item = null;
    }   

    public void hover(){
        hover = true;
    }

    public void press(){
    }

    public void draw(Graphics2D g2,int count,Inventory inventory){
       
        drawRect(g2);

        
    }
    @Override
    public void drawRect(Graphics2D g2){
       
        
        if (hover){
            setBackground(Color.white);
            setForeGround(new Color(240,240,240));
        } else {
            setBackground(Color.gray);
            setForeGround(Color.black);
        }


        width = (container.width-(8*(container.cols+1)))/(container.cols) ;
        height = (container.height- (8*(container.rows+1)))/(container.rows) ;

       
        
        
        
        x = container.x + col*(width+8) +8 ;
        y = container.y + row*(height+8) +8;
        g2.setColor(background);
        g2.fillRect(x,y,width,height);
        g2.setColor(foreground);
        g2.drawRect(x,y,width,height);

        drawContent(g2,number,inventory);
    }

    public void drawRectInPos(Graphics2D g2,int x,int y,boolean indexed){
        
        if (hover){
            setBackground(Color.white);
            setForeGround(new Color(240,240,240));
        } else {
            setBackground(Color.gray);
            setForeGround(Color.black);
        }

        width = (container.width-(8*(container.cols+1)))/(container.cols) ;
        height = width;

        this.x = x;
        this.y = y;

        g2.setColor(background);
        g2.fillRect(x,y,width,height);
        g2.setColor(foreground);
        g2.drawRect(x,y,width,height);

        if (inventory!= null){
            if (inventory.getStack(number) != null){
                if (!inventory.getStack(number).getName().equals("empty")){
                    if (inventory.getStack(number)!= null){
                        BufferedImage image = panel.imageContainer.getItemImage(inventory.getStack(number).getName());
                        
                        g2.drawImage(image, x, y, width, height, null);
                    }
                }
            }
        }
        if (indexed){
            g2.setColor(Color.white);
            g2.setStroke(new BasicStroke(5));
            g2.drawRect(x-1,y-1,width+2,height+2);

            g2.setColor(foreground);
            g2.setStroke(new BasicStroke(1));
            
        }
        


    }
    public void drawContent(Graphics2D g2,int count,Inventory inventory){
        if (inventory!= null){
            if (inventory.getStack(count) != null){
                if (!inventory.getStack(count).getName().equals("empty")){
                    if (inventory.getStack(count)!= null){
                        BufferedImage image = panel.imageContainer.getItemImage(inventory.getStack(count).getName());
                        
                        g2.drawImage(image, x, y, width, height, null);
                        g2.drawString(inventory.getStack(count).getAmount()+"", x+width-10, y+height-10);
                    }
                }
            }
        }
    }



    public void mousePressed(MouseEvent e){

        switchItems();
       
    }

    private void switchItems(){
    
        Stack tempInHand = panel.player.getTempInHand();
        Stack thisStack = getStack();
    
        panel.player.setTempInHand(thisStack);
        setStack(tempInHand);
       
    }

    



    public Item getItem(){
        
        return item;
        
    }
    //getPointerInfo().getLocation()

    public void mouseMoved(MouseEvent e){
        hover();
    }

    private Stack getStack(){
        return inventory.getStack(number);
    }

    private void setStack(Stack tempInHand) {
        inventory.setStack(number,tempInHand);
    }
}
