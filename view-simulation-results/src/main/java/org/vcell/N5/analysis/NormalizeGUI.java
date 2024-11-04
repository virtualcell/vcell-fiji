package org.vcell.N5.analysis;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

class NormalizeGUI extends JPanel implements ActionListener {
    private final JTextField createNormFromImageStart;
    private final JTextField createNormFromImageEnd;
    private final JTextField createNormFromSimStart;
    private final JTextField createNormFromSimEnd;
    private final JPanel entireImageFramesJPanel;
    private final JCheckBox normalizeMeasurement = new JCheckBox("Normalize Measurement: ");

    public NormalizeGUI(JDialog jDialog){
        normalizeMeasurement.addActionListener(this);
        JLabel explainInput = new JLabel("Timeline Range to Create Norm");

        JPanel fromImage = new JPanel(new GridLayout());
        createNormFromImageStart = new JTextField();
        createNormFromImageEnd = new JTextField();
        fromImage.add(new JLabel("Exp. Timeline: "));
        fromImage.add(createNormFromImageStart);
        fromImage.add(new JLabel("to"));
        fromImage.add(createNormFromImageEnd);

        JPanel fromSim = new JPanel(new GridLayout());
        createNormFromSimStart = new JTextField();
        createNormFromSimEnd = new JTextField();
        fromSim.add(new JLabel("Sim Timeline: "));
        fromSim.add(createNormFromSimStart);
        fromSim.add(new JLabel("to"));
        fromSim.add(createNormFromSimEnd);

        entireImageFramesJPanel = new JPanel(new GridLayout(3, 1));
        entireImageFramesJPanel.add(explainInput);
        entireImageFramesJPanel.add(fromImage);
        entireImageFramesJPanel.add(fromSim);
        entireImageFramesJPanel.addComponentListener(new ComponentAdapter() {
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
        entireImageFramesJPanel.setVisible(false);

        this.add(normalizeMeasurement);
        this.add(entireImageFramesJPanel);
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

    public boolean performNormalization(){
        return normalizeMeasurement.isSelected();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(normalizeMeasurement)) {
            entireImageFramesJPanel.setVisible(normalizeMeasurement.isSelected());
        }
    }
}
