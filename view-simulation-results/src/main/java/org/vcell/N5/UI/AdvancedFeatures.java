package org.vcell.N5.UI;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import java.awt.*;

public class AdvancedFeatures extends JPanel {
    public final JCheckBox inMemory;
    public final JCheckBox dataReduction;
    public final JButton copyLink;
    public final JButton useN5Link;


    public AdvancedFeatures(){
        Border lowerEtchedBorder = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);


        inMemory = new JCheckBox("Open In Memory");
        dataReduction = new JCheckBox("Run Measurement Script");
        JPanel checkBoxPanel = new JPanel();
        checkBoxPanel.add(dataReduction);

        JPanel buttonPanel = new JPanel();
        copyLink = new JButton("Copy Link");
        useN5Link = new JButton("Use N5 Link");
        buttonPanel.add(copyLink);
        buttonPanel.add(useN5Link);

        setLayout(new BorderLayout());
        add(buttonPanel, BorderLayout.NORTH);
        add(checkBoxPanel, BorderLayout.SOUTH);
        this.setBorder(BorderFactory.createTitledBorder(lowerEtchedBorder, " Advanced Features "));
    }

}
