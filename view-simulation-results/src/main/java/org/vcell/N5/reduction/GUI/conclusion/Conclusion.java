package org.vcell.N5.reduction.GUI.conclusion;

import org.vcell.N5.reduction.GUI.DataReductionGUI;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class Conclusion extends JPanel implements ActionListener {
    public final SelectMeasurements selectMeasurements;
    public final SelectTableFormat selectTableFormat;
    public final SelectSimRange selectSimRange;

    private final JCheckBox selectRangeOfMeasurement = new JCheckBox("Select Measurement Range: ");
    private final JCheckBox normalizeMeasurement = new JCheckBox("Normalize Measurement: ");

    public Conclusion(DataReductionGUI parent, double simCSize, double simZSize, double simTSize){
        selectSimRange = new SelectSimRange(parent, simCSize, simZSize, simTSize);
        selectMeasurements = new SelectMeasurements(parent);
        selectTableFormat = new SelectTableFormat();

        add(selectMeasurements);
        add(selectTableFormat);
        add(displayOptionsPanel());
        add(selectSimRange);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        normalizeMeasurement.addActionListener(this);
        selectRangeOfMeasurement.addActionListener(this);
    }

    private JPanel displayOptionsPanel(){
        JPanel jPanel = new JPanel();
        jPanel.setLayout(new BoxLayout(jPanel, BoxLayout.X_AXIS));
//        jPanel.add(normalizeMeasurement);
        jPanel.add(selectRangeOfMeasurement);
        return jPanel;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(selectRangeOfMeasurement)){
            selectSimRange.setVisible(selectRangeOfMeasurement.isSelected());
        }
    }
}
