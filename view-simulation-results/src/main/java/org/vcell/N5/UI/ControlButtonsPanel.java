package org.vcell.N5.UI;

import org.vcell.N5.N5ImageHandler;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ControlButtonsPanel extends JPanel implements ActionListener {

    private static JButton openOrCancel;
    private JButton openInMemory;
//    private final JButton openLocal = new JButton("Open N5 Local");
    private final JButton questionMark;

    public final JCheckBox includeExampleExports;
    public final JCheckBox displayAdvancedFeatures;

    private N5ExportTable n5ExportTable;
    private RemoteFileSelection remoteFileSelection;
    public final AdvancedFeatures advancedFeatures = new AdvancedFeatures();

    public ControlButtonsPanel(){
        includeExampleExports = new JCheckBox("Show Example Exports");
        includeExampleExports.setSelected(!N5ImageHandler.exportedDataExists());

        displayAdvancedFeatures = new JCheckBox("Advanced Features");

        openOrCancel = new JButton("Open Virtually");
        openInMemory = new JButton("Open In Memory");
        questionMark = new JButton("?");
        questionMark.setPreferredSize(new Dimension(20, 20));

        GridBagConstraints gridBagConstraints = new GridBagConstraints();

        JPanel topRow = new JPanel(new GridBagLayout());
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        topRow.add(openOrCancel, gridBagConstraints);

        gridBagConstraints.gridx = 1;
        topRow.add(openInMemory, gridBagConstraints);

        gridBagConstraints.gridx = 2;
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
        this.setPreferredSize(new Dimension(paneWidth, 110));
        this.setLayout(new BorderLayout());
//        topBar.add(openLocal);
        Border lowerEtchedBorder = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
        this.add(userButtonsPanel, BorderLayout.EAST);
        this.setBorder(BorderFactory.createTitledBorder(lowerEtchedBorder, " User Options "));

        advancedFeatures.setVisible(false);
        add(advancedFeatures, BorderLayout.WEST);


        openOrCancel.addActionListener(this);
        advancedFeatures.copyLink.addActionListener(this);
        questionMark.addActionListener(this);
        advancedFeatures.useN5Link.addActionListener(this);
//        openLocal.addActionListener(this);
        includeExampleExports.addActionListener(this);
        displayAdvancedFeatures.addActionListener(this);
        openInMemory.addActionListener(this);


        openOrCancel.setEnabled(false);
        openInMemory.setEnabled(false);
        advancedFeatures.copyLink.setEnabled(false);
    }

    public void initialize(N5ExportTable n5ExportTable, RemoteFileSelection remoteFileSelection){
        this.n5ExportTable = n5ExportTable;
        this.remoteFileSelection = remoteFileSelection;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if(e.getSource().equals(openOrCancel) || e.getSource().equals(openInMemory)){
            if (openOrCancel.getText().equals("Cancel")){
                n5ExportTable.removeFromLoadingRows();
            } else {
                n5ExportTable.openSelectedRows(e.getSource().equals(openInMemory));
            }
        } else if (e.getSource().equals(advancedFeatures.copyLink)) {
            n5ExportTable.copySelectedRowLink();
        } else if (e.getSource().equals(questionMark)) {
            new HelpExplanation().displayHelpMenu();
        } else if (e.getSource().equals(advancedFeatures.useN5Link)) {
            remoteFileSelection.setVisible(true);
        } else if (e.getSource().equals(includeExampleExports)){
            n5ExportTable.updateTableData();
        } else if (e.getSource().equals(displayAdvancedFeatures)) {
            advancedFeatures.setVisible(displayAdvancedFeatures.isSelected());
        }
    }

    public void allowCancel(boolean allow){
        openOrCancel.setEnabled(true);
        advancedFeatures.copyLink.setEnabled(true);
        advancedFeatures.useN5Link.setEnabled(true);
        remoteFileSelection.submitS3Info.setEnabled(true);
        openInMemory.setEnabled(!allow);
        if (allow){
            openOrCancel.setText("Cancel");
        } else {
            openOrCancel.setText("Open Virtually");
        }
    }

    public void enableRowContextDependentButtons(boolean enable){
        openOrCancel.setEnabled(enable);
        advancedFeatures.copyLink.setEnabled(enable);
        openInMemory.setEnabled(enable);
    }

    public void enableCriticalButtons(boolean enable){
        advancedFeatures.useN5Link.setEnabled(enable);
        openOrCancel.setEnabled(enable);
        advancedFeatures.copyLink.setEnabled(enable);
        remoteFileSelection.submitS3Info.setEnabled(enable);
        openInMemory.setEnabled(enable);
    }
}
