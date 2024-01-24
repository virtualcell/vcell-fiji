package org.vcell.N5.UI;

import ij.ImagePlus;
import org.janelia.saalfeldlab.n5.s3.N5AmazonS3Reader;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.vcell.N5.ExportDataRepresentation;
import org.vcell.N5.N5ImageHandler;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class N5ExportTable implements ActionListener, ListSelectionListener {
    private JDialog exportTableDialog;
    private JPanel exportTablePanel;
    private N5ExportTableModel n5ExportTableModel;
    private JButton open;
    private JButton copyLink;
    private JButton refreshButton;
    private JTable jTable;
    private JTextPane jTextPane;
    @Parameter
    private LogService logService;
    private N5ImageHandler n5ImageHandler;
    private final int paneWidth = 800;

    public N5ExportTable(N5ImageHandler n5ImageHandler){
        initialize();
        this.n5ImageHandler = n5ImageHandler;
    }
    public void updateTableModel(){
        try{
            ExportDataRepresentation jsonData = N5ImageHandler.getJsonData();
            if (jsonData != null){
                ExportDataRepresentation.FormatExportDataRepresentation formatData = jsonData.formatData.get(N5ImageHandler.formatName);
                Stack<String> formatJobIDs = formatData.formatJobIDs;
                ExportDataRepresentation.SimulationExportDataRepresentation lastTableElement = n5ExportTableModel.getLastRowData();
                ExportDataRepresentation.SimulationExportDataRepresentation recentExport = formatData.simulationDataMap.get(formatJobIDs.pop());
                while (!recentExport.jobID.equals(lastTableElement.jobID)){
                    n5ExportTableModel.addRowData(recentExport, 0);
                    recentExport = formatData.simulationDataMap.get(formatJobIDs.pop());
                }
            }
            jTable.updateUI();
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
                    n5ExportTableModel.appendRowData(formatExportData.simulationDataMap.get(jobStack.pop()));
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

        JTextArea label = new JTextArea("Recent Exports. List is volatile save important export metadata elsewhere.");
        label.setLineWrap(true);
        refreshButton = new JButton("Refresh List");
        open = new JButton("Open");
        copyLink = new JButton("Copy Link");

        jTextPane = new JTextPane();
//        jTextPane.setSize(800, 200);
        jTextPane.setEditable(false);
        JScrollPane jtextScrollPane = new JScrollPane(jTextPane);
        jtextScrollPane.setSize(paneWidth / 2, 200);
        jtextScrollPane.setPreferredSize(new Dimension(paneWidth / 2, 80));

        JPanel buttonsPanel = new JPanel(new FlowLayout());
        buttonsPanel.add(open);
        buttonsPanel.add(refreshButton);
        buttonsPanel.add(copyLink);

        JPanel buttonsAndDescription = new JPanel(new BorderLayout());
        buttonsAndDescription.setPreferredSize(new Dimension(paneWidth / 2, 20));
        label.setBackground(buttonsAndDescription.getBackground());
        buttonsAndDescription.add(buttonsPanel, BorderLayout.SOUTH);
        buttonsAndDescription.add(label, BorderLayout.NORTH);

        JPanel topBar = new JPanel();
        topBar.setPreferredSize(new Dimension(paneWidth / 2, 100));
        topBar.setLayout(new BorderLayout());
        topBar.add(buttonsAndDescription, BorderLayout.WEST);
        topBar.add(jtextScrollPane, BorderLayout.EAST);

        exportTablePanel.setLayout(new BorderLayout());
        exportTablePanel.add(topBar, BorderLayout.NORTH);
        exportTablePanel.add(jScrollPane);

        exportTablePanel.setPreferredSize(new Dimension(paneWidth, 600));
        JOptionPane pane = new JOptionPane(exportTablePanel, JOptionPane.PLAIN_MESSAGE, 0, null, new Object[]{"Close"});
        exportTableDialog = pane.createDialog("VCell Exports");
        exportTableDialog.setModal(false);
        exportTableDialog.setResizable(true);
        exportTableDialog.setVisible(true);

        refreshButton.addActionListener(this);
        open.addActionListener(this);
        copyLink.addActionListener(this);
        jTable.getSelectionModel().addListSelectionListener(this);

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
            exportTableDialog.setCursor(new Cursor(Cursor.WAIT_CURSOR));
            for(int row: selectedRows){
                String uri = n5ExportTableModel.getRowData(row).uri;
                String datasetName = n5ExportTableModel.getRowData(row).savedFileName;
                n5ImageHandler.createS3Client(uri);
                N5AmazonS3Reader n5Reader = n5ImageHandler.getN5AmazonS3Reader();
                try {
                    ImagePlus imagePlus = n5ImageHandler.getImgPlusFromN5File(datasetName, n5Reader);
                    imagePlus.show();
                    exportTableDialog.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                } catch (IOException ex) {
                    exportTableDialog.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                    throw new RuntimeException(ex);
                }
            }
        } else if (e.getSource().equals(copyLink)) {
            ExportDataRepresentation.SimulationExportDataRepresentation selectedRow = n5ExportTableModel.getRowData(jTable.getSelectedRow());
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(new StringSelection(selectedRow.uri), null);
        } else if (e.getSource().equals(refreshButton)) {
            updateTableModel();
        }
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        int row = jTable.getSelectedRow();
        StyleContext styleContext = new StyleContext();
//        AttributeSet attributeSet = styleContext.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.)
        String defaultParameterValues = n5ExportTableModel.getRowData(row).defaultParameterValues.toString();
        String actualParameterValues = n5ExportTableModel.getRowData(row).setParameterValues.toString();
        jTextPane.setText("Set Parameter Values: " + actualParameterValues + "\n \nDefault Parameter Values: " + defaultParameterValues);
        jTextPane.updateUI();
    }

    static class N5ExportTableModel extends AbstractTableModel {
        public final ArrayList<String> headers = new ArrayList<String>(){{
            add("BM Name");
            add("App Name");
            add("Sim Name");
            add("Time Slice");
            add("Variables");
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
            } else if (columnIndex == headers.indexOf("Dataset Name")) {
                return String.valueOf(data.savedFileName);
            }
            return null;
        }

        public ExportDataRepresentation.SimulationExportDataRepresentation getRowData(int rowIndex){
            return tableData.get(rowIndex);
        }

        public void addRowData(ExportDataRepresentation.SimulationExportDataRepresentation rowData, int index){
            tableData.add(index, rowData);
        }
        public void appendRowData(ExportDataRepresentation.SimulationExportDataRepresentation rowData){
            tableData.add(rowData);
        }
        public ExportDataRepresentation.SimulationExportDataRepresentation getLastRowData(){
            return tableData.get(0);
        }

    }
}
