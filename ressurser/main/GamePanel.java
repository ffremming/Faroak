package ressurser.main;

import ressurser.baseEntity.HitBox;
import ressurser.baseEntity.playable.Playable;
import ressurser.baseEntity.tile.TileManager;
import ressurser.chunkSystem.ChunkSystem;

import ressurser.drawing.Camera;
import ressurser.drawing.DrawingManager;
import ressurser.entity.EntityHandler;
import ressurser.entity.spiller.Spiller;
import ressurser.enviroment.EnviromentManager;
import ressurser.enviroment.Lightning;
import ressurser.itemBar.ItemBar;
import ressurser.main.GUIMenu.Button;
import ressurser.main.GUIMenu.Container;
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
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.lang.reflect.InvocationTargetException;
import java.awt.Graphics;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;



public class GamePanel extends JPanel implements Runnable{
    public int tileSize = 64;

    public boolean gameOption = false;
    public boolean arrowYes = false;
    
    public final int screenHeight = tileSize *12;
    public final int screenWidth = tileSize *20;
    

    int aktivStreng = 1;
    
   

    public Thread gameThread; 
    public Graphics2D g ;
    public Camera camera;

    

    // object components:
    GenerationManager generationM;
    public TerrainGenSimplex terrainGen;
    public ObjectManager objM;
    public TileManager tileM;
    public DungeonManager dungeonM;
    public ImagePainter imageP;
    public ImageContainer imageContainer;
    
    public ChunkSystem chunkSystem;
    public PlayInteractionManager interactionPlay;
    public MenuInteraction interactionMenu;
    public InventoryInteraction interactionInventory;
    public OptionInteraction interactionOption;

    public KeyHandler input;
    
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

    public Playable player;
    public Keys keys;
    public Mouse mouse;
    public InputHandlingSystem inputHandlingSystem;
    public Container UI;


    Graphics2D g2;
   
    
    
    
    
 
    
   

   

    public int gameState;
    public final int PLAYSTATE  = 1;
    public final int DIALOGSTATE = 2;
    public final int MENUSTATE = 3;
    public final int INVENTORYSTATE = 4;
    public final int OPTIONSTATE = 4;

    


    boolean newGame;
    public JFrame frame;
    
    public GamePanel(JFrame frame,boolean newGame){
        this.frame = frame;
        this.newGame = newGame;
        this.setPreferredSize(new Dimension(screenWidth,screenHeight));
        this.setBackground(Color.black);
        this.setDoubleBuffered(true);
        this.setFocusable(true);
        requestFocus();
        
      
        

        
        //OBJECT INITIATION
        setUpObjects();

        //INPUTS
        addKeyListener(keys);
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
       
    
       
        
        
        
    }

    
   
    private void setUpObjects(){
       

        generationM = new GenerationManager(this);
        generationM.generateMap();

        enviromentM = new EnviromentManager(this);
       
        keys = new Keys(this);
        inputHandlingSystem = new InputHandlingSystem(this);
        mouse = new Mouse(this);

        UI = new Container(this,0,0,400,400);
        UI.padding = 50;
        UI.border = 30;
        //UI.setVisible(true);
        UI.add(new Button(this,"knapp"));
       
        UI.add(new Button(this,"knapp"));
        UI.add(new Button(this,"knapp"));
       

        generationM.initiate();

        
        camera = new Camera(this,"camera",0,0,(short)screenWidth,(short)screenHeight);

    }


    public void startGameThread(){
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public void run() {
        
        while (gameThread != null){
            
            long drawStart = System.nanoTime();
            repaint();
            long drawend = System.nanoTime();
            Runnable updateThread = new Runnable() {
                public void run() {
                    update();
                };
            };
            try {
                SwingUtilities.invokeAndWait(updateThread);
            } catch (InvocationTargetException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            sleepGameFrame();
        }
    }

    public void update(){
        
        enviromentM.updateTicks();
        inputHandlingSystem.update();
        chunkSystem.workingMemory.simulate();
        if (gameState == PLAYSTATE ){
            //playUpdate();

        } else if (gameState == MENUSTATE){
            //ingenting hittil
        } else if (gameState == DIALOGSTATE){
           //?
        }else if (gameState == OPTIONSTATE){
            
        
        }  

        
    }
    
   


    public void paintComponent(Graphics g){
        
        super.paintComponent(g); 
        camera.draw(g);
        
    }


    

  
   

    //has to be changed
   

    public void sleepGameFrame(){
        try {
            double remainingTime = camera.nextDrawTime-System.nanoTime();
            remainingTime = remainingTime/1000000;
            
            if (remainingTime<0){
                remainingTime = 0;
            }
            Thread.sleep((long)remainingTime);

            camera.nextDrawTime += camera.splitTime;
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