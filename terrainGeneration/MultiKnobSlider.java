package terrainGeneration;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class MultiKnobSlider extends JPanel {
    private List<JSlider> sliders;
    private int numKnobs;

    public MultiKnobSlider(int numKnobs, int min, int max) {
        this.numKnobs = numKnobs;
        sliders = new ArrayList<>();
        
        setLayout(new GridLayout(1, numKnobs));

        for (int i = 0; i < numKnobs; i++) {
            JSlider slider = new JSlider(JSlider.HORIZONTAL, min, max, min);
            slider.setPaintTicks(true);
            slider.setMajorTickSpacing((max - min) / 10);
            slider.setMinorTickSpacing((max - min) / 20);
            slider.addChangeListener(new SliderChangeListener(i));
            sliders.add(slider);
            add(slider);
        }
    }

    private class SliderChangeListener implements ChangeListener {
        private int sliderIndex;

        public SliderChangeListener(int sliderIndex) {
            this.sliderIndex = sliderIndex;
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            JSlider source = (JSlider) e.getSource();

            if (!source.getValueIsAdjusting()) {
                int value = source.getValue();

                // Ensure this slider does not surpass the next slider
                if (sliderIndex < numKnobs - 1) {
                    JSlider nextSlider = sliders.get(sliderIndex + 1);
                    if (value >= nextSlider.getValue()) {
                        source.setValue(nextSlider.getValue() - 1);
                    }
                }

                // Ensure this slider does not get surpassed by the previous slider
                if (sliderIndex > 0) {
                    JSlider prevSlider = sliders.get(sliderIndex - 1);
                    if (value <= prevSlider.getValue()) {
                        source.setValue(prevSlider.getValue() + 1);
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Multi-Knob Slider");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());
            
            MultiKnobSlider slider = new MultiKnobSlider(3, 0, 100);
            frame.add(slider);
            
            frame.pack();
            frame.setVisible(true);
        });
    }
}
