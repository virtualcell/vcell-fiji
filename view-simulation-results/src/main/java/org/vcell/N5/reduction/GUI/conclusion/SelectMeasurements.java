package org.vcell.N5.reduction.GUI.conclusion;

import org.vcell.N5.reduction.GUI.DataReductionGUI;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.util.ArrayList;

public class SelectMeasurements extends JPanel implements ListSelectionListener {
    private final JList<String> chosenMeasurement;
    private final MeasurementsDataModel measurementsDataModel = new MeasurementsDataModel();
    private final DataReductionGUI parentGUI;

    public SelectMeasurements(DataReductionGUI parentGUI){
        setLayout(new GridLayout(1, 1));
        Border border = new CompoundBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5),
                BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "3. Measurement Type"));
        this.setBorder(border);
        chosenMeasurement = new JList<>(measurementsDataModel);
        chosenMeasurement.setVisibleRowCount(4);
        chosenMeasurement.addListSelectionListener(this);
        JScrollPane jScrollPane = new JScrollPane(chosenMeasurement);
        this.add(jScrollPane);
        this.parentGUI = parentGUI;
    }

    public ArrayList<AvailableMeasurements> getChosenMeasurements(){
        return measurementsDataModel.getSelectedMeasurements(chosenMeasurement.getSelectedIndices());
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        if (e.getSource().equals(chosenMeasurement)){
            parentGUI.activateOkayButton();
        }
    }

    public enum AvailableMeasurements{
        AVERAGE("Average"),
        STD_DEV("Standard Deviation"),
        MAX_INTENSITY("Max Intensity"),
        MIN_INTENSITY("Minimum Intensity");

        public final String publicName;
        AvailableMeasurements(String publicName){
            this.publicName = publicName;
        }
    }

    static class MeasurementsDataModel extends AbstractListModel<String>{
        private final AvailableMeasurements[] availableMeasurements = new AvailableMeasurements[]{
                AvailableMeasurements.AVERAGE, AvailableMeasurements.STD_DEV, AvailableMeasurements.MAX_INTENSITY, AvailableMeasurements.MIN_INTENSITY};

        public ArrayList<AvailableMeasurements> getSelectedMeasurements(int[] indices){
            ArrayList<AvailableMeasurements> selected = new ArrayList<>();
            for (int i : indices){
                selected.add(availableMeasurements[i]);
            }
            return selected;
        }

        @Override
        public int getSize() {
            return availableMeasurements.length;
        }

        @Override
        public String getElementAt(int index) {
            return availableMeasurements[index].publicName;
        }
    }

}
