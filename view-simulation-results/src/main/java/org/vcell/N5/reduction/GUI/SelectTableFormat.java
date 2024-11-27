package org.vcell.N5.reduction.GUI;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.*;

public class SelectTableFormat extends JPanel {
    private final JRadioButton wideTable = new JRadioButton("Wide (Best for manual input)");

    public SelectTableFormat(){
        this.setLayout(new GridLayout(1,1));
        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(wideTable);
        JRadioButton tallTable = new JRadioButton("Tall (Best for scripts)");
        buttonGroup.add(tallTable);
        this.add(wideTable);
        this.add(tallTable);
        this.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Select CSV Table Format:"));
        wideTable.setSelected(true);
        this.setVisible(false);
    }


    public boolean isWideTableSelected(){
        return wideTable.isSelected();
    }


}
