package org.vcell.N5.reduction;

import org.vcell.N5.UI.HintTextField;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

class NormalizeGUI extends JPanel {
    private final JTextField createNormFromImageStart;
    private final JTextField createNormFromImageEnd;
    private final JTextField createNormFromSimStart;
    private final JTextField createNormFromSimEnd;
    private final JPanel entireImageFramesJPanel;

    public NormalizeGUI(JDialog jDialog, double simTSize){
        Border lowerEtchedBorder = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
        Border normalizeBorder = BorderFactory.createTitledBorder(lowerEtchedBorder, "Timeline Range to Create Norm");

        JPanel fromImage = new JPanel(new GridLayout());
        createNormFromImageStart = new JTextField();
        createNormFromImageEnd = new JTextField();
        fromImage.add(new JLabel("Exp. Timeline: "));
        fromImage.add(createNormFromImageStart);
        fromImage.add(new JLabel("to"));
        fromImage.add(createNormFromImageEnd);

        JPanel fromSim = new JPanel(new GridLayout());
        createNormFromSimStart = new HintTextField("1");
        createNormFromSimEnd = new HintTextField(String.valueOf((int) simTSize));
        fromSim.add(new JLabel("Sim Timeline: "));
        fromSim.add(createNormFromSimStart);
        fromSim.add(new JLabel("to"));
        fromSim.add(createNormFromSimEnd);

        entireImageFramesJPanel = new JPanel(new GridLayout(3, 1));
        entireImageFramesJPanel.add(fromImage);
        entireImageFramesJPanel.add(fromSim);
        this.addComponentListener(new ComponentAdapter() {
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
        this.add(entireImageFramesJPanel);
        this.setBorder(normalizeBorder);
        this.setVisible(false);
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    }

    public int[] getSimTimRange(){
        return new int[]{
                createNormFromSimStart.getText().isEmpty() ? Integer.MIN_VALUE : Integer.parseInt(createNormFromSimStart.getText()),
                createNormFromSimEnd.getText().isEmpty() ? Integer.MIN_VALUE: Integer.parseInt(createNormFromSimEnd.getText())
        };
    }

    public int[] getImageTimeRange(){
        return new int[]{
                createNormFromImageStart.getText().isEmpty() ? Integer.MIN_VALUE: Integer.parseInt(createNormFromImageStart.getText()),
                createNormFromImageEnd.getText().isEmpty() ? Integer.MIN_VALUE: Integer.parseInt(createNormFromImageEnd.getText())
        };
    }
}
