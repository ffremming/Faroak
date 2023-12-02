package ressurser.main;

import ressurser.Tiles.TileManager;
import ressurser.chunkSystem.ChunkSystem;
import ressurser.chunkSystem.ChunkSystem;
import ressurser.drawing.DrawingManager;
import ressurser.entity.EntityHandler;
import ressurser.entity.spiller.Spiller;
import ressurser.enviroment.EnviromentManager;
import ressurser.enviroment.Lightning;
import ressurser.itemBar.ItemBar;
import ressurser.main.GUIMenu.MenuState;
import ressurser.main.interactions.InventoryInteraction;
import ressurser.main.interactions.MenuInteraction;
import ressurser.main.interactions.OptionInteraction;
import ressurser.main.interactions.PlayInteractionManager;
import ressurser.meny.Meny;
import ressurser.objects.ObjectManager;
import ressurser.sprites.SpriteLoader;
import ressurser.tileSpriteGenerator.ImagePainter;
import ressurser.worldGeneration.TerrainGenSimplex;
import ressurser.worldGeneration.dungeonGenerator.DungeonManager;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Graphics;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class GamePanel extends JPanel implements Runnable{
    public int tileSize = 64;

    public boolean gameOption = false;
    public boolean arrowYes = false;
    
    public final int screenHeight = tileSize/2 *18;
    public final int screenWidth = tileSize/2 *26;
    

    int aktivStreng = 1;
    
   

    public Thread gameThread; 
    public Graphics2D g ;

    // object components:
    public TerrainGenSimplex terrainGen;
    public ObjectManager objM;
    public TileManager tileM;
    public DungeonManager dungeonM;
    public ImagePainter imageP;
    
    public ChunkSystem chunkSystem;
    public PlayInteractionManager interactionPlay;
    public MenuInteraction interactionMenu;
    public InventoryInteraction interactionInventory;
    public OptionInteraction interactionOption;

    public KeyHandler input;
    public MouseHandler mouse;
    public Spiller spiller;
    public Meny menu;
    public CollisionChecker collisionC;
    public EntityHandler entityH;
    public Lightning light;
    public EnviromentManager enviromentM;
    public ItemBar itemB;
    public MapHandler mapH;
    public DrawingManager drawingM;
    public MenuState menuStateUI;
    public SpriteLoader spriteLoader;




    Graphics2D g2;
    public UI UI = new UI(this);
    
    
    
    
    public int animatedSpriteCounter = 0; 
    public int aniTeller = 0;
    
    int PMoveValue = 0;

    public Boolean textOn = false;
    public String textString;

    public int gameState;
    public final int PLAYSTATE  = 1;
    public final int DIALOGSTATE = 2;
    public final int MENUSTATE = 3;
    public final int INVENTORYSTATE = 4;
    public final int OPTIONSTATE = 4;

    
    public boolean dialoge;
    public boolean textBox = false;

    boolean newGame;
    public JFrame frame;
    
    public GamePanel(JFrame frame,boolean newGame){
        this.frame = frame;
        this.g=(Graphics2D) frame.getGraphics();
        
        this.newGame = newGame;
        this.setPreferredSize(new Dimension(screenWidth,screenHeight));
        this.setBackground(Color.black);
        this.setDoubleBuffered(true);
        
        //important - needs to make all this mess cleaner
        this.chunkSystem = new ChunkSystem(this);

        generateMap();
        
        setUpObjects();
        addKeyListener(input);
       
        this.setFocusable(true);
        requestFocus();
       

        
        
    }

    private void generateMap(){
        spriteLoader = new SpriteLoader(this);
        mapH = new MapHandler(this);
        spiller = new Spiller(this,textString, 2000,500,(short) 40,(short) 128,(short) 48,(short) 48, (short)8,(short)( 128-48));
        terrainGen = new TerrainGenSimplex(mapH.mapWidth,mapH.mapHeight,true);
        imageP = new ImagePainter();
        objM = new ObjectManager(this,newGame);
        tileM = new TileManager(this,newGame);
    }
   
    private void setUpObjects(){
        dungeonM = new DungeonManager(this);
        interactionPlay = new PlayInteractionManager(this);

        input = new KeyHandler(this);
        //Smouse = new MouseHandler(this);
        menu = new Meny(this);
        collisionC = new CollisionChecker(this);
        entityH = new EntityHandler(this);
        light = new Lightning(this);
        enviromentM = new EnviromentManager(this);
        itemB = new ItemBar(this);
        drawingM = new DrawingManager(this);
        menuStateUI = new MenuState(this);
    }


    public void startGameThread(){
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public void run() {
        
        while (gameThread != null){
            
            update();
            repaint();
            sleepGameFrame();
        }
    }

    public void update(){
        

        if (gameState == PLAYSTATE ){
            playUpdate();

        } else if (gameState == MENUSTATE){
            //ingenting hittil
        } else if (gameState == DIALOGSTATE){
           //?
        }else if (gameState == OPTIONSTATE){
            
        
        }  

        System.out.println(tileM.tileMap[mapH.activeMapType][mapH.activeMapNumber][0][0]);
    }
    
    //has to be changed
    public void playUpdate(){
        

        enviromentM.updateTicks();
        //entityH.update();
        
        //update players action
        spiller.updateAction();
       // updateAction()
        

        //update animation - shoudl be done in envoirment.
        animatedSpriteCounter();
    }


    public void paintComponent(Graphics g){
        super.paintComponents(g); 
        drawingM.draw(g);
    }


    

    //has to be changed
    public void animatedSpriteCounter(){        //skal kjøres hver gang spillet oppdaterer
        animatedSpriteCounter ++;
        if (animatedSpriteCounter%32 == 0){  //aktiveres hver 64
            animatedSpriteCounter = 0;
            aniTeller ++;
            aniTeller = aniTeller % 2;
        }
    }

    //has to be changed
   

    public void sleepGameFrame(){
        try {
            double gjenstaaendeTid = drawingM.nestetid-System.nanoTime();
            gjenstaaendeTid = gjenstaaendeTid/1000000;
            
            if (gjenstaaendeTid<0){
                gjenstaaendeTid = 0;
            }
            Thread.sleep((long)gjenstaaendeTid);

            drawingM.nestetid += drawingM.splitTime;
        } catch (InterruptedException e) {e.printStackTrace();
        }
    }



    

    public int getFrameHeight(){
        return (int)frame.getBounds().getHeight();
    }

    public int getFrameWidth(){
        return (int)frame.getBounds().getWidth();
    }
}