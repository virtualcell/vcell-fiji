package org.vcell.N5.UI;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ij.ImagePlus;
import ij.plugin.Duplicator;
import org.scijava.log.Logger;
import org.vcell.N5.ExportDataRepresentation;
import org.vcell.N5.N5ImageHandler;
import org.vcell.N5.SimResultsLoader;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

public class N5ExportTable implements ActionListener, ListSelectionListener {
    public static JDialog exportTableDialog;
    private N5ExportTableModel n5ExportTableModel;
    private ParameterTableModel parameterTableModel;
    private JTable parameterTable;
    private JTable exportListTable;

    private static JButton open;
    private static JButton copyLink;
    private static JButton refreshButton;
    private static JButton useN5Link;
    private static JButton questionMark;
    public static JCheckBox openInMemory;
    private JCheckBox todayInterval;
    private JCheckBox monthInterval;
    private JCheckBox yearlyInterval;
    private JCheckBox anyInterval;
    private JTextPane variableTextPanel;
    private final Border lowerEtchedBorder = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
    private static RemoteFileSelection remoteFileSelection;
    private final int paneWidth = 800;

    private final Logger logger = N5ImageHandler.getLogger(N5ExportTable.class);

    public N5ExportTable(){
        remoteFileSelection = new RemoteFileSelection();
    }

    public void initalizeTableData(){
        ExportDataRepresentation jsonData = null;
        n5ExportTableModel.resetData();
        try {
            jsonData = getJsonData();
            if (jsonData != null){
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

                ExportDataRepresentation.FormatExportDataRepresentation formatExportData = jsonData.formatData.get(N5ImageHandler.formatName);
                Stack<String> jobStack = formatExportData.formatJobIDs;
                while (!jobStack.isEmpty()){
                    String jobID = jobStack.pop();
                    DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
                    LocalDateTime exportDate = LocalDateTime.parse(formatExportData.simulationDataMap.get(jobID).exportDate, dateFormat);
                    if (exportDate.isBefore(pastTime)){
                        break;
                    }
                    n5ExportTableModel.appendRowData(formatExportData.simulationDataMap.get(jobID));
                }
            }
            n5ExportTableModel.fireTableDataChanged();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }


    private void initialize(){
        JPanel exportPanel = new JPanel();

        exportPanel.setLayout(new BorderLayout());
        exportPanel.add(topPanel(), BorderLayout.NORTH);
        JSplitPane jSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,tablePanel(), exportDetailsPanel());
        jSplitPane.setContinuousLayout(true);
        exportPanel.add(jSplitPane, BorderLayout.CENTER);

        exportPanel.setPreferredSize(new Dimension(paneWidth, 650));
        JOptionPane pane = new JOptionPane(exportPanel, JOptionPane.PLAIN_MESSAGE, 0, null, new Object[]{"Close"});
        exportTableDialog = pane.createDialog("VCell Exports");
        exportTableDialog.setModal(false);
        exportTableDialog.setResizable(true);
        exportTableDialog.setVisible(true);


        exportListTable.getSelectionModel().addListSelectionListener(this);

        initalizeTableData();
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

        JSplitPane exportDetails = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, jtextScrollPane, parameterTableScrollPane);
        exportDetails.setBorder(BorderFactory.createTitledBorder(lowerEtchedBorder, " Export Details "));
        exportDetails.setResizeWeight(0.5);
        exportDetails.setContinuousLayout(true);
        return exportDetails;
    }

    private JScrollPane tablePanel(){
        n5ExportTableModel = new N5ExportTableModel();
        exportListTable = new JTable(n5ExportTableModel);
        JScrollPane jScrollPane = new JScrollPane(exportListTable);

        jScrollPane.setPreferredSize(new Dimension(500, 400));
        jScrollPane.setBorder(BorderFactory.createTitledBorder(lowerEtchedBorder, " Export Table "));
        return jScrollPane;
    }

    private JPanel topPanel(){
        refreshButton = new JButton("Refresh");
        open = new JButton("Open");
        copyLink = new JButton("Copy Link");
        useN5Link = new JButton("Use N5 Link");
        questionMark = new JButton("?");
        openInMemory = new JCheckBox("Open In Memory");
        openInMemory.setSelected(true);


        JPanel userButtonsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        Insets buttonMargin = new Insets(2, 5, 2, 5);
        gridBagConstraints.insets = buttonMargin;
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        open.setMargin(buttonMargin);
        userButtonsPanel.add(open, gridBagConstraints);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        refreshButton.setMargin(buttonMargin);
        userButtonsPanel.add(refreshButton, gridBagConstraints);
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        copyLink.setMargin(buttonMargin);
        userButtonsPanel.add(copyLink, gridBagConstraints);
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        useN5Link.setMargin(buttonMargin);
        userButtonsPanel.add(useN5Link, gridBagConstraints);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        questionMark.setMargin(buttonMargin);
//        buttonsPanel.add(questionMark);
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        userButtonsPanel.add(openInMemory, gridBagConstraints);

        todayInterval = new JCheckBox("Past 24 Hours");
        monthInterval = new JCheckBox("Past Month");
        yearlyInterval = new JCheckBox("Past Year");
        anyInterval = new JCheckBox("Any Time");

        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(todayInterval);
        buttonGroup.add(monthInterval);
        buttonGroup.add(yearlyInterval);
        buttonGroup.add(anyInterval);

        JPanel timeFilter = new JPanel();
        timeFilter.add(todayInterval);
        timeFilter.add(monthInterval);
        timeFilter.add(yearlyInterval);
        timeFilter.add(anyInterval);
        timeFilter.setBorder(BorderFactory.createTitledBorder(lowerEtchedBorder, " Time Filter "));


        JPanel topBar = new JPanel();
        topBar.setPreferredSize(new Dimension(paneWidth, 100));
        topBar.setLayout(new BorderLayout());
        topBar.add(userButtonsPanel, BorderLayout.EAST);
        topBar.add(timeFilter, BorderLayout.WEST);
        topBar.setBorder(BorderFactory.createTitledBorder(lowerEtchedBorder, " User Options "));

        refreshButton.addActionListener(this);
        open.addActionListener(this);
        copyLink.addActionListener(this);
        questionMark.addActionListener(this);
        useN5Link.addActionListener(this);
        remoteFileSelection.submitS3Info.addActionListener(this);

        return topBar;
    }

    public static void enableCriticalButtons(boolean enable){
        useN5Link.setEnabled(enable);
        open.setEnabled(enable);
        refreshButton.setEnabled(enable);
        copyLink.setEnabled(enable);
        remoteFileSelection.submitS3Info.setEnabled(enable);
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
        }
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        int row = exportListTable.getSelectedRow();
        if (row > exportListTable.getRowCount() || row < 0){
            return;
        }
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

    public static ExportDataRepresentation.SimulationExportDataRepresentation getLastJSONElement() throws FileNotFoundException {
        ExportDataRepresentation jsonData = getJsonData();
        if (jsonData != null && jsonData.formatData.containsKey(N5ImageHandler.formatName)) {
            ExportDataRepresentation.FormatExportDataRepresentation formatExportDataRepresentation = jsonData.formatData.get(N5ImageHandler.formatName);
            Stack<String> formatJobIDs = formatExportDataRepresentation.formatJobIDs;
            String jobID = formatJobIDs.isEmpty() ? null : formatJobIDs.peek();

            return jobID == null ? null : formatExportDataRepresentation.simulationDataMap.get(jobID);
        }
        return null;
    }

    public static ExportDataRepresentation getJsonData() throws FileNotFoundException {
        File jsonFile = new File(System.getProperty("user.home") + "/.vcell", "exportMetaData.json");
        if (jsonFile.exists() && jsonFile.length() != 0){
            ExportDataRepresentation jsonHashMap;
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            jsonHashMap = gson.fromJson(new FileReader(jsonFile.getAbsolutePath()), ExportDataRepresentation.class);
            return jsonHashMap;
        }
        return null;

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
            add("BM Name");
            add("App Name");
            add("Sim Name");
            add("Time Slice");
            add("Date Exported");
            add("N5 File Name");
        }};

        private List<ExportDataRepresentation.SimulationExportDataRepresentation> tableData = new ArrayList<>();

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
            tableData = new ArrayList<>();
        }
        public void appendRowData(ExportDataRepresentation.SimulationExportDataRepresentation rowData){
            tableData.add(rowData);
        }
        public ExportDataRepresentation.SimulationExportDataRepresentation getLastRowData(){
            return tableData.get(0);
        }

    }
}
