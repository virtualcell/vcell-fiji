package org.vcell.N5.reduction.GUI;

import org.vcell.N5.UI.ControlButtonsPanel;
import org.vcell.N5.UI.MainPanel;
import org.vcell.N5.reduction.DTO.DataReductionSubmission;
import org.vcell.N5.reduction.GUI.ROIs.RoiSelection;
import org.vcell.N5.reduction.GUI.conclusion.Conclusion;
import org.vcell.N5.reduction.GUI.images.ImagesToMeasure;
import org.vcell.N5.retrieving.SimResultsLoader;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;

public class DataReductionGUI extends JPanel implements ActionListener {
    private final JDialog jDialog = new CustomDialog(MainPanel.exportTableDialog, true);
    private final JButton okayButton = new JButton("Next");
    private final JButton backButton = new JButton("Back");

    private File resultSaveFile;

    private final ArrayList<SimResultsLoader> simsToOpen;
    private final ArrayList<JPanel> panels = new ArrayList<>();

    private final JPanel switchPanel = new JPanel();
    private final RoiSelection roiSelection;
    private final ImagesToMeasure imagesToMeasurePanel;
    private final NormalizeGUI normalizeGUI;
    private final Conclusion conclusion;

    private boolean continueWithProcess = false;
    private int currentPanelIndex = 0;


    public DataReductionGUI(ArrayList<SimResultsLoader> simsToOpen, double simCSize, double simZSize, double simTSize){
        this.simsToOpen = simsToOpen;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        okayButton.setEnabled(false);

        roiSelection = new RoiSelection(this);
        normalizeGUI = new NormalizeGUI(jDialog, simTSize);
        conclusion = new Conclusion(this, simCSize, simZSize, simTSize);
        imagesToMeasurePanel = new ImagesToMeasure(simsToOpen, this);

        panels.add(imagesToMeasurePanel);
        panels.add(roiSelection);
        panels.add(conclusion);

        switchPanel.add(panels.get(0));
        add(switchPanel);
        add(okayBackPanel());
        setVisible(true);

        okayButton.addActionListener(this);
        backButton.addActionListener(this);


        this.setBorder(new EmptyBorder(15, 12, 15, 12));

        jDialog.add(this);
        jDialog.setVisible(true);
        jDialog.setResizable(true);
        jDialog.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        jDialog.setTitle("Measurement Script");
        jDialog.pack();
    }

    public DataReductionSubmission createSubmission(){
        int[] simRange = normalizeGUI.getSimTimRange();
        int[] labRange = normalizeGUI.getImageTimeRange();
        return new DataReductionSubmission(false,
                roiSelection.getSimROIList(), roiSelection.getImageROIList(),
                imagesToMeasurePanel.getChosenExpImage(),
                simRange[0], simRange[1], labRange[0], labRange[1], simsToOpen.size(), resultSaveFile,
                conclusion.selectSimRange.getRangeOfSim(), conclusion.selectMeasurements.getChosenMeasurements(), conclusion.selectTableFormat.isWideTableSelected());
    }

    private JPanel okayBackPanel(){
        JPanel jPanel = new JPanel();
        jPanel.add(backButton);
        jPanel.add(okayButton);
        return jPanel;
    }

    public void activateNext(){
        okayButton.setEnabled(true);
    }

    public void activateOkayButton(){
        boolean selectedAMeasurement = !conclusion.selectMeasurements.getChosenMeasurements().isEmpty();
        boolean chosenExperimentImage = imagesToMeasurePanel.getChosenExpImage() != null;
        okayButton.setEnabled(selectedAMeasurement && chosenExperimentImage);
    }

    public void updateDisplay(){
        jDialog.revalidate();
        jDialog.repaint();
        jDialog.pack();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(okayButton)) {
            if (okayButton.getText().equals("Next")){
                if (currentPanelIndex == panels.size() - 1){
                    return;
                }
                switchPanel.remove(panels.get(currentPanelIndex));
                currentPanelIndex += 1;
                switchPanel.add(panels.get(currentPanelIndex));
                if (panels.get(currentPanelIndex) != roiSelection){
                    okayButton.setEnabled(false);
                }
                if (currentPanelIndex == panels.size() - 1){
                    okayButton.setText("Finish");
                }
                updateDisplay();
            } else {
                JFileChooser saveToFile = new JFileChooser();
                saveToFile.setFileSelectionMode(JFileChooser.FILES_ONLY);
                int fileChooserReturnValue = saveToFile.showDialog(this, "Save Results To File");
                if (fileChooserReturnValue == JFileChooser.APPROVE_OPTION){
                    resultSaveFile = saveToFile.getSelectedFile();
                    MainPanel.controlButtonsPanel.updateButtonsToMatchState();
                    continueWithProcess = true;
                    jDialog.dispose();
                }
            }
        } else if (e.getSource().equals(backButton)) {
            if (currentPanelIndex == 0){
                return;
            }
            okayButton.setText("Next");
            switchPanel.remove(panels.get(currentPanelIndex));
            currentPanelIndex -= 1;
            switchPanel.add(panels.get(currentPanelIndex));
            updateDisplay();
        }
    }

    public boolean shouldContinueWithProcess() {
        return continueWithProcess;
    }

    public static void main(String[] args) {
        DataReductionGUI dataReductionGUI = new DataReductionGUI(new ArrayList<>(), 0, 0, 0);
    }

    static class CustomDialog extends JDialog{
        public CustomDialog(JFrame parent, boolean modal){
            super(parent, modal);
        }

        @Override
        public void dispose() {
            MainPanel.controlButtonsPanel.updateButtonsToMatchState(false, ControlButtonsPanel.PanelState.NOTHING_OR_LOADING_IMAGE);
            super.dispose();
        }
    }
}








