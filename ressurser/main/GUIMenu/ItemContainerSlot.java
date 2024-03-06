package ressurser.main.GUIMenu;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;

import ressurser.main.GamePanel;
import java.awt.BasicStroke;
import ressurser.meny.items.Item;

public class ItemContainerSlot extends Component{

    Item item;
    int col;
    int row;

    public ItemContainerSlot(GamePanel panel,int x,int y,int width,int height,int col,int row) {
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

    public void draw(Graphics2D g2){
       
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
    }

    public void drawRectInPos(Graphics2D g2,int x,int y){
        System.out.println("drawRect In pos");
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

    }



    public void mousePressed(MouseEvent e){

        switchItems();
       
    }

    private void switchItems(){
       
    }

    public Item getItem(){
        
        return item;
        
    }
    //getPointerInfo().getLocation()

    public void mouseMoved(MouseEvent e){
        hover();
    }
}
