package terrainGeneration;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import java.awt.*;


public class TerrainGenerationView {
    

    ProceduralGeneration generationModel;

    int height = 500;
    int width = 800;

    Graphics2D g2;

    ImagePanel ip;
    PopUpImagePanel popUpimageP;
    JFrame popUpmap;

    int cameraX = 0;
    int cameraY = 0;

    double pointerX;
    double pointerY;

    int camSpeed = 20;

    boolean zoom = false;



    public TerrainGenerationView(ProceduralGeneration generationModel){

        this.generationModel= generationModel; 
        JFrame terrainFrame = new JFrame();
        terrainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
       
        setupUI(terrainFrame);


        terrainFrame.setSize(width, height);      
        terrainFrame.setLocationRelativeTo(null);  
        terrainFrame.setVisible(true);

        setupPopUpwindow();
    }

    private void setupPopUpwindow(){
        popUpmap = new JFrame("map");

        popUpmap.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        popUpmap.setSize(200, 200);      
        
        popUpmap.setVisible(true);


        
        popUpimageP = new PopUpImagePanel();
        popUpmap.add(popUpimageP);
    }

    private void setupUI(JFrame terrainFrame){
        addFrameListenerSize(terrainFrame);
        JPanel terrainPanel = new JPanel();
        
        terrainFrame.add(terrainPanel);

        
        terrainPanel.setLayout(new BorderLayout());


       
        

        terrainPanel.add(addUIPanelNorth(),BorderLayout.NORTH);

        addSliderPanel(terrainPanel );

        addImagePanel(terrainPanel);

        terrainFrame.requestFocus();
       
    }

    private void addFrameListenerSize(JFrame frame){
        frame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                // This method will be called when the JFrame is resized
                width = frame.getWidth();
                height = frame.getHeight();
                ip.repaint();
                
            }
        });
    }

    private JPanel addUIPanelNorth(){
        JPanel UIPanelNorth = new JPanel();

        NewSeed newSeed = new NewSeed("new seed");

        HightColor colorChanger = new HightColor("change color");

         UIPanelNorth.add(newSeed);
         UIPanelNorth.add(colorChanger);
        UIPanelNorth.add(new ZoomButton("Zoom"));

        
        UIPanelNorth.setLayout(new GridLayout(1,3));


        return UIPanelNorth;
    }

    private void addImagePanel( JPanel terrainPanel ){

        ip = new ImagePanel();
        
        ip.addKeyListener(new ImageFramekeys());
        terrainPanel.add(ip,BorderLayout.CENTER);
        ip.requestFocus();
        
         Mouse mouseH = new Mouse();
         ip.addMouseListener(mouseH);
    }

    public class ImageFramekeys implements KeyListener{

            @Override
            public void keyTyped(KeyEvent e) {
                // TODO Auto-generated method stub
                
            }

            @Override
            public void keyPressed(KeyEvent e) {
                int code = e.getKeyCode();

                if (code == KeyEvent.VK_D){
                    cameraX+= camSpeed;
                    System.out.println("keu pressed");
                    
                }

                if (code == KeyEvent.VK_S){
                    cameraY+= camSpeed;
                    System.out.println("keu pressed");
                    
                }

                if (code == KeyEvent.VK_W){
                    cameraY-= camSpeed;
                    System.out.println("keu pressed");
                    
                }

                if (code == KeyEvent.VK_A){
                    cameraX-= camSpeed;
                    System.out.println("keu pressed");
                    

                }
                ip.repaint();
            }

            @Override
            public void keyReleased(KeyEvent e) {
                // TODO Auto-generated method stub
               
            }

        }

    private void addSliderPanel(JPanel terrainPanel){
        JTabbedPane tabbedPanel = new JTabbedPane();


        JPanel sliderPanel = new JPanel();

        sliderPanel.setLayout(new GridLayout(6,1));
        

        //adds different sliders
        sliderPanel.add(getMoistFrequencySlider());
        sliderPanel.add(getTempFrequencySlider());
        sliderPanel.add(getBaseFrequencySlider());
        sliderPanel.add(getContinentalSlider());
        sliderPanel.add(getPeaksSlider());

        //koefficentSlider
        JPanel koefficentPanel = new JPanel();
        koefficentPanel.setLayout(new GridLayout(6,1));
        koefficentPanel.add(getKSlider());
        koefficentPanel.add(getTempKSlider());
        koefficentPanel.add(getMoistKSlider());
       

        //adds all tabs.
        tabbedPanel.addTab("frequenzy",sliderPanel);
        tabbedPanel.addTab("koefficent",koefficentPanel);
        
        
        //adds the tabbedPane
        terrainPanel.add(tabbedPanel,BorderLayout.WEST);
    }
    
    private JSlider getMoistFrequencySlider(){
        int min = 1;
        int max = 10;
        int init = 5;
        JSlider moistFrequencySlider = new JSlider(JSlider.HORIZONTAL,min,max,init);

        moistFrequencySlider.setMajorTickSpacing(20);
        moistFrequencySlider.setMinorTickSpacing(5);
        moistFrequencySlider.setPaintTicks(true);
        moistFrequencySlider.setPaintLabels(true);

        moistFrequencySlider.setBorder(BorderFactory.createTitledBorder("moist frequenzy"));

        moistFrequencySlider.addChangeListener(
            new ChangeListener(){

                @Override
                public void stateChanged(ChangeEvent e){

                    JSlider source = (JSlider)e.getSource();
                   
                    int value = (int)source.getValue();

                    double newValue = (double) value / (max - min)/100;
                    generationModel.setMoistFrequenzy(newValue);
                    ip.repaint();
                    ip.requestFocus();
                }
             }); 
        return moistFrequencySlider;
    }

    private JSlider getTempFrequencySlider(){
        int min = 1;
        int max = 100;
        int init = 5;
        JSlider tempFrequencySlider = new JSlider(JSlider.HORIZONTAL,min,max,init);

        tempFrequencySlider.setMajorTickSpacing(20);
        tempFrequencySlider.setMinorTickSpacing(5);
        tempFrequencySlider.setPaintTicks(true);
        tempFrequencySlider.setPaintLabels(true);

        tempFrequencySlider.setBorder(BorderFactory.createTitledBorder("temperatur frequenzy"));

        tempFrequencySlider.addChangeListener(
            new ChangeListener(){

                @Override
                public void stateChanged(ChangeEvent e){

                    JSlider source = (JSlider)e.getSource();
                    int value = (int)source.getValue();

                    double newValue = (double) value / (max - min)/100;
                    generationModel.setTempFrequenzy(newValue);
                    ip.repaint();
                    ip.requestFocus();
                }
             }); 
        return tempFrequencySlider;
    }

    private JSlider getBaseFrequencySlider(){
        int min = 1;
        int max = 10;
        int init = 5;
        JSlider baseFrequencySlider = new JSlider(JSlider.HORIZONTAL,min,max,init);

        baseFrequencySlider.setMajorTickSpacing(2);
        baseFrequencySlider.setMinorTickSpacing(1);
        //baseFrequencySlider.set
        baseFrequencySlider.setPaintTicks(true);
        baseFrequencySlider.setPaintLabels(true);

        baseFrequencySlider.setBorder(BorderFactory.createTitledBorder("hight frequenzy"));

        baseFrequencySlider.addChangeListener(
            new ChangeListener(){

                @Override
                public void stateChanged(ChangeEvent e){

                    JSlider source = (JSlider)e.getSource();
                   
                    int value = (int)source.getValue();

                    double newValue = (double) value / (max - min)/100;
                    generationModel.setStandardFrequenzy(newValue);
                    ip.repaint();
                    ip.requestFocus();
                }
             }); 
        return baseFrequencySlider;
    }
    
    private JSlider getTempKSlider(){
        int min = 0;
        int max = 100;
        int init = 10;
        JSlider kSlider = new JSlider(JSlider.HORIZONTAL,min,max,init);

        kSlider.setMajorTickSpacing(25);
        kSlider.setMinorTickSpacing(10);
        kSlider.setPaintTicks(true);
        kSlider.setPaintLabels(true);

        kSlider.setBorder(BorderFactory.createTitledBorder("temperature koefficient value"));

        kSlider.addChangeListener(
            new ChangeListener(){

                @Override
                public void stateChanged(ChangeEvent e){

                    JSlider source = (JSlider)e.getSource();
                   
                    int value = (int)source.getValue();

                    double newKValue = (double) value / (max - min)*10;
                    generationModel.setTemperatureK(newKValue);
                    ip.repaint();
                    ip.requestFocus();
                }
             }); 
        return kSlider;
    }

    private JSlider getMoistKSlider(){
        int min = 0;
        int max = 100;
        int init = 10;
        JSlider kSlider = new JSlider(JSlider.HORIZONTAL,min,max,init);

        kSlider.setMajorTickSpacing(25);
        kSlider.setMinorTickSpacing(10);
        kSlider.setPaintTicks(true);
        kSlider.setPaintLabels(true);

        kSlider.setBorder(BorderFactory.createTitledBorder("moist koefficient value"));

        kSlider.addChangeListener(
            new ChangeListener(){

                @Override
                public void stateChanged(ChangeEvent e){

                    JSlider source = (JSlider)e.getSource();
                   
                    int value = (int)source.getValue();

                    double newKValue = (double) value / (max - min)*10;
                    generationModel.setMoistK(newKValue);
                    ip.repaint();
                    ip.requestFocus();
                }
             }); 
        return kSlider;
    }

    private JSlider getKSlider(){
        int min = 0;
        int max = 500;
        int init = 10;
        JSlider kSlider = new JSlider(JSlider.HORIZONTAL,min,max,init);

        kSlider.setMajorTickSpacing(100);
        kSlider.setMinorTickSpacing(50);
        kSlider.setPaintTicks(true);
        kSlider.setPaintLabels(true);

        kSlider.setBorder(BorderFactory.createTitledBorder("hight koefficient value"));

        kSlider.addChangeListener(
            new ChangeListener(){

                @Override
                public void stateChanged(ChangeEvent e){

                    JSlider source = (JSlider)e.getSource();
                   
                    int value = (int)source.getValue();

                    double newKValue = (double) value / (max - min)*10;
                    generationModel.setK(newKValue);
                    ip.repaint();
                    ip.requestFocus();
                }
             }); 
        return kSlider;
    }

    private JSlider getContinentalSlider(){
        int min = 1;
        int max = 10;
        int init = 5;
        JSlider continentalSlider = new JSlider(JSlider.HORIZONTAL,min,max,init);

        continentalSlider.setMajorTickSpacing(2);
        continentalSlider.setMinorTickSpacing(1);
        continentalSlider.setPaintTicks(true);
        continentalSlider.setPaintLabels(true);

        continentalSlider.setBorder(BorderFactory.createTitledBorder("continental frequency"));

        continentalSlider.addChangeListener(
            new ChangeListener(){

                @Override
                public void stateChanged(ChangeEvent e){

                    JSlider source = (JSlider)e.getSource();
                   
                    int value = (int)source.getValue();

                    double newKValue =  ((double)value / (max - min))/1000;
                    generationModel.setContinentalFrequency(newKValue);

                    


                    ip.repaint();
                    ip.requestFocus();
                }
             }); 
        return continentalSlider;
    }

    private JSlider getPeaksSlider(){
        int min = 1;
        int max = 10;
        int init = 5;
        JSlider peakSlider = new JSlider(JSlider.HORIZONTAL,min,max,init);

        peakSlider.setMajorTickSpacing(2);
        peakSlider.setMinorTickSpacing(1);
        peakSlider.setPaintTicks(true);
        peakSlider.setPaintLabels(true);

        peakSlider.setBorder(BorderFactory.createTitledBorder("peak frequency"));

        peakSlider.addChangeListener(
            new ChangeListener(){

                @Override
                public void stateChanged(ChangeEvent e){

                    JSlider source = (JSlider)e.getSource();
                   
                    int value = (int)source.getValue();

                    double newKValue =  ((double)value / (max - min))/10;
                    generationModel.setPeaksFrequency(newKValue);

                    


                    ip.repaint();
                    ip.requestFocus();
                }
             }); 
        return peakSlider;
    }



    public class NewSeed extends JButton{
        
        public NewSeed (String string){
            super(string);
            addActionListener(new ActionListener(){

                @Override
                public void actionPerformed(ActionEvent e){
                generationModel.newSeed();
                ip.repaint();
                System.out.println("newseed");
                ip.requestFocus();
        }

            });
        }
    }


    public class HightColor extends JButton{
        
        public HightColor (String string){
            super(string);
            addActionListener(new ActionListener(){

                @Override
                public void actionPerformed(ActionEvent e){
                generationModel.setHightColor();
                
                
                
                ip.repaint();
                ip.requestFocus();
        }

            });
        }
    }

    public class ZoomButton extends JButton{
        
        public ZoomButton (String string){
            super(string);
            addActionListener(new ActionListener(){

                @Override
                public void actionPerformed(ActionEvent e){
                //generationModel.setHightColor();
                if (zoom){
                    zoom = false;
                }
                else{
                    zoom = true;
                }
                
                ip.requestFocus();
        }

            });
        }
    }

    public class ImagePanel extends JPanel{

        public ImagePanel(){
            addKeyListener(new ImageFramekeys());
            repaint();
        }

        public void paintComponent(Graphics g){
        super.paintComponents(g); 
            g2 = (Graphics2D)g;
            
            drawImage(g2);
            
        }

        private void drawImage(Graphics2D g2){
            
            g2.drawImage(generationModel.getImage(cameraX,cameraY,cameraX+width,cameraY+height),0,0,null);
        }
    }

    public class PopUpImagePanel extends JPanel{

        public PopUpImagePanel(){
            setSize(200,200);
            repaint();
        }

        public void paintComponent(Graphics g){
        super.paintComponents(g); 
            g2 = (Graphics2D)g;
            System.out.println("repaint popup");
            drawImage(g2);
            
        }

        private void drawImage(Graphics2D g2){
            
            g2.drawImage(generationModel.getImage(cameraX+(int)pointerX,cameraY+(int)pointerY,cameraX+getWidth(),cameraY + getHeight()),0,0,null);
        }
    }

    public static void main(String []args){
        ProceduralGeneration pg = new ProceduralGeneration();
        
        TerrainGenerationView tgv = new TerrainGenerationView(pg);
    }
    
    private void zoom(Point point){
        
        //if (ip.contains(point)){
            popUpmap.setVisible(true);
            System.out.println("mouse");
            popUpmap.repaint();
            popUpimageP.repaint();
            popUpmap.revalidate();
        //} else {
           // popUpmap.setVisible(false);
            
        //}
    }

    public class Mouse implements MouseListener, MouseMotionListener {

        

        @Override
        public void mouseDragged(MouseEvent e) {
            // TODO Auto-generated method stub
            
        }

        @Override

        public void mouseMoved(MouseEvent e) {
            System.out.println("moved");
            pointerX = e.getX();
            pointerY = e.getY();

            zoom(e.getPoint());
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            // TODO Auto-generated method stub
           
        }

        @Override
        public void mousePressed(MouseEvent e) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            // TODO Auto-generated method stub
           
        }

        @Override
        public void mouseExited(MouseEvent e) {
            // TODO Auto-generated method stub
            
        }
    }
}
