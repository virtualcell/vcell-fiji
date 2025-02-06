package org.vcell.N5.reduction.GUI.ROIs;

import ij.gui.Roi;
import ij.io.RoiDecoder;
import org.vcell.N5.reduction.GUI.DataReductionGUI;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;

public class RoiSelection extends JPanel {
    private final ROIDataModel imageTableModel = new ROIDataModel();
    private final ROIDataModel simTableModel = new ROIDataModel();
    private final DataReductionGUI parentGUI;

    public RoiSelection(DataReductionGUI parentGUI){
        this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        JList<String> imageROITable = new JList<>(imageTableModel);
        JFileChooser imageROIFileChooser = new JFileChooser();
        this.add(createROIInput(imageROITable, imageTableModel, imageROIFileChooser, "<HTML><i>Experimental</i></HTML>"));

        JList<String> simROITable = new JList<>(simTableModel);
        JFileChooser simROIFileChooser = new JFileChooser();
        this.add(createROIInput(simROITable, simTableModel, simROIFileChooser, "<HTML><i>Simulation</i></HTML>"));
        Border border = new CompoundBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5),
        BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "2. Apply ROI"));
        this.setBorder(border);
        this.parentGUI = parentGUI;
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
                AvailableROIs availableROIs = new AvailableROIs();
                ArrayList<Roi> roiList = availableROIs.getSelectedRows();
                for (Roi roi : roiList){
                    roiDataModel.addRow(roi);
                }
                parentGUI.activateOkayButton();
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
                parentGUI.activateOkayButton();
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
