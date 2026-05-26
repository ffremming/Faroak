package resources.app;

import resources.presentation.image.ImageContainer;
import resources.presentation.image.ImageResources;
import resources.presentation.camera.Camera;
import resources.presentation.ui.Container;
import resources.presentation.ui.Button;
import resources.presentation.ui.UserInterface;
import resources.input.Keys;
import resources.input.Mouse;
import resources.input.InputHandlingSystem;
import resources.world.MapHandler;
import resources.world.ChunkSystem;
import resources.world.WorkingMemory;
import resources.domain.entity.BaseEntity;
import resources.domain.entity.Entity;
import resources.domain.tile.Tile;
import resources.domain.tile.TileManager;
import resources.domain.object.GameObject;
import resources.domain.player.Playable;
import resources.domain.player.Moveable;
import resources.domain.inventory.ItemManager;
import resources.environment.EnvironmentManager;
import resources.generation.factory.EntityFactory;
import resources.geometry.HitBox;
import resources.geometry.Vector;

import resources.domain.player.Playable;
import resources.domain.inventory.ItemManager;
import resources.domain.tile.TileManager;
import resources.world.WorkingMemory;
import resources.presentation.camera.Camera;
import resources.environment.EnvironmentManager;
import resources.presentation.ui.Button;
import resources.presentation.ui.Container;
import resources.presentation.ui.UserInterface;
import java.awt.Dimension;
import java.awt.Color;
import java.lang.reflect.InvocationTargetException;
import java.awt.Graphics;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;




public class GamePanel extends JPanel implements Runnable, GameContext {
    public int tileSize = 64;
    
    public final int screenHeight = tileSize *12;
    public final int screenWidth = tileSize *20;
    
    public Thread gameThread; 
    public Camera camera;
    
    

    // object components:
    GenerationManager generationM;

    public TileManager tileM;
    public ItemManager itemM;
    public ImageContainer imageContainer;
    

    public EnvironmentManager environmentM;
    public MapHandler mapH;
    
    public Playable player;
    public Keys keys;
    public Mouse mouse;
    public InputHandlingSystem inputHandlingSystem;
    public Container UI;
    public UserInterface userInterface;
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

        environmentM = new EnvironmentManager(this);
       
        keys = new Keys(this);
        inputHandlingSystem = new InputHandlingSystem(this);
        mouse = new Mouse(this);

        UI = new Container(this,100,100);
        /* 
        UI.setBackground(Color.red);
        UI.padding = 50;
        UI.border = 30;
        UI.setVisible(true);
        UI.add(new Button(this,"knapp"));
       
        UI.add(new Button(this,"knapp"));
        UI.add(new Button(this,"knapp"));
        */
       
        userInterface = new UserInterface(this,0,0);
        userInterface.setVisible(true);
        userInterface.enable();
        
        
        
        generationM.initiate();


        camera = new Camera(this,"camera",0,0,(short)(screenWidth+400),(short)(screenHeight+400));
        
    }


    public void startGameThread(){
        gameThread = new Thread(this);
        gameThread.start();
    }


   

   // @Override
    public void run2() {
       
       


        while (gameThread != null){

           
            
          
            repaint();
            
            Runnable updateThread = new Runnable() {
                public void run() {
                    //update();
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

    @Override
public void run() {
    final int TARGET_FPS = 60; // Target frames per second
    final long OPTIMAL_TIME = 1000000000 / TARGET_FPS; // Optimal time per frame in nanoseconds

    long lastLoopTime = System.nanoTime();
    long lastFpsTime = 0;
    int fps = 0;

    while (gameThread != null) {
        long now = System.nanoTime();
        long updateLength = now - lastLoopTime;
        lastLoopTime = now;
        
        double delta = updateLength / ((double) OPTIMAL_TIME);

        lastFpsTime += updateLength;
        fps++;

        // Update the game state
        Runnable updateThread = new Runnable() {
            public void run() {
                long before = System.nanoTime();
                update(delta);
                long after = System.nanoTime();
                camera.addbackendPrintData("update time "+ ((after-before)/1000) +"microseconds");
              
              
            }
        };
        try {
            SwingUtilities.invokeAndWait(updateThread);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Repaint the game
        long before = System.nanoTime();
        repaint();
        long after = System.nanoTime();
        camera.addbackendPrintData("repaint time "+ ((after-before)/1000) +"microseconds");
        // If one second has passed, reset the FPS counter, updates Printable values
        if (lastFpsTime >= 1000000000) {

            lastFpsTime = 0;
            camera.setObservedFPS(fps);
            fps = 0;
        }
        

        // Sleep until the next frame
        try {
            Thread.sleep((lastLoopTime - System.nanoTime() + OPTIMAL_TIME) / 1000000);
        } catch (Exception e) {
            // Handle exception
        }
    }
}

    /**
     * logic update.
     *  Called every frame
     * at 0.1 ms regularly, and spikes at 5 ms every update on workingMemory (every second)
     */
    public void update(double delta){
        
        
        environmentM.updateTicks();
        inputHandlingSystem.update(delta);
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

    // ---- GameContext implementation: forwards to existing fields so the rest of
    // the codebase can depend on the GameContext interface instead of GamePanel directly.

    @Override public resources.world.WorldRuntime  world()            { return world; }
    @Override public TileManager                   tiles()            { return tileM; }
    @Override public ItemManager                   items()            { return itemM; }
    @Override public EnvironmentManager            environment()      { return environmentM; }
    @Override public MapHandler                    mapHandler()       { return mapH; }
    @Override public Playable                      player()           { return player; }
    @Override public Camera                        camera()           { return camera; }
    @Override public ImageContainer                images()           { return imageContainer; }
    @Override public UserInterface                 userInterface()    { return userInterface; }
    @Override public Keys                          keys()             { return keys; }
    @Override public Mouse                         mouse()            { return mouse; }
    @Override public InputHandlingSystem           input()            { return inputHandlingSystem; }
    @Override public JFrame                        frame()            { return frame; }
    @Override public int                           tileSize()         { return tileSize; }
    @Override public int                           screenWidth()      { return screenWidth; }
    @Override public int                           screenHeight()     { return screenHeight; }
}