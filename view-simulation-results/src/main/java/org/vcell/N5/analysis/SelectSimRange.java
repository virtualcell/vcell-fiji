package org.vcell.N5.analysis;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

class SelectSimRange extends JPanel implements ActionListener {
    private final JCheckBox selectRangeOfMeasurement = new JCheckBox("Select Measurement Range: ");

    private final JTextField cStart = new JTextField();
    private final JTextField cEnd = new JTextField();

    private final JTextField zStart = new JTextField();
    private final JTextField zEnd = new JTextField();

    private final JTextField tStart = new JTextField();
    private final JTextField tEnd = new JTextField();

    private final JPanel rangeSelectionPanel;

    public SelectSimRange(JDialog jDialog){
        selectRangeOfMeasurement.addActionListener(this);
        JLabel explainInput = new JLabel("Range of Simulation to Perform Measurement");

        JPanel cRange = new JPanel(new GridLayout());
        cRange.add(new JLabel("Channel Range: "));
        cRange.add(cStart);
        cRange.add(new JLabel("to"));
        cRange.add(cEnd);

        JPanel zRange = new JPanel(new GridLayout());
        zRange.add(new JLabel("Z Range: "));
        zRange.add(zStart);
        zRange.add(new JLabel("to"));
        zRange.add(zEnd);

        JPanel tRange = new JPanel(new GridLayout());
        tRange.add(new JLabel("Time Range: "));
        tRange.add(tStart);
        tRange.add(new JLabel("to"));
        tRange.add(tEnd);

        rangeSelectionPanel = new JPanel(new GridLayout(4, 1));
        rangeSelectionPanel.add(explainInput);
        rangeSelectionPanel.add(cRange);
        rangeSelectionPanel.add(zRange);
        rangeSelectionPanel.add(tRange);
        rangeSelectionPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                jDialog.revalidate();
                jDialog.repaint();
                jDialog.pack();
            }
            @Override
            public void componentHidden(ComponentEvent e) {
                jDialog.revalidate();
                jDialog.repaint();
                jDialog.pack();
            }
        });
        rangeSelectionPanel.setVisible(false);

        this.add(selectRangeOfMeasurement);
        this.add(rangeSelectionPanel);
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    }

    public RangeOfImage getRangeOfSim(){
        RangeOfImage rangeOfImage = new RangeOfImage(
                Integer.parseInt(tStart.getText()), Integer.parseInt(tEnd.getText()),
                Integer.parseInt(zStart.getText()), Integer.parseInt(zEnd.getText()),
                Integer.parseInt(cStart.getText()), Integer.parseInt(cEnd.getText())
        );
        return rangeOfImage;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(selectRangeOfMeasurement)){
            rangeSelectionPanel.setVisible(selectRangeOfMeasurement.isSelected());
        }
    }

    public static class RangeOfImage{
        public final int timeStart;
        public final int timeEnd;
        public final int zStart;
        public final int zEnd;
        public final int channelStart;
        public final int channelEnd;
        public RangeOfImage(int timeStart, int timeEnd, int zStart, int zEnd, int channelStart, int channelEnd){
            this.timeStart = timeStart;
            this.timeEnd = timeEnd;
            this.zStart = zStart;
            this.zEnd = zEnd;
            this.channelStart = channelStart;
            this.channelEnd = channelEnd;
        }
    }
}
