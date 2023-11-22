package org.vcell.vcellfiji.UI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

public class N5ViewerGUI extends JFrame {
    public JButton localFiles;
    public JPanel mainPanel;
    private JFrame thisJFrame;
    public JFileChooser localFileDialog;
    private JToolBar menuBar;
    public JList<String> datasetList;
    private JScrollPane resultsScrollPane;
    public JButton remoteFiles;
    public JButton okayButton;
    public JCheckBox openMemoryCheckBox;
    private JPanel datasetListPanel;
    private JLabel datasetLabel;
    private JLabel openInMemory;

    public RemoteFileSelection remoteFileSelection;

    public N5ViewerGUI() {
        thisJFrame = this;
        localFileDialog = new JFileChooser();
        mainPanel = new JPanel();
        datasetListPanel = new JPanel();
        mainPanel.setLayout(new GridBagLayout());
        datasetListPanel.setLayout(new GridBagLayout());
        datasetList = new JList<>();
        datasetList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        GridBagConstraints mainPanelConstraints = new GridBagConstraints();
        mainPanelConstraints.gridx = 0;
        mainPanelConstraints.gridy = 0;
        localFiles = new JButton();
        localFiles.setText("Local Files");
        mainPanel.add(localFiles, mainPanelConstraints);

        mainPanelConstraints.gridy = 0;
        mainPanelConstraints.gridx = 1;
        remoteFiles = new JButton();
        remoteFiles.setText("Remote Files");
        mainPanel.add(remoteFiles, mainPanelConstraints);

        mainPanelConstraints.gridy = 0;
        mainPanelConstraints.gridx = 3;
        openInMemory = new JLabel();
        openInMemory.setText("Open Image in Memory");
        mainPanel.add(openInMemory, mainPanelConstraints);

        mainPanelConstraints.gridy = 0;
        mainPanelConstraints.gridx = 4;
        openMemoryCheckBox = new JCheckBox();
        mainPanel.add(openMemoryCheckBox, mainPanelConstraints);


        GridBagConstraints datasetConstraints = new GridBagConstraints();
        datasetConstraints.gridx = 0;
        datasetConstraints.gridy = 0;
        datasetLabel = new JLabel();
        datasetLabel.setText("Dataset List");
        datasetListPanel.add(datasetLabel, datasetConstraints);

        datasetConstraints.gridx = 0;
        datasetConstraints.gridy = 1;
        datasetConstraints.gridwidth = 1;
        datasetConstraints.ipady = 70;
        datasetConstraints.ipadx = 100;
        resultsScrollPane = new JScrollPane(datasetList);
        datasetListPanel.add(resultsScrollPane, datasetConstraints);

        mainPanelConstraints.gridwidth = 4;
        mainPanelConstraints.gridy = 1;
        mainPanelConstraints.ipady = 40;
        mainPanelConstraints.gridx = 0;
        mainPanelConstraints.fill = GridBagConstraints.BOTH;
        mainPanelConstraints.anchor = GridBagConstraints.CENTER;
        mainPanel.add(datasetListPanel, mainPanelConstraints);


        mainPanelConstraints = new GridBagConstraints();
        mainPanelConstraints.gridy = 2;
        mainPanelConstraints.gridx = 0;
        mainPanelConstraints.gridwidth = 4;
        okayButton = new JButton();
        okayButton.setText("Open Dataset");
        mainPanel.add(okayButton, mainPanelConstraints);


        localFiles.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                localFileDialog.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
                localFileDialog.showOpenDialog(thisJFrame);
//                System.out.print(localFileDialog.getSelectedFile());
            }
        });

        remoteFiles.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                remoteFileSelection.setVisible(true);
            }
        });

        // listener for if credentials or endpoint is used

        this.setTitle("VCell Manager");
        this.setContentPane(this.mainPanel);
        this.setSize(500, 400);
        this.setVisible(true);
        this.remoteFileSelection = new RemoteFileSelection();


    }

    public void updateDatasetList(ArrayList<String> arrayList){
        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (String s: arrayList){
            listModel.addElement(s);
        }
        this.datasetList.setModel(listModel);
        this.datasetList.updateUI();
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
    }
}

