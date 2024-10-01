package org.vcell.N5.UI;

import ij.ImagePlus;
import ij.plugin.Duplicator;
import org.scijava.log.Logger;
import org.vcell.N5.N5ImageHandler;
import org.vcell.N5.retrieving.SimLoadingEventCreator;
import org.vcell.N5.retrieving.SimLoadingListener;
import org.vcell.N5.retrieving.SimResultsLoader;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.IOException;

public class ImageIntoMemory implements ActionListener, SimLoadingEventCreator {
    public int startC;
    public int endC;
    public int startT;
    public int endT;
    public int startZ;
    public int endZ;

    private final JTextField channelStartTextField;
    private final JTextField channelEndTextField;
    private final JTextField timeStartTextField;
    private final JTextField timeEndTextField;
    private final JTextField zStartTextField;
    private final JTextField zEndTextField;

    public static final String okayButtonText = "Okay";
    public static final String cancelButtonText = "Cancel";
    public final JButton okayButton;
    public final JButton cancelButton;
    private final JFrame frame;
    private final SimResultsLoader simResultsLoader;

    private static final Logger logger = N5ImageHandler.getLogger(ImageIntoMemory.class);
    private static final EventListenerList eventListenerList = new EventListenerList();

    public ImageIntoMemory(double cDim, double zDim, double tDim, SimResultsLoader simResultsLoader){
        this.simResultsLoader = simResultsLoader;
        channelStartTextField = new HintTextField("1");
        channelEndTextField = new HintTextField("" + (int) cDim);

        zStartTextField = new HintTextField("1");
        zEndTextField = new HintTextField("" + (int) zDim);

        timeStartTextField = new HintTextField("1");
        timeEndTextField = new HintTextField("" + (int) tDim);

        // Create the frame
        frame = new JFrame("Select " + simResultsLoader.userSetFileName + " Dimensions");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 200);

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
    }

    public void dispose(){
        frame.dispose();
    }

    public void displayRangeMenu(){
        // Make the window visible
        frame.setVisible(true);
    }

    public static void usePopUp(){

    }

    public static void useExistingParameters(int startC, int endC, int startT, int endT, int startZ, int endZ){

    }

    public static void main(String[] args) {
        ImageIntoMemory inMemoryPopUp = new ImageIntoMemory(1, 2, 3, null);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(okayButton)){
            frame.dispose();

            startC = Integer.parseInt(channelStartTextField.getText());
            endC = Integer.parseInt(channelEndTextField.getText());
            startT = Integer.parseInt(timeStartTextField.getText()) - 1;
            endT = Integer.parseInt(timeEndTextField.getText()) - 1;
            startZ = Integer.parseInt(zStartTextField.getText()) - 1;
            endZ = Integer.parseInt(zEndTextField.getText()) - 1;

            Thread openInMemory = new Thread(() -> {
                try {
                    ImagePlus imagePlus = simResultsLoader.getImgPlusFromN5File();
                    long start = System.currentTimeMillis();
                    logger.debug("Loading Virtual N5 File " + simResultsLoader.userSetFileName + " Into Memory");
                    imagePlus = new Duplicator().run(imagePlus, startC, endC, startZ,
                            endZ, startT, endT);
                    long end = System.currentTimeMillis();
                    logger.debug("Loaded Virtual N5 File " + simResultsLoader.userSetFileName + " Into Memory taking: " + ((end - start)/ 1000) + "s");
                    imagePlus.show();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                } finally {
                    notifySimIsDoneLoading(simResultsLoader);
                    N5ExportTable.exportTableDialog.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                    N5ExportTable.enableCriticalButtons(true);
                }
            });
            openInMemory.setName("Open N5 Image in Memory");
            openInMemory.start();
        }

        else if (e.getSource().equals(cancelButton)) {
            notifySimIsDoneLoading(simResultsLoader);
            N5ExportTable.exportTableDialog.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            N5ExportTable.enableCriticalButtons(true);
            frame.dispose();
        }
    }

    @Override
    public void addSimLoadingListener(SimLoadingListener simLoadingListener) {
        eventListenerList.add(SimLoadingListener.class, simLoadingListener);
    }

    @Override
    public void notifySimIsLoading(SimResultsLoader simResultsLoader) {
        for (SimLoadingListener simLoadingListener: eventListenerList.getListeners(SimLoadingListener.class)){
            simLoadingListener.simIsLoading(simResultsLoader.rowNumber, simResultsLoader.exportID);
        }
    }

    @Override
    public void notifySimIsDoneLoading(SimResultsLoader simResultsLoader) {
        for (SimLoadingListener simLoadingListener: eventListenerList.getListeners(SimLoadingListener.class)){
            simLoadingListener.simFinishedLoading(simResultsLoader.rowNumber, simResultsLoader.exportID);
        }
    }

    static class HintTextField extends JTextField {

        Font gainFont = new Font("Tahoma", Font.PLAIN, 11);
        Font lostFont = new Font("Tahoma", Font.ITALIC, 11);

        public HintTextField(final String hint) {

            setText(hint);
            setFont(lostFont);
            setForeground(Color.GRAY);

            this.addFocusListener(new FocusAdapter() {

                @Override
                public void focusGained(FocusEvent e) {
                    if (getText().equals(hint)) {
                        setText("");
                        setFont(gainFont);
                    } else {
                        setText(getText());
                        setFont(gainFont);
                    }
                }

                @Override
                public void focusLost(FocusEvent e) {
                    if (getText().equals(hint)|| getText().isEmpty()) {
                        setText(hint);
                        setFont(lostFont);
                        setForeground(Color.GRAY);
                    } else {
                        setText(getText());
                        setFont(gainFont);
                        setForeground(Color.BLACK);
                    }
                }
            });

        }
    }
}
