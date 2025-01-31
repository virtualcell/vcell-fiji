package org.vcell.N5.reduction.GUI;

import ij.gui.Roi;
import ij.plugin.frame.RoiManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;

public class AvailableROIs extends JDialog implements ActionListener {
    private final JButton okayButton = new JButton("Okay");
    private final JButton cancelButton = new JButton("Cancel");
    private final ROIDataModel roiDataModel = new ROIDataModel();
    private final JTable table = new JTable(roiDataModel);
    private int[] selectedRows = new int[0];

    public AvailableROIs(){
        JScrollPane scrollPane = new JScrollPane(table);

        JPanel buttonPanel = new JPanel();
        okayButton.addActionListener(this);
        cancelButton.addActionListener(this);
        buttonPanel.add(okayButton);
        buttonPanel.add(cancelButton);

        this.setLayout(new BoxLayout(this.getContentPane(), BoxLayout.Y_AXIS));

        this.add(scrollPane);
        this.add(buttonPanel);
        this.setSize(300, 300);
        this.setModal(true);
        this.setVisible(true);


    }

    public ArrayList<Roi> getSelectedRows(){
        ArrayList<Roi> selectedRois = new ArrayList<>();
        for (int selectedRow : selectedRows){
            selectedRois.add(roiDataModel.rois.get(selectedRow));
        }
        return selectedRois;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(okayButton)){
            selectedRows = table.getSelectedRows();
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
