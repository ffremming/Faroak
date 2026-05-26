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
import javax.swing.JFrame;

public class Main{
    public static void main(String[] args) {
    JFrame frame = new JFrame();
    
    //make a new panel - where yoi can choose generation

    

    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setResizable(true);
    frame.setTitle("nytt vindu");
    
     System.out.println("new game");
   
   
   
  
    
    
    
    
    GamePanel gamePanel = new GamePanel(frame,true);
    
    
    frame.getContentPane().add(gamePanel);
    frame.setSize(gamePanel.screenWidth,gamePanel.screenHeight);
    frame.add(gamePanel);
    gamePanel.requestFocusInWindow();
    
    
    
    
    gamePanel.startGameThread();
    frame.setVisible(true);
    frame.toFront();

    /** 
    //MenuPanel menu = new MenuPanel(vindu);

    vindu.setSize(640,400);

    vindu.getContentPane().add(menu);
    //vindu.setLocationRelativeTo(null);
    vindu.setVisible(true);
    vindu.toFront();
    
    */
    }
}
