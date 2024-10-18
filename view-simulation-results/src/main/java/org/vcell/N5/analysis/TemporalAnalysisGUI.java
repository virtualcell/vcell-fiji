package org.vcell.N5.analysis;

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

public class TemporalAnalysisGUI extends JPanel implements ActionListener {
    private JComboBox<String> chosenImage;
    private JComboBox<String> chosenMeasurement;
    private JCheckBox normalizeEntireImage = new JCheckBox("Normalize Entire Image");
    private JCheckBox normalizeROI = new JCheckBox("Normalize ROI");

    private JTextField entireImageFramesFromImageStart;
    private JTextField entireImageFramesFromImageEnd;
    private JTextField entireImageFramesFromSimStart;
    private JTextField entireImageFramesFromSimEnd;
    private JPanel entireImageFramesJPanel;

    private JTextField roiFramesFromImageStart;
    private JTextField roiFramesFromImageEnd;
    private JTextField roiFramesFromSimStart;
    private JTextField roiFramesFromSimEnd;
    private JPanel roiFramesJPanel;

    private JButton imageROIFileButton;
    private JButton simROIFileButton;

    private String notInMemoryWarning;

    private final JFileChooser imageROIFileChooser = new JFileChooser();
    private final JFileChooser simROIFileChooser = new JFileChooser();

    private ArrayList<Roi> imageROIList;
    private ArrayList<Roi> simROIList;

    private final JDialog jDialog;
    private final JOptionPane pane;

//    private TemporalAnalysis temporalAnalysis = new TemporalAnalysis();

    public TemporalAnalysisGUI(){
        setLayout(new GridLayout(4, 1));

        add(imageAndAnalysisType());
        add(roisSelectedGUI());
        add(normalizeEntireImageGUI());
        add(normalizeROIGUI());
        setSize(400, 400);
        setVisible(true);

        pane = new JOptionPane(this, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
        jDialog = pane.createDialog("Data Reduction");
    }

    public void displayGUI(){
        jDialog.setVisible(true);
        Integer returnValue = (Integer) pane.getValue();
        if (returnValue.equals(JOptionPane.OK_OPTION)){
            JFileChooser saveToFile = new JFileChooser();
            saveToFile.setFileSelectionMode(JFileChooser.FILES_ONLY);
            int response = saveToFile.showDialog(this, "Save Results To File");
            if (response == JFileChooser.APPROVE_OPTION){
                Thread thread = new Thread(() -> {
                    DataReduction dataReduction = new DataReduction(
                            normalizeROI.isSelected(), normalizeEntireImage.isSelected(), simROIList,
                            imageROIList, WindowManager.getImage((String) chosenImage.getSelectedItem()),
                            0, 0, 0,
                            5, saveToFile.getSelectedFile()
                    );
                    N5ImageHandler.loadingManager.addSimLoadingListener(dataReduction);
                });
                thread.start();
            }
        }
    }

    private JPanel normalizeROIGUI(){
        JPanel jPanel = new JPanel(new GridLayout());
        normalizeROI.addActionListener(this);

        JPanel fromImage = new JPanel(new GridLayout());
        roiFramesFromImageStart = new JTextField();
        roiFramesFromImageEnd = new JTextField();
        fromImage.add(roiFramesFromImageStart);
        fromImage.add(new JLabel("to"));
        fromImage.add(roiFramesFromImageEnd);

        JPanel fromSim = new JPanel(new GridLayout());
        roiFramesFromSimStart = new JTextField();
        roiFramesFromSimEnd = new JTextField();
        fromSim.add(roiFramesFromSimStart);
        fromSim.add(new JLabel("to"));
        fromSim.add(roiFramesFromSimEnd);

        roiFramesJPanel = new JPanel(new GridLayout());
        roiFramesJPanel.add(fromImage);
        roiFramesJPanel.add(fromSim);
        roiFramesJPanel.setVisible(false);

        jPanel.add(normalizeROI);
        jPanel.add(roiFramesJPanel);

        return jPanel;
    }

    private JPanel normalizeEntireImageGUI(){
        JPanel jPanel = new JPanel(new GridLayout());
        normalizeEntireImage.addActionListener(this);

        JPanel fromImage = new JPanel(new GridLayout());
        entireImageFramesFromImageStart = new JTextField();
        entireImageFramesFromImageEnd = new JTextField();
        fromImage.add(entireImageFramesFromImageStart);
        fromImage.add(new JLabel("to"));
        fromImage.add(entireImageFramesFromImageEnd);

        JPanel fromSim = new JPanel(new GridLayout());
        entireImageFramesFromSimStart = new JTextField();
        entireImageFramesFromSimEnd = new JTextField();
        fromSim.add(entireImageFramesFromSimStart);
        fromSim.add(new JLabel("to"));
        fromSim.add(entireImageFramesFromSimEnd);

        entireImageFramesJPanel = new JPanel(new GridLayout());
        entireImageFramesJPanel.add(fromImage);
        entireImageFramesJPanel.add(fromSim);
        entireImageFramesJPanel.setVisible(false);

        jPanel.add(normalizeEntireImage);
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
        } else if (e.getSource().equals(normalizeEntireImage)) {
            entireImageFramesJPanel.setVisible(normalizeEntireImage.isSelected());
        } else if (e.getSource().equals(normalizeROI)) {
            roiFramesJPanel.setVisible(normalizeROI.isSelected());
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
        TemporalAnalysisGUI temporalAnalysisGUI = new TemporalAnalysisGUI();
        temporalAnalysisGUI.displayGUI();
    }
}








