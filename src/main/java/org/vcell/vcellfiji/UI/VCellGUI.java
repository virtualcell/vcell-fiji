package org.vcell.vcellfiji.UI;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

public class VCellGUI extends JFrame {
    public JButton LocalFiles;
    public JPanel mainPanel;
    private final JFrame jFrame;
    public JFileChooser localFileDialog;
    private JToolBar menuBar;
    public JList<String> datasetList;
    private JScrollPane resultsScrollPane;
    public JButton remoteFiles;
    public JButton okayButton;
    public JCheckBox openVirtualCheckBox;

    public RemoteFileSelection remoteFileSelection;

    public VCellGUI() {
        jFrame = this;
        localFileDialog = new JFileChooser();


        this.setTitle("VCell Manager");
        this.setContentPane(this.mainPanel);
        this.setSize(500, 400);
        this.setVisible(true);
        this.remoteFileSelection = new RemoteFileSelection();


        LocalFiles.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                localFileDialog.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
                localFileDialog.showOpenDialog(jFrame);
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
