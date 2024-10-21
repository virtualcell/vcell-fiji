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

    private static JButton openOrCancel;
//    private final JButton openLocal = new JButton("Open N5 Local");
    private final JButton copyLink;
    private final JButton useN5Link;
    private final JButton questionMark;

    public final JCheckBox includeExampleExports;
    public final JCheckBox displayAdvancedFeatures;

    private N5ExportTable n5ExportTable;
    private RemoteFileSelection remoteFileSelection;
    private final AdvancedFeatures advancedFeatures = new AdvancedFeatures();

    public ControlButtonsPanel(){
        includeExampleExports = new JCheckBox("Show Example Exports");
        includeExampleExports.setSelected(!N5ImageHandler.exportedDataExists());

        displayAdvancedFeatures = new JCheckBox("Advanced Features");

        openOrCancel = new JButton("Open");
        copyLink = new JButton("Copy Link");
        useN5Link = new JButton("Use N5 Link");
        questionMark = new JButton("?");
        questionMark.setPreferredSize(new Dimension(20, 20));

        GridBagConstraints gridBagConstraints = new GridBagConstraints();

        JPanel topRow = new JPanel(new GridBagLayout());
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        topRow.add(openOrCancel, gridBagConstraints);

        gridBagConstraints.gridx = 1;
        topRow.add(copyLink, gridBagConstraints);

        gridBagConstraints.gridx = 2;
        topRow.add(useN5Link, gridBagConstraints);

        gridBagConstraints.gridx = 3;
        topRow.add(questionMark);

        JPanel bottomRow = new JPanel(new GridBagLayout());
        bottomRow.add(includeExampleExports);
        gridBagConstraints.gridx = 1;
        bottomRow.add(displayAdvancedFeatures, gridBagConstraints);


        JPanel userButtonsPanel = new JPanel(new GridBagLayout());
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        userButtonsPanel.add(topRow, gridBagConstraints);
        gridBagConstraints.gridy = 1;
        userButtonsPanel.add(bottomRow, gridBagConstraints);

//        buttonsPanel.add(questionMark);





        int paneWidth = 800;
        this.setPreferredSize(new Dimension(paneWidth, 80));
        this.setLayout(new BorderLayout());
//        topBar.add(openLocal);
        Border lowerEtchedBorder = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
        this.add(userButtonsPanel, BorderLayout.EAST);
        this.setBorder(BorderFactory.createTitledBorder(lowerEtchedBorder, " User Options "));

        advancedFeatures.setVisible(false);
        add(advancedFeatures, BorderLayout.WEST);


        openOrCancel.addActionListener(this);
        copyLink.addActionListener(this);
        questionMark.addActionListener(this);
        useN5Link.addActionListener(this);
//        openLocal.addActionListener(this);
        includeExampleExports.addActionListener(this);
        displayAdvancedFeatures.addActionListener(this);


        openOrCancel.setEnabled(false);
        copyLink.setEnabled(false);
    }

    public void initialize(N5ExportTable n5ExportTable, RemoteFileSelection remoteFileSelection){
        this.n5ExportTable = n5ExportTable;
        this.remoteFileSelection = remoteFileSelection;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if(e.getSource().equals(openOrCancel)){
            if (openOrCancel.getText().equals("Cancel")){
                n5ExportTable.removeFromLoadingRows();
            } else {
                n5ExportTable.openSelectedRows(advancedFeatures.inMemory.isSelected());
            }
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
        } else if (e.getSource().equals(displayAdvancedFeatures)) {
            advancedFeatures.setVisible(displayAdvancedFeatures.isSelected());
        }
    }

    public void allowCancel(boolean allow){
        openOrCancel.setEnabled(true);
        copyLink.setEnabled(true);
        useN5Link.setEnabled(true);
        remoteFileSelection.submitS3Info.setEnabled(true);
        if (allow){
            openOrCancel.setText("Cancel");
        } else {
            openOrCancel.setText("Open");
        }
    }

    public void enableRowContextDependentButtons(boolean enable){
        openOrCancel.setEnabled(enable);
        copyLink.setEnabled(enable);
    }

    public void enableCriticalButtons(boolean enable){
        useN5Link.setEnabled(enable);
        openOrCancel.setEnabled(enable);
        copyLink.setEnabled(enable);
        remoteFileSelection.submitS3Info.setEnabled(enable);
    }
}
