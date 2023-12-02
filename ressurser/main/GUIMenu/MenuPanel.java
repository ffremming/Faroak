package ressurser.main.GUIMenu;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Timer;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import ressurser.main.GamePanel;

public class MenuPanel extends JPanel {
    
    JFrame frame;

    public MenuPanel(JFrame frame){
        this.frame = frame;
        this.setPreferredSize(new Dimension(32*20,32*15));
        this.setBackground(Color.black);
        this.setDoubleBuffered(true);
        //this.setFocusable(true);
       

        JButton b = new JButton("New Game");
        newGameActionListener newGame = new newGameActionListener(this);
        b.addActionListener(newGame);

        LoadGameActionListener loadGame = new LoadGameActionListener();
        JButton b2 = new JButton("Load Game");
        b2.addActionListener(loadGame);

        add(b);
        add(b2);

        



    }
    public class newGameActionListener implements  ActionListener{
        MenuPanel p;

        public newGameActionListener(MenuPanel p){
            this.p = p;
        }

        public void actionPerformed(ActionEvent e) {
            System.out.println("new game");
            JLabel headLine = new JLabel("Loader spill...");
        
            //headLine.setBounds(0,50,50,50);
            headLine.setPreferredSize(new Dimension(100, 50)); // set size
            headLine.setBounds(20*16-50,15*16-50,100,100);
            
            headLine.setVisible(true); // make visible
            headLine.setForeground(Color.white);
            p.add(headLine);
            frame.validate();
            headLine.repaint();
            frame.repaint();
            frame.getContentPane().remove(p);
            
            
            
            
            GamePanel gamePanel = new GamePanel(frame,true);
            
            
            frame.getContentPane().add(gamePanel);
            frame.setSize(gamePanel.screenWidth,gamePanel.screenHeight);
            frame.add(gamePanel);
            gamePanel.requestFocusInWindow();
          

            

            gamePanel.startGameThread();

        }
    }

    public class LoadGameActionListener implements  ActionListener{
        public void actionPerformed(ActionEvent e) {
           System.out.println("load game");
           JLabel headLine = new JLabel("Loader spill...");
           add(headLine);
           repaint();
        }
    }

    
}
