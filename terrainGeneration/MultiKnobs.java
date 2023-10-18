package terrainGeneration;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class MultiKnobs extends JPanel {

    private List<JSlider> sliders;

    public MultiKnobs(int min, int max, int thumbCount) {
        setLayout(new GridLayout(1, thumbCount));
        sliders = new ArrayList<>();

        for (int i = 0; i < thumbCount; i++) {
            JSlider slider = new JSlider(JSlider.VERTICAL, min, max, min);
            slider.setPaintTicks(true);
            slider.setPaintLabels(true);
            slider.setSnapToTicks(true);

            sliders.add(slider);
            add(slider);

            slider.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    repaint();
                }
            });
        }
    }

    public List<JSlider> getSliders() {
        return sliders;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        for (JSlider slider : sliders) {
            int thumbX = getXForValue(slider);
            g.setColor(Color.RED);
            g.fillRect(thumbX - 5, 0, 10, getHeight());
        }
    }

    private int getXForValue(JSlider slider) {
        int min = slider.getMinimum();
        int max = slider.getMaximum();
        int trackLength = slider.getWidth() - slider.getInsets().left - slider.getInsets().right;
        double valueRatio = (double) (slider.getValue() - min) / (max - min);
        int thumbX = (int) (slider.getInsets().left + valueRatio * trackLength);
        return thumbX;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JFrame frame = new JFrame("MultiThumbSlider Example");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setSize(400, 100);

                MultiKnobs multiThumbSlider = new MultiKnobs(0, 100, 3);
                frame.add(multiThumbSlider);

                frame.setVisible(true);
            }
        });
    }
}
