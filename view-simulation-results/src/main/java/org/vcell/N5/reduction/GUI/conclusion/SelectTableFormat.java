package org.vcell.N5.reduction.GUI.conclusion;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EtchedBorder;
import java.awt.*;

public class SelectTableFormat extends JPanel {
    private final JRadioButton wideTable = new JRadioButton("Wide (Best for manual analysis)");

    public SelectTableFormat(){
        this.setLayout(new GridLayout(1,1));
        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(wideTable);
        JRadioButton tallTable = new JRadioButton("Tall (Best for scripts)");
        buttonGroup.add(tallTable);
        this.add(wideTable);
        this.add(tallTable);
        Border border = new CompoundBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5),
                BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "4. Select CSV Table Format:"));
        this.setBorder(border);
        wideTable.setSelected(true);
    }


    public boolean isWideTableSelected(){
        return wideTable.isSelected();
    }


}
