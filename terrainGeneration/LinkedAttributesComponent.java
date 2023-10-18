package terrainGeneration;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;

public class LinkedAttributesComponent extends JPanel {
    private JSlider slider;
    private JLabel valueLabel;

    public LinkedAttributesComponent(int minValue, int maxValue) {
        setLayout(new FlowLayout());
        
        slider = new JSlider(minValue, maxValue);
        valueLabel = new JLabel("Value: " + slider.getValue());

        // Add a change listener to the slider
        slider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                // When the slider value changes, update the label and trigger the linked attribute change
                int sliderValue = slider.getValue();
                valueLabel.setText("Value: " + sliderValue);
                
                // You can add logic here to update other linked attributes as needed
                // For this example, we're just updating the label.
            }
        });

        add(slider);
        add(valueLabel);
    }

    public int getSliderValue() {
        return slider.getValue();
    }

    public void setSliderValue(int value) {
        slider.setValue(value);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Linked Attributes Example");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(300, 100);
            
            LinkedAttributesComponent component = new LinkedAttributesComponent(0, 100);
            frame.add(component);
            
            frame.setVisible(true);
        });
    }
}
