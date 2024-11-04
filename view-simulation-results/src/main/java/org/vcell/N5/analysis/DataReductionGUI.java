package org.vcell.N5.analysis;

import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Roi;
import org.vcell.N5.N5ImageHandler;
import org.vcell.N5.UI.MainPanel;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;

public class DataReductionGUI extends JPanel {
    private JComboBox<String> chosenImage;

    private final JDialog jDialog;
    private final JOptionPane pane;
    private File chosenFile;

    private final int numSimsToOpen;

    public int mainGUIReturnValue;
    public int fileChooserReturnValue;

    private final SelectSimRange selectSimRange;
    private final RoiSelection roiSelection;
    private final NormalizeGUI normalizeGUI;

    public DataReductionGUI(int numSimsToOpen){
         this.numSimsToOpen = numSimsToOpen;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        pane = new JOptionPane(this, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
        jDialog = pane.createDialog("Data Reduction");
        jDialog.setResizable(true);

        selectSimRange = new SelectSimRange(jDialog);
        roiSelection = new RoiSelection();
        normalizeGUI = new NormalizeGUI(jDialog);

        add(imageSelectionPanel());
        add(new SelectMeasurements());
        add(roiSelection);
        add(normalizeGUI);
        add(selectSimRange);
        setVisible(true);

        jDialog.pack();
    }

    public void displayGUI(){
        jDialog.setVisible(true);
        mainGUIReturnValue = (Integer) pane.getValue();
        if (mainGUIReturnValue == JOptionPane.OK_OPTION){
            JFileChooser saveToFile = new JFileChooser();
            saveToFile.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooserReturnValue = saveToFile.showDialog(this, "Save Results To File");
            if (fileChooserReturnValue == JFileChooser.APPROVE_OPTION){
                chosenFile = saveToFile.getSelectedFile();
                MainPanel.controlButtonsPanel.enableCriticalButtons(false);
                Thread thread = new Thread(() -> {
                    DataReduction dataReduction = new DataReduction(createSubmission());
                    N5ImageHandler.loadingManager.addSimLoadingListener(dataReduction);
                });
                thread.start();
            }
        }
    }

    private DataReductionSubmission createSubmission(){
        int[] simRange = normalizeGUI.getSimTimRange();
        int[] labRange = normalizeGUI.getImageTimeRange();
        return new DataReductionSubmission(normalizeGUI.performNormalization(),
                roiSelection.getSimROIList(), roiSelection.getImageROIList(),
                WindowManager.getImage((String) chosenImage.getSelectedItem()),
                simRange[0], simRange[1], labRange[0], labRange[1], numSimsToOpen, chosenFile,
                selectSimRange.getRangeOfSim());
    }

    private JPanel imageSelectionPanel(){
        JPanel jPanel = new JPanel(new GridLayout(1, 2));
        jPanel.add(new JLabel("Select Experimental Image"));
        chosenImage = new JComboBox<>(WindowManager.getImageTitles());
        jPanel.add(chosenImage);
        return jPanel;
    }

    public static class DataReductionSubmission{
        public final boolean normalizeMeasurementsBool;
        public final ArrayList<Roi> arrayOfSimRois;
        public final ArrayList<Roi> arrayOfLabRois;
        public final ImagePlus labResults;
        public final int simStartPointNorm;
        public final int simEndPointNorm;
        public final int imageStartPointNorm;
        public final int imageEndPointNorm;
        public final int numOfSimImages;
        public final File fileToSaveResultsTo;
        public final SelectSimRange.RangeOfImage experimentImageRange;
        public final SelectSimRange.RangeOfImage simImageRange;

        public DataReductionSubmission(boolean normalizeMeasurementsBool,ArrayList<Roi> arrayOfSimRois, ArrayList<Roi> arrayOfLabRois,
                                       ImagePlus labResults, int numOfSimImages, File fileToSaveResultsTo){
            this(normalizeMeasurementsBool, arrayOfSimRois, arrayOfLabRois, labResults,
                    0,0,0,0, numOfSimImages, fileToSaveResultsTo);
        }

        public DataReductionSubmission(boolean normalizeMeasurementsBool, ArrayList<Roi> arrayOfSimRois, ArrayList<Roi> arrayOfLabRois,
                                       ImagePlus labResults, int simStartPointNorm, int simEndPointNorm, int imageStartPointNorm,
                                       int imageEndPointNorm, int numOfSimImages, File fileToSaveResultsTo){
            this(normalizeMeasurementsBool, arrayOfSimRois, arrayOfLabRois, labResults, simStartPointNorm, simEndPointNorm, imageStartPointNorm, imageEndPointNorm,
                    numOfSimImages, fileToSaveResultsTo,
                    new SelectSimRange.RangeOfImage(1, labResults.getNFrames(),
                            1, labResults.getNSlices(), 1, labResults.getNChannels()));
        }

        public DataReductionSubmission(boolean normalizeMeasurementsBool, ArrayList<Roi> arrayOfSimRois, ArrayList<Roi> arrayOfLabRois,
                                       ImagePlus labResults, int simStartPointNorm, int simEndPointNorm, int imageStartPointNorm,
                                       int imageEndPointNorm, int numOfSimImages, File fileToSaveResultsTo,
                                       SelectSimRange.RangeOfImage simRange){
            this.normalizeMeasurementsBool = normalizeMeasurementsBool;
            this.arrayOfLabRois = arrayOfLabRois;
            this.arrayOfSimRois = arrayOfSimRois;
            this.labResults = labResults;
            this.simStartPointNorm = simStartPointNorm;
            this.simEndPointNorm = simEndPointNorm;
            this.imageStartPointNorm = imageStartPointNorm;
            this.imageEndPointNorm = imageEndPointNorm;
            this.numOfSimImages = numOfSimImages;
            this.fileToSaveResultsTo = fileToSaveResultsTo;
            this.experimentImageRange = new SelectSimRange.RangeOfImage(1, labResults.getNFrames(),
                    1, labResults.getNSlices(), 1, labResults.getNChannels());
            this.simImageRange = simRange;
        }
    }

    public static void main(String[] args) {
        DataReductionGUI dataReductionGUI = new DataReductionGUI(0);
        dataReductionGUI.displayGUI();
    }
}








