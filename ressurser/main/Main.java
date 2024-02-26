package ressurser.main;
import javax.swing.JFrame;

import ressurser.main.GUIMenu.MenuPanel;

public class Main{
    public static void main(String[] args) {
    JFrame vindu = new JFrame();
    
    //make a new panel - where yoi can choose generation

    

    vindu.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    vindu.setResizable(true);
    vindu.setTitle("nytt vindu");
    
    MenuPanel menu = new MenuPanel(vindu);
    vindu.setSize(640,400);

    vindu.getContentPane().add(menu);
    //vindu.setLocationRelativeTo(null);
    vindu.setVisible(true);
    vindu.toFront();
    }
}
