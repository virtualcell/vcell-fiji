package org.vcell.N5.UI;

import ij.ImagePlus;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.real.DoubleType;
import org.janelia.saalfeldlab.n5.N5FSReader;
import org.scijava.log.Logger;
import org.vcell.N5.ExportDataRepresentation;
import org.vcell.N5.N5ImageHandler;
import org.vcell.N5.SimCacheLoader;
import org.vcell.N5.SimResultsLoader;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class N5ExportTable implements ActionListener, ListSelectionListener {
    public static JDialog exportTableDialog;
    private N5ExportTableModel n5ExportTableModel;
    private ParameterTableModel parameterTableModel;
    private JTable parameterTable;
    private JTable exportListTable;
    private JScrollPane tableScrollPane;
    private JSplitPane exportDetails;

    private static JButton open;
    private static final JButton openLocal = new JButton("Open N5 Local");
    private static JButton copyLink;
    private static JButton refreshButton;
    private static JButton useN5Link;
    private static JButton questionMark;
    public static JCheckBox openInMemory;
    public static JCheckBox includeExampleExports;
    private JCheckBox todayInterval;
    private JCheckBox monthInterval;
    private JCheckBox yearlyInterval;
    private JCheckBox anyInterval;
    private JPanel timeFilter;
    private JTextPane variableTextPanel;
    private final Border lowerEtchedBorder = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
    private final Border exampleBorder = BorderFactory.createTitledBorder(lowerEtchedBorder, "Example Exports");
    private static RemoteFileSelection remoteFileSelection;
    private final int paneWidth = 800;

    private final Logger logger = N5ImageHandler.getLogger(N5ExportTable.class);

    public N5ExportTable(){
        remoteFileSelection = new RemoteFileSelection();
    }

    private LocalDateTime oldestTimeAllowed(){
        LocalDateTime pastTime = LocalDateTime.now();
        if (todayInterval.isSelected()){
            pastTime = pastTime.minusDays(1);
        } else if (monthInterval.isSelected()) {
            pastTime = pastTime.minusMonths(1);
        } else if (yearlyInterval.isSelected()) {
            pastTime = pastTime.minusYears(1);
        } else {
            pastTime = pastTime.minusYears(10); //Max date back is 10 years
        }
        return pastTime;
    }

    public void initalizeTableData(){
        n5ExportTableModel.resetData();
        tableScrollPane.setBorder(BorderFactory.createTitledBorder(lowerEtchedBorder, "Personal Exports"));
        try {
            ExportDataRepresentation.FormatExportDataRepresentation formatExportData = N5ImageHandler.getJsonData();
            if (formatExportData != null){
                Stack<String> jobStack = formatExportData.formatJobIDs;
                while (!jobStack.isEmpty()){
                    String jobID = jobStack.pop();
                    if (!n5ExportTableModel.appendRowData(formatExportData.simulationDataMap.get(jobID), oldestTimeAllowed())){
                        break;
                    }
                }
            }
            n5ExportTableModel.fireTableDataChanged();
            tableScrollPane.updateUI();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void updateExampleExportsToTable(){
        n5ExportTableModel.resetData();
        tableScrollPane.setBorder(exampleBorder);
        try{
            ExportDataRepresentation.FormatExportDataRepresentation exampleFormatExportData = N5ImageHandler.getExampleJSONData();
            Stack<String> exampleJobStack = (Stack<String>) exampleFormatExportData.formatJobIDs.clone();
            while (!exampleJobStack.isEmpty()){
                String jobID = exampleJobStack.pop();
                if (!n5ExportTableModel.appendRowData(exampleFormatExportData.simulationDataMap.get(jobID), oldestTimeAllowed())){
                    break;
                }
            }
            n5ExportTableModel.fireTableDataChanged();
            tableScrollPane.updateUI();
        }
        catch (FileNotFoundException e){
            throw new RuntimeException("Can't open example N5 export table.", e);
        }
    }

    private void automaticRefresh(){
        Thread refreshTableThread = new Thread(() -> {
            try {
                while(true){
                    ExportDataRepresentation.FormatExportDataRepresentation formatExportData = N5ImageHandler.getJsonData();
                    if (formatExportData != null && !tableScrollPane.getBorder().equals(exampleBorder)){
                        ExportDataRepresentation.SimulationExportDataRepresentation mostRecentTableEntry = !n5ExportTableModel.tableData.isEmpty() ? n5ExportTableModel.tableData.getFirst() : null;
                        Stack<String> jobStack = formatExportData.formatJobIDs;
                        boolean isUpdated = false;
                        while (!jobStack.isEmpty()){
                            String currentJob = jobStack.pop();
                            if (mostRecentTableEntry != null && (currentJob.equals(mostRecentTableEntry.jobID)
                                    || !formatExportData.simulationDataMap.containsKey(mostRecentTableEntry.jobID))){
                                break;
                            }
                            isUpdated = n5ExportTableModel.prependRowData(formatExportData.simulationDataMap.get(currentJob), oldestTimeAllowed());
                        }
                        if(isUpdated){
                            n5ExportTableModel.fireTableDataChanged();
                            tableScrollPane.updateUI();
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


    private void initialize(){
        JPanel parentPanel = new JPanel();

        parentPanel.setLayout(new BorderLayout());
        parentPanel.add(topPanel(), BorderLayout.NORTH);
        JSplitPane jSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,tablePanel(), exportDetailsPanel());
        jSplitPane.setContinuousLayout(true);
        parentPanel.add(jSplitPane, BorderLayout.CENTER);

        parentPanel.setPreferredSize(new Dimension(paneWidth, 650));
        JOptionPane pane = new JOptionPane(parentPanel, JOptionPane.PLAIN_MESSAGE, 0, null, new Object[]{"Close"});
        exportTableDialog = pane.createDialog("VCell Exports");
        exportTableDialog.setModal(false);
        exportTableDialog.setResizable(true);
        exportTableDialog.setVisible(true);
        if(!N5ImageHandler.exportedDataExists()){
            updateExampleExportsToTable();
        }
        else{
            initalizeTableData();
        }
        automaticRefresh();
    }


    public void displayExportTable() {
        if (exportTableDialog == null) {
            initialize();
        } else {
            exportTableDialog.setVisible(true);
        }
    }

    private JSplitPane exportDetailsPanel(){
        variableTextPanel = new JTextPane();
        parameterTableModel = new ParameterTableModel();
        parameterTable = new JTable(parameterTableModel);
        JScrollPane parameterTableScrollPane = new JScrollPane(parameterTable);
        parameterTableScrollPane.setPreferredSize(new Dimension(paneWidth / 2, 80));
        parameterTableScrollPane.setBorder(BorderFactory.createTitledBorder(lowerEtchedBorder, " Parameter Values "));
//        jTextPane.setSize(800, 200);
        variableTextPanel.setEditable(false);
        JScrollPane jtextScrollPane = new JScrollPane(variableTextPanel);
        jtextScrollPane.setPreferredSize(new Dimension(paneWidth / 2, 80));
        jtextScrollPane.setBorder(BorderFactory.createTitledBorder(lowerEtchedBorder, " Variables "));

        exportDetails = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, jtextScrollPane, parameterTableScrollPane);
        exportDetails.setBorder(BorderFactory.createTitledBorder(lowerEtchedBorder, " Export Details "));
        exportDetails.setResizeWeight(0.5);
        exportDetails.setContinuousLayout(true);

        setEnableParentAndChild(exportDetails, false);
        return exportDetails;
    }

    private JScrollPane tablePanel(){
        n5ExportTableModel = new N5ExportTableModel();
        exportListTable = new JTable(n5ExportTableModel);
        tableScrollPane = new JScrollPane(exportListTable);


        tableScrollPane.setPreferredSize(new Dimension(500, 400));
        tableScrollPane.setBorder(BorderFactory.createTitledBorder(lowerEtchedBorder, "Export Table"));
        exportListTable.getSelectionModel().addListSelectionListener(this);

        return tableScrollPane;
    }

    private JPanel topPanel(){
        refreshButton = new JButton("Refresh");
        open = new JButton("Open");
        copyLink = new JButton("Copy Link");
        useN5Link = new JButton("Use N5 Link");
        questionMark = new JButton("?");
        questionMark.setPreferredSize(new Dimension(20, 20));
        openInMemory = new JCheckBox("Open In Memory");
        openInMemory.setSelected(false);
        includeExampleExports = new JCheckBox("Show Example Exports");
        includeExampleExports.setSelected(!N5ImageHandler.exportedDataExists());

        GridBagConstraints gridBagConstraints = new GridBagConstraints();

        JPanel topRow = new JPanel(new GridBagLayout());
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        topRow.add(open, gridBagConstraints);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        topRow.add(copyLink, gridBagConstraints);
        gridBagConstraints.gridx = 2;
        topRow.add(useN5Link, gridBagConstraints);
        gridBagConstraints.gridx = 3;
        topRow.add(questionMark, gridBagConstraints);

        JPanel bottomRow = new JPanel(new GridBagLayout());
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        bottomRow.add(includeExampleExports, gridBagConstraints);
        gridBagConstraints.gridx = 1;
        bottomRow.add(openInMemory, gridBagConstraints);


        JPanel userButtonsPanel = new JPanel(new GridBagLayout());
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        userButtonsPanel.add(topRow, gridBagConstraints);
        gridBagConstraints.gridy = 1;
        userButtonsPanel.add(bottomRow, gridBagConstraints);

//        buttonsPanel.add(questionMark);


        todayInterval = new JCheckBox("Past 24 Hours");
        monthInterval = new JCheckBox("Past Month");
        yearlyInterval = new JCheckBox("Past Year");
        anyInterval = new JCheckBox("Any Time");

        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(todayInterval);
        buttonGroup.add(monthInterval);
        buttonGroup.add(yearlyInterval);
        buttonGroup.add(anyInterval);

        timeFilter = new JPanel();
        timeFilter.add(todayInterval);
        timeFilter.add(monthInterval);
        timeFilter.add(yearlyInterval);
        timeFilter.add(anyInterval);
        timeFilter.setBorder(BorderFactory.createTitledBorder(lowerEtchedBorder, " Time Filter "));


        JPanel topBar = new JPanel();
        topBar.setPreferredSize(new Dimension(paneWidth, 100));
        topBar.setLayout(new BorderLayout());
        topBar.add(openLocal);
        topBar.add(userButtonsPanel, BorderLayout.EAST);
        topBar.add(timeFilter, BorderLayout.WEST);
        topBar.setBorder(BorderFactory.createTitledBorder(lowerEtchedBorder, " User Options "));


        refreshButton.addActionListener(this);
        open.addActionListener(this);
        copyLink.addActionListener(this);
        questionMark.addActionListener(this);
        useN5Link.addActionListener(this);
        includeExampleExports.addActionListener(this);
        openLocal.addActionListener(this);

        Enumeration<AbstractButton> b = buttonGroup.getElements();
        while (b.hasMoreElements()){
            b.nextElement().addActionListener(this);
        }
        
        open.setEnabled(false);
        copyLink.setEnabled(false);

        return topBar;
    }

    public static void enableCriticalButtons(boolean enable){
        useN5Link.setEnabled(enable);
        open.setEnabled(enable);
        refreshButton.setEnabled(enable);
        copyLink.setEnabled(enable);
        remoteFileSelection.submitS3Info.setEnabled(enable);
    }

    public static void setEnableParentAndChild(Container container, boolean enable){
        container.setEnabled(enable);
        for (Component component : container.getComponents()){
            if (component instanceof Container){
                setEnableParentAndChild((Container) component, enable);
            }
            component.setEnabled(enable);
            if(component instanceof JTable){
                Enumeration<TableColumn> columns = ((JTable) component).getColumnModel().getColumns();
                while (columns.hasMoreElements()){
                    columns.nextElement().setHeaderRenderer(new DefaultTableCellRenderer(){
                        @Override
                        public Component getTableCellRendererComponent(JTable table,Object value,boolean isSelected,boolean hasFocus,int row,int column) {
                            Component c = super.getTableCellRendererComponent(table,value,isSelected,hasFocus,row,column);
                            c.setForeground(enable ? Color.BLACK : Color.GRAY);
                            return c;
                        }
                    });
                }
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if(e.getSource().equals(open)){
            ArrayList<SimResultsLoader> filesToOpen = new ArrayList<>();
            for(int row: exportListTable.getSelectedRows()){
                String uri = n5ExportTableModel.getRowData(row).uri;
                SimResultsLoader simResultsLoader = new SimResultsLoader(uri, n5ExportTableModel.getRowData(row).savedFileName);
                filesToOpen.add(simResultsLoader);
            }
            SimResultsLoader.openN5FileDataset(filesToOpen, openInMemory.isSelected());
        } else if (e.getSource().equals(copyLink)) {
            ExportDataRepresentation.SimulationExportDataRepresentation selectedRow = n5ExportTableModel.getRowData(exportListTable.getSelectedRow());
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(new StringSelection(selectedRow.uri), null);
        } else if (e.getSource().equals(refreshButton)) {
            initalizeTableData();
        } else if (e.getSource().equals(questionMark)) {
            new HelpExplanation().displayHelpMenu();
        } else if (e.getSource().equals(useN5Link)) {
            remoteFileSelection.setVisible(true);
        } else if (e.getSource().equals(openLocal)){ // This button is not displayed to the end user
            ArrayList<SimResultsLoader> filesToOpen = new ArrayList<>();
            for(int row: exportListTable.getSelectedRows()){
                String uri = n5ExportTableModel.getRowData(row).uri;
                SimResultsLoader simResultsLoader = new SimResultsLoader(uri, n5ExportTableModel.getRowData(row).savedFileName);
                filesToOpen.add(simResultsLoader);
            }
            SimResultsLoader.openLocalN5FS(filesToOpen);
        }
        else if (e.getSource().equals(includeExampleExports)){
            if(includeExampleExports.isSelected()){
                updateExampleExportsToTable();
                return;
            }
            initalizeTableData();
        } else if (e.getSource().equals(anyInterval) || e.getSource().equals(todayInterval)
                || e.getSource().equals(monthInterval) || e.getSource().equals(yearlyInterval)) {
            if(includeExampleExports.isSelected()){
                updateExampleExportsToTable();
                return;
            }
            initalizeTableData();
        }
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        int row = exportListTable.getSelectedRow();
        if (row > exportListTable.getRowCount() || row < 0){
            parameterTableModel.resetTableData();
            variableTextPanel.setText("");
            open.setEnabled(false);
            copyLink.setEnabled(false);
            setEnableParentAndChild(exportDetails, false);
            return;
        }
        open.setEnabled(true);
        copyLink.setEnabled(true);
        setEnableParentAndChild(exportDetails, true);
//        AttributeSet attributeSet = styleContext.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.)
        ExportDataRepresentation.SimulationExportDataRepresentation rowData = n5ExportTableModel.getRowData(row);
        variableTextPanel.setText("Variables: " + rowData.variables);

        parameterTableModel.resetTableData();
        for(String parameterValues : rowData.differentParameterValues){
            String[] tokens = parameterValues.split(":");
            parameterTableModel.addRowData(tokens[0], tokens[1], tokens[2]);
        }

        variableTextPanel.updateUI();
        parameterTable.updateUI();
    }


    static class ParameterTableModel extends AbstractTableModel{

        private final static String parameterHeader = "Parameter";
        private final static String defaultValueHeader = "Default Value";
        private final static String newValueHeader = "New Value";


        private List<HashMap<String, String>> tableData = new ArrayList<>();
        private final ArrayList<String> headers = new ArrayList<String>(){{
            add(parameterHeader);
            add(defaultValueHeader);
            add(newValueHeader);
        }};
        @Override
        public String getColumnName(int column) {
            return headers.get(column);
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
        public Object getValueAt(int rowIndex, int columnIndex) {
            HashMap<String, String> rowData = tableData.get(rowIndex);
            if(columnIndex == headers.indexOf(parameterHeader)){
                return rowData.get(parameterHeader);
            } else if (columnIndex == headers.indexOf(defaultValueHeader)) {
                return rowData.get(defaultValueHeader);
            } else if (columnIndex == headers.indexOf(newValueHeader)) {
                return rowData.get(newValueHeader);
            }
            return null;
        }

        public void addRowData(String parameterName, String defaultValue, String newValue){
            HashMap<String, String> data = new HashMap<String, String>(){{
                put(parameterHeader, parameterName);
                put(defaultValueHeader, defaultValue);
                put(newValueHeader, newValue);
            }};
            tableData.add(data);
        }

        public void resetTableData(){
            tableData = new ArrayList<>();
        }

    }

    static class N5ExportTableModel extends AbstractTableModel {
        public final ArrayList<String> headers = new ArrayList<String>(){{
            add("BioModel");
            add("Application");
            add("Simulation");
            add("Time Slice");
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
            } else if (columnIndex == headers.indexOf("Time Slice")) {
                return  data.startAndEndTime;
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
        public boolean appendRowData(ExportDataRepresentation.SimulationExportDataRepresentation rowData, LocalDateTime oldestExportAllowed){
            DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
            LocalDateTime exportDate = LocalDateTime.parse(rowData.exportDate, dateFormat);
            if (exportDate.isBefore(oldestExportAllowed)){
                return false;
            }
            tableData.add(rowData);
            return true;
        }

        public boolean prependRowData(ExportDataRepresentation.SimulationExportDataRepresentation rowData, LocalDateTime oldestExportAllowed){
            DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
            LocalDateTime exportDate = LocalDateTime.parse(rowData.exportDate, dateFormat);
            if (exportDate.isBefore(oldestExportAllowed)){
                return false;
            }
            tableData.addFirst(rowData);
            return true;
        }

        public ExportDataRepresentation.SimulationExportDataRepresentation getLastRowData(){
            return tableData.get(0);
        }

    }
}
