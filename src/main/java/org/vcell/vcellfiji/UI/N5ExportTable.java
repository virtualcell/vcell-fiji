package org.vcell.vcellfiji.UI;

import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.vcell.vcellfiji.ExportDataRepresentation;
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

public class N5ExportTable implements ActionListener {
    private JDialog exportTableDialog;
    private JPanel exportTablePanel;
    private DefaultTableModel exportTableModel;
    @Parameter
    private LogService logService;
    public void updateTableModel(){
        try{
            ExportDataRepresentation jsonData = N5ImageHandler.getJsonData();
//            if (jsonData != null){
//                List<String> globalJobIDs = jsonData.globalJobIDs;
//                String lastElement = tableModel.getRowCount() == 0 ? null: tableModel.tableData.get(tableModel.tableData.size() - 1).jobID;
//                for(int i = globalJobIDs.size() - 1; i > -1; i--){
//                    // first index is JobID, second is data format
//                    String[] tokens = globalJobIDs.get(i).split(",");
//                    if(lastElement != null && lastElement.equals(tokens[0])){
//                        break;
//                    }
//                    addRowFromJson(tokens[0], tokens[1], jsonData);
//                }
//            }
//            tableModel.refreshData();
        }
        catch (Exception e){
            logService.error("Failed Update Export Viewer Table Model:", e);
        }
    }

    public void initalizeTableData(){
//        ExportDataRepresentation jsonData = null;
//        try {
//            jsonData = N5ImageHandler.getJsonData();
//            if (jsonData != null){
//                List<String> set = (ArrayList<String>) jsonData.get("jobIDs");
//                for (String s : set) {
//                    addRowFromJson(jsonData, s);
//                }
//            }
//            exportTableModel.fireTableDataChanged();
//        } catch (FileNotFoundException e) {
//            throw new RuntimeException(e);
//        }
    }

    public void addRowFromJson(HashMap<String, Object> jsonData, String s){
        exportTableModel.addRow(new Object[]{""});
    }

    public void displayExportTable() {
        if (exportTableDialog == null) {
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
            JOptionPane pane = new JOptionPane(exportTablePanel, JOptionPane.PLAIN_MESSAGE, 0, null, new Object[]{"Close"});
            exportTableDialog = pane.createDialog("VCell Exports");
            exportTableDialog.setModal(false);
            exportTableDialog.setResizable(true);
            exportTableDialog.setVisible(true);
        } else {
            exportTableDialog.setVisible(true);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {

    }
}
