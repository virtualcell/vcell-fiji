package org.vcell.N5.analysis;

import javax.swing.*;
import java.awt.*;

class SelectMeasurements extends JPanel{
    private final JComboBox<String> chosenMeasurement;

    public SelectMeasurements(){
        setLayout(new GridLayout(1, 2));
        this.add(new JLabel("Measurement Type"));
        chosenMeasurement = new JComboBox<>(new String[]{AvailableMeasurements.AVERAGE.publicName});
        this.add(chosenMeasurement);
    }

    enum AvailableMeasurements{
        AVERAGE("Average");

        public final String publicName;
        AvailableMeasurements(String publicName){
            this.publicName = publicName;
        }
    }
}
