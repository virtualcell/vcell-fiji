package org.vcell.vcellfiji.UI;

import ij.ImagePlus;
import org.janelia.saalfeldlab.n5.s3.N5AmazonS3Reader;
import org.janelia.saalfeldlab.n5.*;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.vcell.vcellfiji.ExportDataRepresentation;
import org.vcell.vcellfiji.N5ImageHandler;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class N5ExportTable implements ActionListener {
    private JDialog exportTableDialog;
    private JPanel exportTablePanel;
    private N5ExportTableModel n5ExportTableModel;
    private JButton open;
    private JTable jTable;
    @Parameter
    private LogService logService;
    private N5ImageHandler n5ImageHandler;

    public N5ExportTable(N5ImageHandler n5ImageHandler){
        initialize();
        this.n5ImageHandler = n5ImageHandler;
    }
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
        ExportDataRepresentation jsonData = null;
        try {
            jsonData = N5ImageHandler.getJsonData();
            if (jsonData != null){
                ExportDataRepresentation.FormatExportDataRepresentation formatExportData = jsonData.formatData.get(N5ImageHandler.formatName);
                Stack<String> jobStack = formatExportData.formatJobIDs;
                while (!jobStack.isEmpty()){
                    n5ExportTableModel.addRowData(formatExportData.simulationDataMap.get(jobStack.pop()));
                }
            }
            n5ExportTableModel.fireTableDataChanged();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }


    private void initialize(){
        exportTablePanel = new JPanel();

        n5ExportTableModel = new N5ExportTableModel();

        jTable = new JTable(n5ExportTableModel);
        JScrollPane jScrollPane = new JScrollPane(jTable);

        jScrollPane.setSize(500, 400);
        jScrollPane.setPreferredSize(new Dimension(500, 400));
        jScrollPane.setMinimumSize(new Dimension(500, 400));

        JLabel jLabel = new JLabel("Recent Exports. List is volatile save important export metadata elsewhere.");
        JButton refresh = new JButton("Refresh List");
        open = new JButton("Open");

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

        initalizeTableData();
    }


    public void displayExportTable() {
        if (exportTableDialog == null) {
            initialize();
        } else {
            exportTableDialog.setVisible(true);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if(e.getSource().equals(open)){
            int[] selectedRows = jTable.getSelectedRows();
            for(int row: selectedRows){
                String uri = n5ExportTableModel.getRowData(row).uri;
                String datasetName = n5ExportTableModel.getRowData(row).savedFileName;
                n5ImageHandler.createS3Client(uri);
                N5AmazonS3Reader n5Reader = n5ImageHandler.getN5AmazonS3Reader();
                try {
                    ImagePlus imagePlus = n5ImageHandler.getImgPlusFromN5File(datasetName, n5Reader);
                    imagePlus.show();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
    }

    static class N5ExportTableModel extends AbstractTableModel {
        public final ArrayList<String> headers = new ArrayList<String>(){{
            add("BM Name");
            add("App Name");
            add("Sim Name");
            add("Time Slice");
            add("Variables");
            add("Default Parameters");
            add("Set Parameters");
            add("Date Exported");
            add("Dataset Name");
        }};

        private final List<ExportDataRepresentation.SimulationExportDataRepresentation> tableData = new ArrayList<>();

        public N5ExportTableModel(){
        }

        @Override
        public int getRowCount() {
            return tableData.size();
        }

        @Override
        public int getColumnCount() {
            return headers.size();
        }

        @Override
        public String getColumnName(int column) {
            return headers.get(column);
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            ExportDataRepresentation.SimulationExportDataRepresentation data = getRowData(rowIndex);
            if (columnIndex == headers.indexOf("App Name")){
                return data.applicationName;
            } else if (columnIndex == headers.indexOf("BM Name")) {
                return data.biomodelName;
            } else if (columnIndex == headers.indexOf("Sim Name")) {
                return data.simulationName;
            } else if (columnIndex == headers.indexOf("Time Slice")) {
                return  data.startAndEndTime;
            } else if (columnIndex == headers.indexOf("Variables")) {
                return data.variables;
            } else if (columnIndex == headers.indexOf("Date Exported")) {
                return data.exportDate;
            }
            else if (columnIndex == headers.indexOf("Default Parameters")) {
                return String.valueOf(data.defaultParameterValues);
            }
            else if (columnIndex == headers.indexOf("Set Parameters")) {
                return String.valueOf(data.setParameterValues);
            }else if (columnIndex == headers.indexOf("Dataset Name")) {
                return String.valueOf(data.savedFileName);
            }
            return null;
        }

        public ExportDataRepresentation.SimulationExportDataRepresentation getRowData(int rowIndex){
            return tableData.get(rowIndex);
        }

        public void addRowData(ExportDataRepresentation.SimulationExportDataRepresentation rowData){
            tableData.add(rowData);
        }
    }
}
