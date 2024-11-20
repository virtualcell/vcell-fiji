package org.vcell.N5.reduction;

import javax.swing.*;
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
        this.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Measurement Type"));
        chosenMeasurement = new JList<>(measurementsDataModel);
        chosenMeasurement.setVisibleRowCount(3);
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
        STD_DEV("Standard Deviation");

        public final String publicName;
        AvailableMeasurements(String publicName){
            this.publicName = publicName;
        }
    }

    static class MeasurementsDataModel extends AbstractListModel<String>{
        private final AvailableMeasurements[] availableMeasurements = new AvailableMeasurements[]{AvailableMeasurements.AVERAGE, AvailableMeasurements.STD_DEV};

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
