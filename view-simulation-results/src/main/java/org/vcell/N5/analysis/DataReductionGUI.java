package org.vcell.N5.analysis;

import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Roi;
import ij.io.RoiDecoder;
import org.vcell.N5.N5ImageHandler;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;

public class DataReductionGUI extends JPanel implements ActionListener {
    private JComboBox<String> chosenImage;
    private JComboBox<String> chosenMeasurement;
    private final JCheckBox normalizeMeasurement = new JCheckBox("Normalize Measurement: ");

    private JTextField createNormFromImageStart;
    private JTextField createNormFromImageEnd;
    private JTextField createNormFromSimStart;
    private JTextField createNormFromSimEnd;
    private JPanel entireImageFramesJPanel;

    private JButton imageROIFileButton;
    private JButton simROIFileButton;

    private String notInMemoryWarning;

    private final JFileChooser imageROIFileChooser = new JFileChooser();
    private final JFileChooser simROIFileChooser = new JFileChooser();

    private ArrayList<Roi> imageROIList;
    private ArrayList<Roi> simROIList;

    private final JDialog jDialog;
    private final JOptionPane pane;
    private File chosenFile;

    private int numSimsToOpen;

    public int mainGUIReturnValue;
    public int fileChooserReturnValue;

//    private TemporalAnalysis temporalAnalysis = new TemporalAnalysis();

     public class DataReductionSubmission{
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
        public DataReductionSubmission(){
            normalizeMeasurementsBool = normalizeMeasurement.isSelected();
            arrayOfSimRois = simROIList;
            arrayOfLabRois = imageROIList;
            labResults = WindowManager.getImage((String) chosenImage.getSelectedItem());
            simStartPointNorm = createNormFromSimStart.getText().isEmpty() ? Integer.MIN_VALUE : Integer.parseInt(createNormFromSimStart.getText());
            simEndPointNorm = createNormFromSimEnd.getText().isEmpty() ? Integer.MIN_VALUE: Integer.parseInt(createNormFromSimEnd.getText());
            imageStartPointNorm = createNormFromImageStart.getText().isEmpty() ? Integer.MIN_VALUE: Integer.parseInt(createNormFromImageStart.getText());
            imageEndPointNorm = createNormFromImageEnd.getText().isEmpty() ? Integer.MIN_VALUE: Integer.parseInt(createNormFromImageEnd.getText());
            numOfSimImages = numSimsToOpen;
            fileToSaveResultsTo = chosenFile;
        }
    }

    public DataReductionGUI(int numSimsToOpen){
         this.numSimsToOpen = numSimsToOpen;
        setLayout(new GridLayout(4, 1));

        add(imageAndAnalysisType());
        add(roisSelectedGUI());
        add(normalizeGUI());
        setSize(400, 400);
        setVisible(true);

        pane = new JOptionPane(this, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
        jDialog = pane.createDialog("Data Reduction");
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
                Thread thread = new Thread(() -> {
                    DataReduction dataReduction = new DataReduction(new DataReductionSubmission());
                    N5ImageHandler.loadingManager.addSimLoadingListener(dataReduction);
                });
                thread.start();
            }
        }
    }

    private JPanel normalizeGUI(){
        JPanel jPanel = new JPanel(new GridLayout(2, 1));
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

        entireImageFramesJPanel = new JPanel(new GridLayout(2, 1));
        entireImageFramesJPanel.add(fromImage);
        entireImageFramesJPanel.add(fromSim);
        entireImageFramesJPanel.setVisible(false);

        jPanel.add(normalizeMeasurement);
        jPanel.add(entireImageFramesJPanel);
        return jPanel;
    }

    private JPanel roisSelectedGUI(){
        JPanel jPanel = new JPanel(new BorderLayout());
        imageROIFileButton = new JButton("ROI's For Image");
        simROIFileButton = new JButton("ROI's For Simulation");

        imageROIFileButton.addActionListener(this);
        simROIFileButton.addActionListener(this);
        
        jPanel.add(imageROIFileButton, BorderLayout.WEST);
        jPanel.add(simROIFileButton, BorderLayout.EAST);
        return jPanel;
    }


    private JPanel imageAndAnalysisType(){
        JPanel jPanel = new JPanel(new BorderLayout());
        chosenImage = new JComboBox<>(WindowManager.getImageTitles());
        chosenMeasurement = new JComboBox<>(new String[]{AvailableMeasurements.MEDIAN.publicName});

        chosenMeasurement.addActionListener(this);
        chosenImage.addActionListener(this);
        jPanel.add(chosenImage, BorderLayout.NORTH);
        jPanel.add(chosenMeasurement, BorderLayout.SOUTH);

        return jPanel;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(imageROIFileButton)){
            imageROIList = fillROIList(imageROIFileChooser);
        } else if (e.getSource().equals(simROIFileButton)) {
            simROIList = fillROIList(simROIFileChooser);
        } else if (e.getSource().equals(normalizeMeasurement)) {
            entireImageFramesJPanel.setVisible(normalizeMeasurement.isSelected());
        }
    }

    private ArrayList<Roi> fillROIList(JFileChooser fileChooser){
        fileChooser.setMultiSelectionEnabled(true);
        int choice = fileChooser.showDialog(this, "");
        ArrayList<Roi> roiList = new ArrayList<>();
        if (choice == JFileChooser.APPROVE_OPTION){
            for (File file: fileChooser.getSelectedFiles()){
                roiList.add(RoiDecoder.open(file.getAbsolutePath()));
            }
        }
        return roiList;
    }

    enum AvailableMeasurements{
        MEDIAN("Median");

        public final String publicName;
        AvailableMeasurements(String publicName){
            this.publicName = publicName;
        }
    }

    public static void main(String[] args) {
        DataReductionGUI dataReductionGUI = new DataReductionGUI(0);
        dataReductionGUI.displayGUI();
    }
}








