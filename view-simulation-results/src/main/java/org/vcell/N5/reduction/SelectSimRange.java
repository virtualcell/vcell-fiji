package org.vcell.N5.reduction;

import org.vcell.N5.UI.HintTextField;
import org.vcell.N5.reduction.DTO.RangeOfImage;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

class SelectSimRange extends JPanel {
    private final JTextField cStart = new HintTextField("1");
    private final JTextField cEnd;

    private final JTextField zStart = new HintTextField("1");
    private final JTextField zEnd;

    private final JTextField tStart = new HintTextField("1");
    private final JTextField tEnd;

    public SelectSimRange(JDialog jDialog, double simCSize, double simZSize, double simTSize){
        Border lowerEtchedBorder = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
        Border rangeBorder = BorderFactory.createTitledBorder(lowerEtchedBorder, "Range of Simulation to Perform Measurement");
        cEnd = new HintTextField(String.valueOf((int) simCSize - 1));
        zEnd = new HintTextField(String.valueOf((int) simZSize));
        tEnd = new HintTextField(String.valueOf((int) simTSize));

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

        JPanel rangeSelectionPanel = new JPanel(new GridLayout(4, 1));
        rangeSelectionPanel.add(cRange);
        rangeSelectionPanel.add(zRange);
        rangeSelectionPanel.add(tRange);

        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                SelectSimRange.this.revalidate();
                SelectSimRange.this.repaint();
                jDialog.revalidate();
                jDialog.repaint();
                jDialog.pack();
            }
            @Override
            public void componentHidden(ComponentEvent e) {
                SelectSimRange.this.revalidate();
                SelectSimRange.this.repaint();
                jDialog.revalidate();
                jDialog.repaint();
                jDialog.pack();
            }
        });
        this.add(rangeSelectionPanel);
        this.setBorder(rangeBorder);
        this.setVisible(false);
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
}
