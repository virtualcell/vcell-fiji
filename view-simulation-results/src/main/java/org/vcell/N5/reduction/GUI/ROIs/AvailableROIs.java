package org.vcell.N5.reduction.GUI.ROIs;

import ij.gui.Roi;
import ij.io.RoiDecoder;
import ij.plugin.frame.RoiManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class AvailableROIs extends JDialog implements ActionListener {
    private final JButton okayButton = new JButton("Okay");
    private final JButton cancelButton = new JButton("Cancel");
    private final ROIDataModel roiDataModel = new ROIDataModel();
    private final JTable table = new JTable(roiDataModel);
    private final ArrayList<Roi> selectedROIs = new ArrayList<>();

    public AvailableROIs(){
        JScrollPane scrollPane = new JScrollPane(table);

        JPanel buttonPanel = new JPanel();
        okayButton.addActionListener(this);
        cancelButton.addActionListener(this);
        buttonPanel.add(okayButton);
        buttonPanel.add(cancelButton);

        this.setLayout(new BoxLayout(this.getContentPane(), BoxLayout.Y_AXIS));

        if (roiDataModel.getRowCount() == 0){
            String message = "There seems to be no ROI's present in ImageJ's ROI manager.\nWould you rather open your ROI directly from the file system?";
            int confirm = JOptionPane.showConfirmDialog(this, message, "No ROI's In ROI Manager", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.OK_OPTION){
                JFileChooser fileChooser = new JFileChooser();
                fillROIList(fileChooser);
            }
        } else{
            this.add(scrollPane);
            this.add(buttonPanel);
            this.setSize(300, 300);
            this.setModal(true);
            this.setVisible(true);
        }
    }

    private void fillROIList(JFileChooser fileChooser){
        fileChooser.setMultiSelectionEnabled(true);
        int choice = fileChooser.showDialog(this, "Open ROI's");
        if (choice == JFileChooser.APPROVE_OPTION){
            for (File file: fileChooser.getSelectedFiles()){
                selectedROIs.add(RoiDecoder.open(file.getAbsolutePath()));
            }
        }
    }

    public ArrayList<Roi> getSelectedRows(){
        return selectedROIs;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(okayButton)){
            int[] selectedRows = table.getSelectedRows();
            for (int selectedRow : selectedRows){
                selectedROIs.add(roiDataModel.rois.get(selectedRow));
            }
            this.dispose();
        } else if (e.getSource().equals(cancelButton)){
            this.dispose();
        }
    }

    private static class ROIDataModel extends DefaultTableModel{
        private final ArrayList<Roi> rois;
        public ROIDataModel(){
            rois = new ArrayList<>();
            rois.addAll(Arrays.asList(RoiManager.getRoiManager().getRoisAsArray()));
        }

        @Override
        public int getRowCount() {
            if (rois == null){
                return 0;
            }
            return rois.size();
        }

        @Override
        public String getColumnName(int column) {
            return "Available ROI's From ROI Manager";
        }

        @Override
        public int getColumnCount() {
            return 1;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            return rois.get(rowIndex).getName();
        }
    }

}
