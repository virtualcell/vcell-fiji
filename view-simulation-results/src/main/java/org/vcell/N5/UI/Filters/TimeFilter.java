package org.vcell.N5.UI.Filters;

import org.vcell.N5.N5ImageHandler;
import org.vcell.N5.UI.MainPanel;
import org.vcell.N5.UI.N5ExportTable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDateTime;
import java.util.Enumeration;


public class TimeFilter extends JPanel implements ActionListener {
    private final JCheckBox todayInterval;
    private final JCheckBox monthInterval;
    private final JCheckBox yearlyInterval;
    private final JCheckBox anyInterval;
    private final JPanel timeFilter;

    private final N5ExportTable n5ExportTable;


    public TimeFilter(){
        n5ExportTable = MainPanel.n5ExportTable;

        todayInterval = new JCheckBox("Past 24 Hours");
        monthInterval = new JCheckBox("Past Month");
        yearlyInterval = new JCheckBox("Past Year");
        anyInterval = new JCheckBox("Any Time");
        anyInterval.setSelected(true);

        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(todayInterval);
        buttonGroup.add(monthInterval);
        buttonGroup.add(yearlyInterval);
        buttonGroup.add(anyInterval);

        this.setLayout(new BorderLayout());
        timeFilter = new JPanel(new GridBagLayout());
        timeFilter.add(anyInterval);
        timeFilter.add(todayInterval);
        timeFilter.add(monthInterval);
        timeFilter.add(yearlyInterval);
//        timeFilter.setBorder(BorderFactory.createTitledBorder(lowerEtchedBorder, " Time "));
        this.add(timeFilter, BorderLayout.NORTH);
        Border lowerEtchedBorder = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
        this.setBorder(BorderFactory.createTitledBorder(lowerEtchedBorder, " Filters "));

        Enumeration<AbstractButton> b = buttonGroup.getElements();
        while (b.hasMoreElements()){
            b.nextElement().addActionListener(this);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(anyInterval) || e.getSource().equals(todayInterval)
                || e.getSource().equals(monthInterval) || e.getSource().equals(yearlyInterval)) {
            if(MainPanel.controlButtonsPanel.includeExampleExports.isSelected()){
                n5ExportTable.updateExampleExportsToTable();
                return;
            }
            n5ExportTable.updateTableData();
        }
    }

    public LocalDateTime oldestTimeAllowed(){
        LocalDateTime pastTime = LocalDateTime.now();
        if (todayInterval.isSelected()){
            pastTime = pastTime.minusDays(1);
        } else if (monthInterval.isSelected()) {
            pastTime = pastTime.minusMonths(1);
        } else if (yearlyInterval.isSelected()) {
            pastTime = pastTime.minusYears(1);
        } else {
            pastTime = pastTime.minusYears(10); //Max date back is 10 years
        }
        return pastTime;
    }
}
