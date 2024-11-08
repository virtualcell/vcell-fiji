package org.vcell.N5.reduction;

import ij.gui.Roi;
import ij.io.RoiDecoder;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;

class RoiSelection extends JPanel {
    private final ROIDataModel imageTableModel = new ROIDataModel();
    private final ROIDataModel simTableModel = new ROIDataModel();

    public RoiSelection(){
        this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        JList<String> imageROITable = new JList<>(imageTableModel);
        JFileChooser imageROIFileChooser = new JFileChooser();
        this.add(createROIInput(imageROITable, imageTableModel, imageROIFileChooser, "Experimental"));

        JList<String> simROITable = new JList<>(simTableModel);
        JFileChooser simROIFileChooser = new JFileChooser();
        this.add(createROIInput(simROITable, simTableModel, simROIFileChooser, "Sim"));
        this.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "ROI Files"));
    }

    private JPanel createROIInput(JList<String> jList, ROIDataModel roiDataModel,
                                  JFileChooser fileChooser, String label){
        JPanel jPanel = new JPanel();
        jPanel.setLayout(new BoxLayout(jPanel, BoxLayout.Y_AXIS));
        jList.setVisibleRowCount(4);
        JScrollPane jScrollPane = new JScrollPane(jList);
        jPanel.add(jScrollPane);

        JButton addButton = new JButton("+");
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ArrayList<Roi> roiList = fillROIList(fileChooser);
                for (Roi roi : roiList){
                    roiDataModel.addRow(roi);
                }
                jList.updateUI();
            }
        });
        JButton minusButton = new JButton("-");
        minusButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                for (int roiIndex: jList.getSelectedIndices()){
                    roiDataModel.removeRow(roiIndex);
                }
                jList.updateUI();
            }
        });
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
        buttonPanel.add(addButton);
        buttonPanel.add(minusButton);
        jPanel.add(buttonPanel);
        jPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), label));

        return jPanel;
    }

    public ArrayList<Roi> getImageROIList(){
        return imageTableModel.data;
    }

    public ArrayList<Roi> getSimROIList(){
        return simTableModel.data;
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

    static class ROIDataModel extends AbstractListModel<String> {
        private final ArrayList<Roi> data = new ArrayList<>();
        public void addRow(Roi roi){
            data.add(roi);
        }
        public void removeRow(int index){data.remove(index);}

        @Override
        public int getSize() {
            return data.size();
        }

        @Override
        public String getElementAt(int index) {
            return data.get(index).getName();
        }
    }
}
