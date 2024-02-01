package org.vcell.N5.UI;

import org.vcell.N5.N5ImageHandler;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

public class N5ViewerGUI extends JFrame implements ActionListener {
    public JButton localFiles;
    public JPanel mainPanel;
    private JFrame thisJFrame;
    public JFileChooser localFileDialog;
    private JToolBar menuBar;
    public JList<String> datasetList;
    private JScrollPane resultsScrollPane;
    public JButton remoteFiles;
    public JButton okayButton;
    public JButton exportTableButton;
    public JCheckBox openMemoryCheckBox;
    private JPanel datasetListPanel;
    private JLabel datasetLabel;
    private JLabel openInMemory;
    public int jFileChooserResult;
    private N5ExportTable n5ExportTable;

    public JButton mostRecentExport;
    private JButton questionMark;

    public RemoteFileSelection remoteFileSelection;
    private HelpExplanation helpExplanation;

    public N5ViewerGUI(N5ImageHandler n5ImageHandler) {
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
//        mainPanel.add(localFiles, mainPanelConstraints);

        mainPanelConstraints.gridy = 0;
        mainPanelConstraints.gridx = 1;
        remoteFiles = new JButton();
        remoteFiles.setText("Remote Files");
        mainPanel.add(remoteFiles, mainPanelConstraints);

        mainPanelConstraints.gridy = 0;
        mainPanelConstraints.gridx = 2;
        mostRecentExport = new JButton();
        mostRecentExport.setText("Recent Export");
        mainPanel.add(mostRecentExport, mainPanelConstraints);

        mainPanelConstraints.gridy = 0;
        mainPanelConstraints.gridx = 3;
        exportTableButton = new JButton();
        exportTableButton.setText("Export Table");
        mainPanel.add(exportTableButton, mainPanelConstraints);

        mainPanelConstraints.gridy = 0;
        mainPanelConstraints.gridx = 4;
        openInMemory = new JLabel();
        openInMemory.setText("Open in Memory");
        mainPanel.add(openInMemory, mainPanelConstraints);

        mainPanelConstraints.gridy = 0;
        mainPanelConstraints.gridx = 5;
        openMemoryCheckBox = new JCheckBox();
        mainPanel.add(openMemoryCheckBox, mainPanelConstraints);

        mainPanelConstraints.gridy = 0;
        mainPanelConstraints.gridx = 6;
        questionMark = new JButton("?");
        mainPanel.add(questionMark, mainPanelConstraints);


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

        mainPanelConstraints.gridwidth = 5;
        mainPanelConstraints.gridy = 1;
        mainPanelConstraints.ipady = 40;
        mainPanelConstraints.gridx = 0;
        mainPanelConstraints.fill = GridBagConstraints.BOTH;
        mainPanelConstraints.anchor = GridBagConstraints.CENTER;
        mainPanel.add(datasetListPanel, mainPanelConstraints);


        mainPanelConstraints = new GridBagConstraints();
        mainPanelConstraints.gridy = 2;
        mainPanelConstraints.gridx = 0;
        mainPanelConstraints.gridwidth = 5;
        okayButton = new JButton();
        okayButton.setText("Open Dataset");
        mainPanel.add(okayButton, mainPanelConstraints);


        localFiles.addActionListener(this);
        questionMark.addActionListener(this);
        remoteFiles.addActionListener(this);
        exportTableButton.addActionListener(this);

        // listener for if credentials or endpoint is used

        this.setTitle("VCell Manager");
        this.setContentPane(this.mainPanel);
        this.setSize(600, 400);
        this.setVisible(true);
        this.remoteFileSelection = new RemoteFileSelection(thisJFrame);


        n5ExportTable = new N5ExportTable(n5ImageHandler);
        helpExplanation = new HelpExplanation();
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

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == localFiles){
            localFileDialog.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            jFileChooserResult = localFileDialog.showOpenDialog(thisJFrame);
        } else if (e.getSource() == remoteFiles) {
            remoteFileSelection.setVisible(true);
        } else if (e.getSource() == exportTableButton) {
            n5ExportTable.displayExportTable();
        } else if (e.getSource() == questionMark) {
            helpExplanation.displayHelpMenu();
        }
    }

}

