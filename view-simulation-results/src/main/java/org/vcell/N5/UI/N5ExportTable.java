package org.vcell.N5.UI;

import ij.ImagePlus;
import org.scijava.log.Logger;
import org.vcell.N5.ExportDataRepresentation;
import org.vcell.N5.N5ImageHandler;
import org.vcell.N5.UI.Filters.SearchBar;
import org.vcell.N5.UI.Filters.TimeFilter;
import org.vcell.N5.retrieving.SimLoadingListener;
import org.vcell.N5.retrieving.SimResultsLoader;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.FileNotFoundException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class N5ExportTable extends JScrollPane implements ListSelectionListener, SimLoadingListener {
    private N5ExportTableModel n5ExportTableModel;
    private JTable exportListTable;

    private final Border lowerEtchedBorder = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
    private final Border exampleBorder = BorderFactory.createTitledBorder(lowerEtchedBorder, "Example Exports");
    private final Map<Integer, String> loadingRowsJobID = new HashMap<>();

    private ControlButtonsPanel controlPanel;
    private ExportDetailsPanel exportDetailsPanel;
    private TimeFilter timeFilter;


    private final Logger logger = N5ImageHandler.getLogger(N5ExportTable.class);

    public N5ExportTable(){}

    public void initialize(ControlButtonsPanel controlButtonsPanel, ExportDetailsPanel exportDetailsPanel,
                           TimeFilter timeFilter){
        this.controlPanel = controlButtonsPanel;
        this.exportDetailsPanel = exportDetailsPanel;
        this.timeFilter = timeFilter;
        N5ImageHandler.loadingManager.addSimLoadingListener(this);
        n5ExportTableModel = new N5ExportTableModel();
        exportListTable = new JTable(n5ExportTableModel);
        this.setViewportView(exportListTable);


        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer(){
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component cell = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                int actualRow = loadingRowsJobID.containsKey(row) ? findLoadingRow(row, row) : -1;
                if (actualRow != -1){
                    cell = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, actualRow, column);
                    cell.setForeground(Color.decode("#228B22"));
                } else {
                    cell.setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
                }
                return cell;
            }
        };

        int columns = n5ExportTableModel.getColumnCount();
        for (int i = 0; i < columns; i++){
            exportListTable.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
        }

        this.setPreferredSize(new Dimension(500, 400));
        this.setBorder(BorderFactory.createTitledBorder(lowerEtchedBorder, "Export Table"));
        exportListTable.getSelectionModel().addListSelectionListener(this);

        updateTableData("");
        automaticRefresh();
    }

    public void updateTableData(){
        updateTableData(SearchBar.searchTextField.getText());
    }

    public void updateTableData(String strFilter){
        // when initializing it is null
        if (controlPanel == null){
            updateTableData(LocalDateTime.now().minusYears(10), strFilter);
        } else {
            updateTableData(timeFilter.oldestTimeAllowed(), strFilter);
        }
    }

    void updateTableData(LocalDateTime oldestTimeAllowed, String strFilter){
        n5ExportTableModel.resetData();
        if (!controlPanel.includeExampleExports.isSelected()){
            this.setBorder(BorderFactory.createTitledBorder(lowerEtchedBorder, "Personal Exports"));
        } else {
            this.setBorder(exampleBorder);
        }
        try {
            ExportDataRepresentation.FormatExportDataRepresentation formatExportData = N5ImageHandler.exportedDataExists() && !controlPanel.includeExampleExports.isSelected() ?
                    N5ImageHandler.getJsonData() : N5ImageHandler.getExampleJSONData();

            Stack<String> jobStack = (Stack<String>) formatExportData.formatJobIDs.clone();
            while (!jobStack.isEmpty()){
                String jobID = jobStack.pop();
                n5ExportTableModel.addToRowData(formatExportData.simulationDataMap.get(jobID), oldestTimeAllowed,
                        strFilter, true);
            }

            n5ExportTableModel.fireTableDataChanged();
            this.updateUI();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void automaticRefresh(){
        Thread refreshTableThread = new Thread(() -> {
            try {
                while(true){
                    ExportDataRepresentation.FormatExportDataRepresentation formatExportData = N5ImageHandler.getJsonData();
                    if (formatExportData != null && !controlPanel.includeExampleExports.isSelected()){
                        ExportDataRepresentation.SimulationExportDataRepresentation mostRecentTableEntry = !n5ExportTableModel.tableData.isEmpty() ? n5ExportTableModel.tableData.getFirst() : null;
                        Stack<String> jobStack = formatExportData.formatJobIDs;
                        boolean isUpdated = false;
                        while (!jobStack.isEmpty()){
                            String currentJob = jobStack.pop();
                            if (mostRecentTableEntry != null && (currentJob.equals(mostRecentTableEntry.jobID)
                                    || !formatExportData.simulationDataMap.containsKey(mostRecentTableEntry.jobID))){
                                break;
                            }
                            isUpdated = n5ExportTableModel.addToRowData(formatExportData.simulationDataMap.get(currentJob),
                                    timeFilter.oldestTimeAllowed(), SearchBar.searchTextField.getText(),
                                    false);
                        }
                        if(isUpdated){
                            n5ExportTableModel.fireTableDataChanged();
                            this.updateUI();
                            exportListTable.repaint();
                        }
                    }
                    Thread.sleep(TimeUnit.SECONDS.toMillis(2));
                }
            } catch (FileNotFoundException e) {
                throw new RuntimeException("Problem Loading Export JSON",e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        refreshTableThread.start();
    }

    public void openSelectedRows(boolean openInMemory){
        ArrayList<SimResultsLoader> filesToOpen = new ArrayList<>();
        for(int row: exportListTable.getSelectedRows()){
            String uri = n5ExportTableModel.getRowData(row).uri;
            ExportDataRepresentation.SimulationExportDataRepresentation rowData = n5ExportTableModel.getRowData(row);
            SimResultsLoader simResultsLoader = new SimResultsLoader(uri, rowData.savedFileName, row, rowData.jobID);
            filesToOpen.add(simResultsLoader);
        }
        AdvancedFeatures advancedFeatures = MainPanel.controlButtonsPanel.advancedFeatures;
        N5ImageHandler.loadingManager.openN5FileDataset(filesToOpen, openInMemory,
                advancedFeatures.dataReduction.isSelected());
    }

    public void copySelectedRowLink(){
        ExportDataRepresentation.SimulationExportDataRepresentation selectedRow = n5ExportTableModel.getRowData(exportListTable.getSelectedRow());
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(new StringSelection(selectedRow.uri), null);
    }

    // The table may have changed by refreshing it, so if past idea of affected row no longer align correct it
    private int findLoadingRow(int currentRow, int initialRow){
        String expectedJobID = loadingRowsJobID.get(initialRow);
        if (currentRow < 0 || currentRow == n5ExportTableModel.getRowCount()){
            return -1;
        }
        else if (n5ExportTableModel.getRowData(currentRow).jobID.equals(expectedJobID) &&
                currentRow < n5ExportTableModel.getRowCount()){
            if (currentRow != initialRow){
                loadingRowsJobID.put(currentRow, expectedJobID);
                loadingRowsJobID.remove(initialRow);
            }
            return currentRow;
        } else {
            return findLoadingRow(currentRow + 1, initialRow);
        }
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        int row = exportListTable.getSelectedRow();
        exportDetailsPanel.resetExportDetails();
        if (row > exportListTable.getRowCount() || row < 0){
            controlPanel.enableRowContextDependentButtons(false);
            return;
        }
        controlPanel.enableRowContextDependentButtons(true);
        MainPanel.setEnableParentAndChild(exportDetailsPanel, true);
        ExportDataRepresentation.SimulationExportDataRepresentation rowData = n5ExportTableModel.getRowData(row);
        exportDetailsPanel.addExportDetailEntries("Variables: " + rowData.variables, rowData.differentParameterValues);

        int loadingRow = loadingRowsJobID.containsKey(row) ? findLoadingRow(row, row) : -1;
        controlPanel.allowCancel(loadingRow != -1);
    }

    public void removeFromLoadingRows(){
        int row = exportListTable.getSelectedRow();
        N5ImageHandler.loadingManager.stopOpeningSimulation(n5ExportTableModel.tableData.get(row).jobID);
        loadingRowsJobID.remove(row);
        exportListTable.repaint();
    }

    @Override
    public void simIsLoading(int itemRow, String exportID) {
        loadingRowsJobID.put(itemRow, exportID);
        exportListTable.repaint();
    }

    @Override
    public void simFinishedLoading(int itemRow, String exportID, ImagePlus imagePlus) {
        loadingRowsJobID.remove(itemRow);
        exportListTable.repaint();
        controlPanel.allowCancel(false);
        imagePlus.show();
    }

    static class N5ExportTableModel extends AbstractTableModel {
        public final ArrayList<String> headers = new ArrayList<String>(){{
            add("BioModel");
            add("Application");
            add("Simulation");
            add("Channel,Z,Time");
            add("Date Exported");
            add("N5 File Name");
        }};

        private LinkedList<ExportDataRepresentation.SimulationExportDataRepresentation> tableData = new LinkedList<>();

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
            if (columnIndex == headers.indexOf("Application")){
                return data.applicationName;
            } else if (columnIndex == headers.indexOf("BioModel")) {
                return data.biomodelName;
            } else if (columnIndex == headers.indexOf("Simulation")) {
                return data.simulationName;
            } else if (columnIndex == headers.indexOf("Channel,Z,Time")) {
                return String.format("%s,%s,%s", data.numVariables, data.zSlices, data.tSlices);
            } else if (columnIndex == headers.indexOf("Date Exported")) {
                return data.exportDate;
            } else if (columnIndex == headers.indexOf("N5 File Name")) {
                return String.valueOf(data.savedFileName);
            }
            return null;
        }

        public ExportDataRepresentation.SimulationExportDataRepresentation getRowData(int rowIndex){
            return tableData.get(rowIndex);
        }
        public void resetData(){
            tableData = new LinkedList<>();
        }
        private boolean stringFilter(ExportDataRepresentation.SimulationExportDataRepresentation rowData, String strFilter){
            return rowData.biomodelName.contains(strFilter) || rowData.applicationName.contains(strFilter) ||
                    rowData.simulationName.contains(strFilter) || rowData.savedFileName.contains(strFilter);
        }
        public boolean addToRowData(ExportDataRepresentation.SimulationExportDataRepresentation rowData, LocalDateTime oldestExportAllowed, String strFilter, boolean append){
            DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
            LocalDateTime exportDate = LocalDateTime.parse(rowData.exportDate, dateFormat);
            if (exportDate.isBefore(oldestExportAllowed) || !stringFilter(rowData, strFilter)){
                return false;
            }
            if (append){
                tableData.add(rowData);
            } else {
                tableData.addFirst(rowData);
            }
            return true;
        }

        public ExportDataRepresentation.SimulationExportDataRepresentation getLastRowData(){
            return tableData.get(0);
        }

    }
}
