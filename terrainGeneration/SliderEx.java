package terrainGeneration;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class SliderEx extends JSlider {

    private List<Integer> thumbPositions;

    public SliderEx(int min, int max, int initialThumbCount) {
        super(min, max);
        thumbPositions = new ArrayList<>();

        // Initialize the thumb positions
        for (int i = 0; i < initialThumbCount; i++) {
            int position = min + i * (max - min) / (initialThumbCount - 1);
            thumbPositions.add(position);
        }

        setPaintTicks(true);
        setPaintLabels(true);
        setSnapToTicks(true);

        addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                repaint();
            }
        });
    }

    public List<Integer> getThumbPositions() {
        return thumbPositions;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        for (int position : thumbPositions) {
            int thumbX = getXForValue(position);
            g.setColor(Color.RED);
            g.fillRect(thumbX - 5, 0, 10, getHeight());
        }
    }

    private int getXForValue(int value) {
        int min = getMinimum();
        int max = getMaximum();
        int trackLength = getWidth() - getInsets().left - getInsets().right;
        double valueRatio = (double)(value - min) / (max - min);
        int thumbX = (int)(getInsets().left + valueRatio * trackLength);
        return thumbX;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JFrame frame = new JFrame("MultiThumbSlider Example");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setSize(400, 100);

                SliderEx slider = new SliderEx(0, 100, 3);
                frame.add(slider);

                frame.setVisible(true);
            }
        });
    }
}
