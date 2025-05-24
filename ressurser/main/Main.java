package ressurser.main;
import javax.swing.JFrame;
import javax.swing.JLabel;

import ressurser.main.GUIMenu.MenuPanel;

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
