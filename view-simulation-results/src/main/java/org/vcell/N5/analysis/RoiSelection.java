package org.vcell.N5.analysis;

import ij.gui.Roi;
import ij.io.RoiDecoder;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;

class RoiSelection extends JPanel implements ActionListener {
    private final JFileChooser imageROIFileChooser = new JFileChooser();
    private final ROIDataModel imageTableModel = new ROIDataModel();
    private final JTable imageROITable = new JTable(imageTableModel);

    private final JFileChooser simROIFileChooser = new JFileChooser();
    private final ROIDataModel simTableModel = new ROIDataModel();
    private final JTable simROITable = new JTable(simTableModel);

    private final JButton imageROIFileButton;
    private final JButton simROIFileButton;

    private ArrayList<Roi> imageROIList;
    private ArrayList<Roi> simROIList;

    public RoiSelection(){
        JPanel roisForImage = new JPanel(new GridBagLayout());
        JPanel roisForSims = new JPanel(new GridBagLayout());
        Dimension tableDimensions = new Dimension(100, 70);
        imageROITable.getTableHeader().setBackground(Color.WHITE);
        imageROITable.setEnabled(false);
        simROITable.getTableHeader().setBackground(Color.WHITE);
        simROITable.setEnabled(false);

        imageROIFileButton = new JButton("ROI's For Image");
        JScrollPane displayImageROIList = new JScrollPane(imageROITable);
        displayImageROIList.setPreferredSize(tableDimensions);
        setROIPanelSettings(roisForImage, imageROIFileButton, displayImageROIList);

        simROIFileButton = new JButton("ROI's For Simulation");
        JScrollPane displaySimROIList = new JScrollPane(simROITable);
        displaySimROIList.setPreferredSize(tableDimensions);
        setROIPanelSettings(roisForSims, simROIFileButton, displaySimROIList);

        imageROIFileButton.addActionListener(this);
        simROIFileButton.addActionListener(this);

        this.setLayout(new BorderLayout());
        this.add(roisForImage, BorderLayout.WEST);
        this.add(roisForSims, BorderLayout.EAST);
    }

    public ArrayList<Roi> getImageROIList(){
        return imageROIList;
    }

    public ArrayList<Roi> getSimROIList(){
        return simROIList;
    }

    private void setROIPanelSettings(JPanel jPanel, JButton button, JScrollPane jScrollPane){
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        jPanel.add(button, gridBagConstraints);
        gridBagConstraints.gridy = 1;
        jPanel.add(jScrollPane, gridBagConstraints);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(imageROIFileButton)){
            imageROIList = fillROIList(imageROIFileChooser);
            imageTableModel.clear();
            for (Roi roi : imageROIList){
                imageTableModel.addRow(roi.getName());
            }
            imageROITable.updateUI();
        } else if (e.getSource().equals(simROIFileButton)) {
            simROIList = fillROIList(simROIFileChooser);
            simTableModel.clear();
            for (Roi roi : simROIList){
                simTableModel.addRow(roi.getName());
            }
            simROITable.updateUI();
        }
    }
    private ArrayList<Roi> fillROIList(JFileChooser fileChooser){
        fileChooser.setMultiSelectionEnabled(true);
        int choice = fileChooser.showDialog(this, "Open ROI's");
        ArrayList<Roi> roiList = new ArrayList<>();
        if (choice == JFileChooser.APPROVE_OPTION){
            for (File file: fileChooser.getSelectedFiles()){
                roiList.add(RoiDecoder.open(file.getAbsolutePath()));
            }
        }
        return roiList;
    }

    class ROIDataModel extends AbstractTableModel {
        private final ArrayList<String> data = new ArrayList<>();
        private final String[] headers = new String[]{"ROI's Selected"};
        @Override
        public int getRowCount() {
            return data.size();
        }

        @Override
        public int getColumnCount() {
            return headers.length;
        }

        @Override
        public String getColumnName(int column) {
            return headers[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            return data.get(rowIndex);
        }

        public void addRow(String roiName){
            data.add(roiName);
        }

        public void clear(){
            data.clear();
        }
    }
}
