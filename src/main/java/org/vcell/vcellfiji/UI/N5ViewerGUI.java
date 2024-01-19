package org.vcell.vcellfiji.UI;

import org.vcell.vcellfiji.N5ImageHandler;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
    private JDialog exportTableDialog;
    private JPanel exportTablePanel;
    private DefaultTableModel exportTableModel;

    public JButton mostRecentExport;

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
        openInMemory.setText("Open Image in Memory");
        mainPanel.add(openInMemory, mainPanelConstraints);

        mainPanelConstraints.gridy = 0;
        mainPanelConstraints.gridx = 5;
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

        remoteFiles.addActionListener(this);
        exportTableButton.addActionListener(this);

        // listener for if credentials or endpoint is used

        this.setTitle("VCell Manager");
        this.setContentPane(this.mainPanel);
        this.setSize(600, 400);
        this.setVisible(true);
        this.remoteFileSelection = new RemoteFileSelection(thisJFrame);


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
            displayExportTable();
        }
    }

    public void refreshDataList(){

    }

    public void updateTableModel(){
        try{
            HashMap<String, Object> jsonData = N5ImageHandler.getJsonData();
            if (jsonData != null){
                List<String> set = (ArrayList<String>) jsonData.get("jobIDs");
                String lastElement = exportTableModel.getRowCount() == 0 ? null: (String) exportTableModel.getValueAt(0,0);
                for(int i = set.size() - 1; i > -1; i--){
                    if(lastElement != null && lastElement.equals(set.get(i))){
                        break;
                    }
                    addRowFromJson(jsonData, set.get(i));
                }
            }
            exportTableModel.fireTableDataChanged();
        }
        catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    public void initalizeTableData(){
        HashMap<String, Object> jsonData = null;
        try {
            jsonData = N5ImageHandler.getJsonData();
            if (jsonData != null){
                List<String> set = (ArrayList<String>) jsonData.get("jobIDs");
                for (String s : set) {
                    addRowFromJson(jsonData, s);
                }
            }
            exportTableModel.fireTableDataChanged();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void addRowFromJson(HashMap<String, Object> jsonData, String s){
        exportTableModel.addRow(new Object[]{""});
    }

    public void displayExportTable(){
        if (exportTableDialog == null){
            exportTablePanel = new JPanel();

            DefaultTableModel defaultTableModel = new DefaultTableModel();
            defaultTableModel.addColumn("Test");
            defaultTableModel.addColumn("Test2");
            Object[] testRow = {"k", "b"};
            defaultTableModel.addRow(testRow);

            JTable jTable = new JTable(defaultTableModel);
            JScrollPane jScrollPane = new JScrollPane(jTable);

            jScrollPane.setSize(500, 400);
            jScrollPane.setPreferredSize(new Dimension(500, 400));
            jScrollPane.setMinimumSize(new Dimension(500, 400));

            JLabel jLabel = new JLabel("Recent Exports. List is volatile save important export metadata elsewhere.");
            JButton refresh = new JButton("Refresh List");
            JButton open = new JButton("Open");

            JPanel topBar = new JPanel();
            topBar.setLayout(new FlowLayout());
            refresh.addActionListener(this);
            open.addActionListener(this);
            topBar.add(jLabel);
            topBar.add(open);
            topBar.add(refresh);

            exportTablePanel.setLayout(new BorderLayout());
            exportTablePanel.add(topBar, BorderLayout.NORTH);
            exportTablePanel.add(jScrollPane);

            exportTablePanel.setPreferredSize(new Dimension(800, 500));
            JOptionPane pane = new JOptionPane(exportTablePanel, JOptionPane.PLAIN_MESSAGE, 0, null, new Object[] {"Close"});
            exportTableDialog = pane.createDialog("VCell Exports");
            exportTableDialog.setModal(false);
            exportTableDialog.setResizable(true);
            exportTableDialog.setVisible(true);
        }
        else {
            exportTableDialog.setVisible(true);
        }
    }


}

