package org.vcell.N5.UI;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class RangeSelector extends JDialog implements ActionListener {
    public int startC;
    public int endC;
    public int startT;
    public int endT;
    public int startZ;
    public int endZ;

    private JTextField channelStartTextField;
    private JTextField channelEndTextField;
    private JTextField timeStartTextField;
    private JTextField timeEndTextField;
    private JTextField zStartTextField;
    private JTextField zEndTextField;

    public boolean cancel;

    private static final String okayButtonText = "Okay";
    private static final String cancelButtonText = "Cancel";
    public JButton okayButton;
    public JButton cancelButton;
    private JFrame frame;

    private static final EventListenerList eventListenerList = new EventListenerList();

    private final ControlButtonsPanel controlButtonsPanel = MainPanel.controlButtonsPanel;

    public RangeSelector(){

    }

    public void displayRangeMenu(double cDim, double zDim, double tDim){
        channelEndTextField = new HintTextField("" + (int) cDim);
        zEndTextField = new HintTextField("" + (int) zDim);
        timeEndTextField = new HintTextField("" + (int) tDim);
        String userSetFileName = "Range for All Images Used";
        channelStartTextField = new HintTextField("1");

        zStartTextField = new HintTextField("1");

        timeStartTextField = new HintTextField("1");

        // Create the frame
        frame = new JFrame("Select " + userSetFileName + " Dimensions");
        this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        this.setSize(400, 200);
        this.setTitle("Select " + userSetFileName + " Dimensions");

        // Create a panel to hold the input boxes and buttons
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(4, 4, 10, 10)); // 4 rows: 3 for input, 1 for buttons

        panel.add(new JLabel("Channel Start: "));
        panel.add(channelStartTextField);
        panel.add(new JLabel("Channel End: "));
        panel.add(channelEndTextField);

        panel.add(new JLabel("Z-Slice Start: "));
        panel.add(zStartTextField);
        panel.add(new JLabel("Z-Slice End: "));
        panel.add(zEndTextField);

        panel.add(new JLabel("Time Start: "));
        panel.add(timeStartTextField);
        panel.add(new JLabel("Time End: "));
        panel.add(timeEndTextField);


        // Create the "Okay" and "Cancel" buttons
        panel.add(new JLabel());
        okayButton = new JButton(okayButtonText);
        cancelButton = new JButton(cancelButtonText);

        // Add action listeners to the buttons
        okayButton.addActionListener(this);

        cancelButton.addActionListener(this);

        // Add the buttons to the panel
        panel.add(okayButton);
        panel.add(cancelButton);

        // Add the panel to the frame
        frame.add(panel);
        this.setContentPane(panel);
        this.setModal(true);
        // Make the window visible
        this.setVisible(true);
    }

    public static void main(String[] args) {
        RangeSelector inMemoryPopUp = new RangeSelector();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(okayButton)){
            startC = Integer.parseInt(channelStartTextField.getText());
            endC = Integer.parseInt(channelEndTextField.getText());
            startT = Integer.parseInt(timeStartTextField.getText()) - 1;
            endT = Integer.parseInt(timeEndTextField.getText()) - 1;
            startZ = Integer.parseInt(zStartTextField.getText()) - 1;
            endZ = Integer.parseInt(zEndTextField.getText()) - 1;

            cancel = false;

            this.setVisible(false);
            this.setModal(false);
        }

        else if (e.getSource().equals(cancelButton)) {
            MainPanel.changeCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            controlButtonsPanel.enableAllButtons(true);
            cancel = true;
            this.setVisible(false);
        }
    }
}
