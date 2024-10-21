package org.vcell.N5.UI;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

public class AdvancedFeatures extends JPanel {
    public final JCheckBox inMemory;
    private JCheckBox dataReduction;
    private JCheckBox rangeSelection;


    public AdvancedFeatures(){
        Border lowerEtchedBorder = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);

        inMemory = new JCheckBox("Open In Memory");
        dataReduction = new JCheckBox("Data Reduction");
        rangeSelection = new JCheckBox("Select Range");

        add(inMemory);
        add(dataReduction);
        add(rangeSelection);
        this.setBorder(BorderFactory.createTitledBorder(lowerEtchedBorder, " Advanced Features "));
    }

}
