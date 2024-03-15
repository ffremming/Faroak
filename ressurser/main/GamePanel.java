package ressurser.main;

import ressurser.baseEntity.playable.Playable;
import ressurser.baseEntity.playable.Inventory.ItemManager;
import ressurser.baseEntity.tile.TileManager;
import ressurser.chunkSystem.WorkingMemory;
import ressurser.drawing.Camera;
import ressurser.enviroment.EnviromentManager;
import ressurser.main.GUIMenu.Button;
import ressurser.main.GUIMenu.Container;
import ressurser.main.GUIMenu.UserInferface;
import ressurser.worldGeneration.TerrainGenSimplex;
import java.awt.Dimension;
import java.awt.Color;
import java.lang.reflect.InvocationTargetException;
import java.awt.Graphics;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;




public class GamePanel extends JPanel implements Runnable{
    public int tileSize = 64;
    
    public final int screenHeight = tileSize *12;
    public final int screenWidth = tileSize *20;
    
   
    
    public Thread gameThread; 
    public Camera camera;
    

    // object components:
    GenerationManager generationM;
    public TerrainGenSimplex terrainGen;
    
    public TileManager tileM;
    public ItemManager itemM;
    public ImageContainer imageContainer;
    

    public EnviromentManager enviromentM;
    public MapHandler mapH;
    
    public Playable player;
    public Keys keys;
    public Mouse mouse;
    public InputHandlingSystem inputHandlingSystem;
    public Container UI;
    public UserInferface userInterface;
    public WorkingMemory world;
    //public ItemContainer container;
   
    
    public int height;
    public int width;
    
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
        addMouseWheelListener(mouse);
       
    
       
        
        
       
    }

    
   
    private void setUpObjects(){
       
        
        generationM = new GenerationManager(this);
        generationM.generateMap();

        enviromentM = new EnviromentManager(this);
       
        keys = new Keys(this);
        inputHandlingSystem = new InputHandlingSystem(this);
        mouse = new Mouse(this);

        UI = new Container(this,100,100);
        UI.padding = 50;
        UI.border = 30;
        //UI.setVisible(true);
        UI.add(new Button(this,"knapp"));
       
        UI.add(new Button(this,"knapp"));
        UI.add(new Button(this,"knapp"));

       
        userInterface = new UserInferface(this,0,0);
        userInterface.setVisible(true);
        userInterface.enable();
        
        
        
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

           
            
          
            repaint();
           
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
        world.simulate(); 
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

    public void newSeed() {
        generationM.newSeed();
        world.simulate();
        camera.follow(player);
    }

    
}