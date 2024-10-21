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
    private final JButton openInMemory;

    private N5ExportTable n5ExportTable;
    private RemoteFileSelection remoteFileSelection;

    public ControlButtonsPanel(){
        openOrCancel = new JButton("Open");
        copyLink = new JButton("Copy Link");
        useN5Link = new JButton("Use N5 Link");
        questionMark = new JButton("?");
        questionMark.setPreferredSize(new Dimension(20, 20));
        openInMemory = new JButton("Open In Memory");
        openInMemory.setSelected(false);

        GridBagConstraints gridBagConstraints = new GridBagConstraints();

        JPanel topRow = new JPanel(new GridBagLayout());
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        topRow.add(openOrCancel, gridBagConstraints);
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





        int paneWidth = 800;
        this.setPreferredSize(new Dimension(paneWidth, 100));
        this.setLayout(new BorderLayout());
//        topBar.add(openLocal);
        Border lowerEtchedBorder = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
        this.add(userButtonsPanel, BorderLayout.EAST);
        this.setBorder(BorderFactory.createTitledBorder(lowerEtchedBorder, " User Options "));


        openOrCancel.addActionListener(this);
        copyLink.addActionListener(this);
        questionMark.addActionListener(this);
        useN5Link.addActionListener(this);
//        openLocal.addActionListener(this);
        openInMemory.addActionListener(this);



        openOrCancel.setEnabled(false);
        copyLink.setEnabled(false);
        openInMemory.setEnabled(false);
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
        } else if (e.getSource().equals(copyLink)) {
            n5ExportTable.copySelectedRowLink();
        } else if (e.getSource().equals(questionMark)) {
            new HelpExplanation().displayHelpMenu();
        } else if (e.getSource().equals(useN5Link)) {
            remoteFileSelection.setVisible(true);
        }
    }

    public void allowCancel(boolean allow){
        openOrCancel.setEnabled(true);
        copyLink.setEnabled(true);
        openInMemory.setEnabled(!allow);
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
        openInMemory.setEnabled(enable);
    }

    public void enableCriticalButtons(boolean enable){
        useN5Link.setEnabled(enable);
        openOrCancel.setEnabled(enable);
        copyLink.setEnabled(enable);
        remoteFileSelection.submitS3Info.setEnabled(enable);
        openInMemory.setEnabled(enable);
    }
}
