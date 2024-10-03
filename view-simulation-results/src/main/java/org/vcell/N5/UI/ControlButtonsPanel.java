package org.vcell.N5.UI;

import org.vcell.N5.N5ImageHandler;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDateTime;
import java.util.Enumeration;

public class ControlButtonsPanel extends JPanel implements ActionListener {

    private static JButton open;
//    private final JButton openLocal = new JButton("Open N5 Local");
    private final JButton copyLink;
    private final JButton refreshButton;
    private final JButton useN5Link;
    private final JButton questionMark;
    private final JButton openInMemory;
    private final JCheckBox includeExampleExports;
    private final JCheckBox todayInterval;
    private final JCheckBox monthInterval;
    private final JCheckBox yearlyInterval;
    private final JCheckBox anyInterval;
    private final JPanel timeFilter;

    private N5ExportTable n5ExportTable;
    private RemoteFileSelection remoteFileSelection;

    public ControlButtonsPanel(){
        refreshButton = new JButton("Refresh");
        open = new JButton("Open");
        copyLink = new JButton("Copy Link");
        useN5Link = new JButton("Use N5 Link");
        questionMark = new JButton("?");
        questionMark.setPreferredSize(new Dimension(20, 20));
        openInMemory = new JButton("Open In Memory");
        openInMemory.setSelected(false);
        includeExampleExports = new JCheckBox("Show Example Exports");
        includeExampleExports.setSelected(!N5ImageHandler.exportedDataExists());

        GridBagConstraints gridBagConstraints = new GridBagConstraints();

        JPanel topRow = new JPanel(new GridBagLayout());
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        topRow.add(open, gridBagConstraints);
        gridBagConstraints.gridwidth = 1;

        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        topRow.add(openInMemory, gridBagConstraints);
        gridBagConstraints.gridx = 2;

        JPanel bottomRow = new JPanel(new GridBagLayout());
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        bottomRow.add(copyLink, gridBagConstraints);
        gridBagConstraints.gridx = 1;
        bottomRow.add(useN5Link, gridBagConstraints);
        bottomRow.add(questionMark);


        JPanel userButtonsPanel = new JPanel(new GridBagLayout());
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        userButtonsPanel.add(topRow, gridBagConstraints);
        gridBagConstraints.gridy = 1;
        userButtonsPanel.add(bottomRow, gridBagConstraints);

//        buttonsPanel.add(questionMark);


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

        JPanel filters = new JPanel();
        filters.setLayout(new BorderLayout());
        timeFilter = new JPanel(new GridBagLayout());
        timeFilter.add(anyInterval);
        timeFilter.add(todayInterval);
        timeFilter.add(monthInterval);
        timeFilter.add(yearlyInterval);
//        timeFilter.setBorder(BorderFactory.createTitledBorder(lowerEtchedBorder, " Time "));
        filters.add(timeFilter, BorderLayout.NORTH);
        filters.add(includeExampleExports, BorderLayout.SOUTH);
        Border lowerEtchedBorder = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
        filters.setBorder(BorderFactory.createTitledBorder(lowerEtchedBorder, " Filters "));


        int paneWidth = 800;
        this.setPreferredSize(new Dimension(paneWidth, 100));
        this.setLayout(new BorderLayout());
//        topBar.add(openLocal);
        this.add(userButtonsPanel, BorderLayout.EAST);
        this.add(filters, BorderLayout.WEST);
        this.setBorder(BorderFactory.createTitledBorder(lowerEtchedBorder, " User Options "));


        refreshButton.addActionListener(this);
        open.addActionListener(this);
        copyLink.addActionListener(this);
        questionMark.addActionListener(this);
        useN5Link.addActionListener(this);
        includeExampleExports.addActionListener(this);
//        openLocal.addActionListener(this);
        openInMemory.addActionListener(this);

        Enumeration<AbstractButton> b = buttonGroup.getElements();
        while (b.hasMoreElements()){
            b.nextElement().addActionListener(this);
        }

        open.setEnabled(false);
        copyLink.setEnabled(false);
        openInMemory.setEnabled(false);
    }

    public void initialize(N5ExportTable n5ExportTable, RemoteFileSelection remoteFileSelection){
        this.n5ExportTable = n5ExportTable;
        this.remoteFileSelection = remoteFileSelection;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if(e.getSource().equals(open) || e.getSource().equals(openInMemory)){
            n5ExportTable.openSelectedRows(e.getSource().equals(openInMemory));
        } else if (e.getSource().equals(copyLink)) {
            n5ExportTable.copySelectedRowLink();
        } else if (e.getSource().equals(questionMark)) {
            new HelpExplanation().displayHelpMenu();
        } else if (e.getSource().equals(useN5Link)) {
            remoteFileSelection.setVisible(true);
        } else if (e.getSource().equals(includeExampleExports)){
            if(includeExampleExports.isSelected()){
                n5ExportTable.updateExampleExportsToTable();
                return;
            }
            n5ExportTable.updateTableData();
        } else if (e.getSource().equals(anyInterval) || e.getSource().equals(todayInterval)
                || e.getSource().equals(monthInterval) || e.getSource().equals(yearlyInterval)) {
            if(includeExampleExports.isSelected()){
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

    public void enableRowContextDependentButtons(boolean enable){
        open.setEnabled(enable);
        copyLink.setEnabled(enable);
        openInMemory.setEnabled(enable);
    }

    public void enableCriticalButtons(boolean enable){
        useN5Link.setEnabled(enable);
        open.setEnabled(enable);
        refreshButton.setEnabled(enable);
        copyLink.setEnabled(enable);
        remoteFileSelection.submitS3Info.setEnabled(enable);
        openInMemory.setEnabled(enable);
    }
}
