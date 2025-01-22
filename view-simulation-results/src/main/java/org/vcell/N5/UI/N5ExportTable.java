package org.vcell.N5.UI;

import org.scijava.log.Logger;
import org.vcell.N5.ExportDataRepresentation;
import org.vcell.N5.N5ImageHandler;
import org.vcell.N5.UI.Filters.SearchBar;
import org.vcell.N5.UI.Filters.TimeFilter;
import org.vcell.N5.retrieving.SimLoadingListener;
import org.vcell.N5.retrieving.SimResultsLoader;

import javax.swing.*;
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

public class N5ExportTable extends JTabbedPane implements ListSelectionListener, SimLoadingListener {
    private N5ExportTableModel n5ExportTableModel;
    private JTable exportListTable;
    private JTable exampleExportListTable;
    private final Map<Integer, String> loadingRowsJobID = new HashMap<>();

    private final JScrollPane personalExportsPanel = new JScrollPane();
    private final JScrollPane exampleExportsPanel = new JScrollPane();


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
        exampleExportListTable = new JTable(n5ExportTableModel);
        personalExportsPanel.setViewportView(exportListTable);
        exampleExportsPanel.setViewportView(exampleExportListTable);

        this.addTab("Personal Exports", personalExportsPanel);
        this.addTab("Example Exports", exampleExportsPanel);


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
            exampleExportListTable.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
        }

        this.setPreferredSize(new Dimension(500, 400));
        exportListTable.getSelectionModel().addListSelectionListener(this);
        exampleExportListTable.getSelectionModel().addListSelectionListener(this);

        this.setSelectedIndex(N5ImageHandler.exportedDataExists() ? 0 : 1);
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
        try {
            boolean hasPersonalExports = N5ImageHandler.exportedDataExists() && this.getSelectedIndex() == 0;
            ExportDataRepresentation.FormatExportDataRepresentation formatExportData = hasPersonalExports ?
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
                    if (formatExportData != null && this.getSelectedIndex() == 0){
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

    public void openSelectedRows(boolean openInMemory, boolean performDataReduction, SimResultsLoader.OpenTag openTag){
        ArrayList<SimResultsLoader> filesToOpen = new ArrayList<>();
        JTable currentTable = getCurrentTable();
        for(int row: currentTable.getSelectedRows()){
            String uri = n5ExportTableModel.getRowData(row).uri;
            ExportDataRepresentation.SimulationExportDataRepresentation rowData = n5ExportTableModel.getRowData(row);
            SimResultsLoader simResultsLoader = new SimResultsLoader(uri, rowData.savedFileName, row, rowData.jobID, openTag);
            filesToOpen.add(simResultsLoader);
        }
        N5ImageHandler.loadingManager.openN5FileDataset(filesToOpen, openInMemory,
                performDataReduction);
    }

    public void copySelectedRowLink(){
        JTable currentTable = getCurrentTable();
        ExportDataRepresentation.SimulationExportDataRepresentation selectedRow = n5ExportTableModel.getRowData(currentTable.getSelectedRow());
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
        JTable currentTable = getCurrentTable();
        int row = currentTable.getSelectedRow();
        exportDetailsPanel.resetExportDetails();
        if (row > currentTable.getRowCount() || row < 0){
            controlPanel.disableAllContextDependentButtons();
            return;
        }
        MainPanel.setEnableParentAndChild(exportDetailsPanel, true);
        ExportDataRepresentation.SimulationExportDataRepresentation rowData = n5ExportTableModel.getRowData(row);
        exportDetailsPanel.addExportDetailEntries("Variables: " + rowData.variables, rowData.differentParameterValues);

        int loadingRow = loadingRowsJobID.containsKey(row) ? findLoadingRow(row, row) : -1;
        controlPanel.updateButtonsToMatchState(loadingRow != -1);
    }

    @Override
    public void setSelectedIndex(int index) {
        super.setSelectedIndex(index);
        updateTableData();
    }

    public void stopSelectedImageFromLoading(){
        JTable currentTable = getCurrentTable();
        int row = currentTable.getSelectedRow();
        N5ImageHandler.loadingManager.stopLoadingImage(n5ExportTableModel.tableData.get(row).jobID);
        loadingRowsJobID.remove(row);
        currentTable.repaint();
    }

    public void removeSpecificRowFromLoadingRows(int rowNumber){
        JTable currentTable = getCurrentTable();
        int realRowNumber = findLoadingRow(rowNumber, rowNumber);
        loadingRowsJobID.remove(realRowNumber);
        controlPanel.updateButtonsToMatchState(false);
        currentTable.repaint();
    }

    @Override
    public void simIsLoading(int itemRow, String exportID) {
        JTable currentTable = getCurrentTable();
        loadingRowsJobID.put(itemRow, exportID);
        currentTable.repaint();
    }

    @Override
    public void simFinishedLoading(SimResultsLoader loadedResults) {
        if (loadedResults.openTag == SimResultsLoader.OpenTag.VIEW){
            removeSpecificRowFromLoadingRows(loadedResults.rowNumber);
            loadedResults.getImagePlus().show();
        }
    }

    /**
     * Required, for there is one table for examples and another for personal exports. The same table
     * can not be used for both tabs, because a JComponent can have only one parent.
     */
    private JTable getCurrentTable(){
        return this.getSelectedIndex() == 0 ? exportListTable : exampleExportListTable;
    }

    public N5ExportTableModel getN5ExportTableModel(){
        return n5ExportTableModel;
    }

    public static class N5ExportTableModel extends AbstractTableModel {
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
