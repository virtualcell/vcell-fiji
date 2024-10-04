package org.vcell.N5.UI;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ExportDetailsPanel extends JSplitPane {
    private final JTextPane variableTextPanel;
    private final Border lowerEtchedBorder = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
    private final ParameterTableModel parameterTableModel;
    private final JTable parameterTable;


    public ExportDetailsPanel(){
        super(JSplitPane.HORIZONTAL_SPLIT);
        variableTextPanel = new JTextPane();
        parameterTableModel = new ParameterTableModel();
        parameterTable = new JTable(parameterTableModel);
        JScrollPane parameterTableScrollPane = new JScrollPane(parameterTable);
        int paneWidth = 800;
        parameterTableScrollPane.setPreferredSize(new Dimension(paneWidth / 2, 80));
        parameterTableScrollPane.setBorder(BorderFactory.createTitledBorder(lowerEtchedBorder, " Parameter Values "));
//        jTextPane.setSize(800, 200);
        variableTextPanel.setEditable(false);
        JScrollPane jtextScrollPane = new JScrollPane(variableTextPanel);
        jtextScrollPane.setPreferredSize(new Dimension(paneWidth / 2, 80));
        jtextScrollPane.setBorder(BorderFactory.createTitledBorder(lowerEtchedBorder, " Variables "));

        this.add(jtextScrollPane);
        this.add(parameterTableScrollPane);
//        exportDetails = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, jtextScrollPane, parameterTableScrollPane);
        this.setBorder(BorderFactory.createTitledBorder(lowerEtchedBorder, " Export Details "));
        this.setResizeWeight(0.5);
        this.setContinuousLayout(true);

        MainPanel.setEnableParentAndChild(this, false);
//        return exportDetails;
    }

    public void resetExportDetails(){
        parameterTableModel.resetTableData();
        variableTextPanel.setText("");
        MainPanel.setEnableParentAndChild(this, false);
    }

    public void addExportDetailEntries(String variableText, ArrayList<String> parameters){
        variableTextPanel.setText(variableText);
        for(String parameterValues : parameters){
            String[] tokens = parameterValues.split(":");
            parameterTableModel.addRowData(tokens[0], tokens[1], tokens[2]);
        }
        variableTextPanel.updateUI();
        parameterTable.updateUI();
    }


    static class ParameterTableModel extends AbstractTableModel {

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
}
