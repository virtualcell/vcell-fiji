package org.vcell.N5.UI;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import java.awt.*;

public class AdvancedFeatures extends JPanel {
    public final JButton openInMemory;
    public final JButton copyLink;
    public final JButton useN5Link;


    public AdvancedFeatures(){
        Border lowerEtchedBorder = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);


        openInMemory = new JButton("Open In Memory");
        JPanel checkBoxPanel = new JPanel();
//        checkBoxPanel.add(openInMemory);

        JPanel buttonPanel = new JPanel();
        copyLink = new JButton("Copy Link");
        useN5Link = new JButton("Use N5 Link");
        buttonPanel.add(copyLink);
        buttonPanel.add(useN5Link);
        buttonPanel.add(openInMemory);

//        setLayout(new BorderLayout());
//        add(buttonPanel, BorderLayout.NORTH);
//        add(checkBoxPanel, BorderLayout.SOUTH);
        add(buttonPanel);
        this.setBorder(BorderFactory.createTitledBorder(lowerEtchedBorder, " Advanced Features "));
    }
}
