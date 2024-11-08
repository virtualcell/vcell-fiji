package org.vcell.N5.reduction;

import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Roi;
import org.vcell.N5.UI.MainPanel;
import org.vcell.N5.reduction.DTO.RangeOfImage;
import org.vcell.N5.retrieving.SimResultsLoader;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;

public class DataReductionGUI extends JPanel implements ActionListener {
    private JComboBox<String> chosenImage;
    private final JCheckBox selectRangeOfMeasurement = new JCheckBox("Select Measurement Range: ");
    private final JCheckBox normalizeMeasurement = new JCheckBox("Normalize Measurement: ");


    private final JDialog jDialog;
    private final JOptionPane pane;
    private File chosenFile;

    private final ArrayList<SimResultsLoader> filesToOpen;

    public int mainGUIReturnValue;
    public int fileChooserReturnValue;

    private final SelectSimRange selectSimRange;
    private final RoiSelection roiSelection;
    private final NormalizeGUI normalizeGUI;
    private final SelectMeasurements selectMeasurements;

    private final Border lowerEtchedBorder = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);

    public DataReductionGUI(ArrayList<SimResultsLoader> filesToOpen, double simCSize, double simZSize, double simTSize){
         this.filesToOpen = filesToOpen;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        pane = new JOptionPane(this, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
        jDialog = pane.createDialog("Data Reduction");
        jDialog.setResizable(true);

        selectSimRange = new SelectSimRange(jDialog, simCSize, simZSize, simTSize);
        roiSelection = new RoiSelection();
        normalizeGUI = new NormalizeGUI(jDialog, simTSize);
        selectMeasurements = new SelectMeasurements();

        JPanel imagesToMeasurePanel = new JPanel();
        imagesToMeasurePanel.setLayout(new BoxLayout(imagesToMeasurePanel, BoxLayout.Y_AXIS));
        imagesToMeasurePanel.setBorder(BorderFactory.createTitledBorder(lowerEtchedBorder, "Images To Measure"));
        imagesToMeasurePanel.add(imageSelectionPanel());
        imagesToMeasurePanel.add(selectedImagesToOpenPanel());

        add(imagesToMeasurePanel);
        add(roiSelection);
        add(selectMeasurements);
        add(displayOptionsPanel());
        add(normalizeGUI);
        add(selectSimRange);
        setVisible(true);

        normalizeMeasurement.addActionListener(this);
        selectRangeOfMeasurement.addActionListener(this);

        jDialog.pack();
    }

    public DataReductionSubmission displayGUI(){
        jDialog.setVisible(true);
        mainGUIReturnValue = (Integer) pane.getValue();
        if (mainGUIReturnValue == JOptionPane.OK_OPTION){
            JFileChooser saveToFile = new JFileChooser();
            saveToFile.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooserReturnValue = saveToFile.showDialog(this, "Save Results To File");
            if (fileChooserReturnValue == JFileChooser.APPROVE_OPTION){
                chosenFile = saveToFile.getSelectedFile();
                MainPanel.controlButtonsPanel.enableCriticalButtons(false);
                return createSubmission();
            }
        }
        return null;
    }

    public DataReductionSubmission createSubmission(){
        int[] simRange = normalizeGUI.getSimTimRange();
        int[] labRange = normalizeGUI.getImageTimeRange();
        return new DataReductionSubmission(normalizeMeasurement.isSelected(),
                roiSelection.getSimROIList(), roiSelection.getImageROIList(),
                WindowManager.getImage((String) chosenImage.getSelectedItem()),
                simRange[0], simRange[1], labRange[0], labRange[1], filesToOpen.size(), chosenFile,
                selectSimRange.getRangeOfSim(), selectMeasurements.getChosenMeasurements());
    }

    private JPanel imageSelectionPanel(){
        JPanel jPanel = new JPanel(new GridLayout(1, 2));
        jPanel.add(new JLabel("Experimental"));
        chosenImage = new JComboBox<>(WindowManager.getImageTitles());
        jPanel.add(chosenImage);
        return jPanel;
    }

    private JPanel selectedImagesToOpenPanel(){
        JPanel jPanel = new JPanel();
        String[] namesOfImagesToOpen = new String[filesToOpen.size()];
        for (int i = 0; i < filesToOpen.size(); i++){
            namesOfImagesToOpen[i] = filesToOpen.get(i).userSetFileName;
        }
        JList<String> selectedImagesToOpen = new JList<>(namesOfImagesToOpen);
        selectedImagesToOpen.setEnabled(false);
        selectedImagesToOpen.setVisibleRowCount(4);
        JScrollPane jScrollPane = new JScrollPane(selectedImagesToOpen);
        jPanel.add(new JLabel("Selected Simulations"));
        jPanel.add(jScrollPane);
        jPanel.setLayout(new GridLayout(1, 2));
        return jPanel;
    }

    private JPanel displayOptionsPanel(){
        JPanel jPanel = new JPanel();
        jPanel.setLayout(new BoxLayout(jPanel, BoxLayout.X_AXIS));
        jPanel.add(normalizeMeasurement);
        jPanel.add(selectRangeOfMeasurement);
        return jPanel;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(normalizeMeasurement)) {
            normalizeGUI.setVisible(normalizeMeasurement.isSelected());
        } else if (e.getSource().equals(selectRangeOfMeasurement)){
            selectSimRange.setVisible(selectRangeOfMeasurement.isSelected());
        }
    }

    public static class DataReductionSubmission{
        public final boolean normalizeMeasurementsBool;
        public final ArrayList<Roi> arrayOfSimRois;
        public final ArrayList<Roi> arrayOfLabRois;
        public final ImagePlus labResults;
        public final int numOfSimImages;
        public final File fileToSaveResultsTo;

        public final RangeOfImage experiementNormRange;
        public final RangeOfImage simNormRange;

        public final RangeOfImage experimentImageRange;
        public final RangeOfImage simImageRange;
        public final ArrayList<SelectMeasurements.AvailableMeasurements> selectedMeasurements;

        public DataReductionSubmission(boolean normalizeMeasurementsBool, ArrayList<Roi> arrayOfSimRois, ArrayList<Roi> arrayOfLabRois,
                                       ImagePlus labResults, int simStartPointNorm, int simEndPointNorm, int imageStartPointNorm,
                                       int imageEndPointNorm, int numOfSimImages, File fileToSaveResultsTo,
                                       RangeOfImage simRange, ArrayList<SelectMeasurements.AvailableMeasurements> selectedMeasurements){
            this.normalizeMeasurementsBool = normalizeMeasurementsBool;
            this.arrayOfLabRois = arrayOfLabRois;
            this.arrayOfSimRois = arrayOfSimRois;
            this.labResults = labResults;
            this.numOfSimImages = numOfSimImages;
            this.fileToSaveResultsTo = fileToSaveResultsTo;

            this.experiementNormRange = new RangeOfImage(imageStartPointNorm, imageEndPointNorm);
            this.simNormRange = new RangeOfImage(simStartPointNorm, simEndPointNorm);

            this.experimentImageRange = new RangeOfImage(1, labResults.getNFrames(),
                    1, labResults.getNSlices(), 1, labResults.getNChannels());
            this.simImageRange = simRange;
            this.selectedMeasurements = selectedMeasurements;
        }
    }

    public static void main(String[] args) {
        DataReductionGUI dataReductionGUI = new DataReductionGUI(new ArrayList<>(), 0, 0, 0);
        dataReductionGUI.displayGUI();
    }
}








