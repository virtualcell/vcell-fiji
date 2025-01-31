package org.vcell.N5.UI;

import org.vcell.N5.N5ImageHandler;
import org.vcell.N5.retrieving.SimResultsLoader;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ControlButtonsPanel extends JPanel implements ActionListener {

    private static JButton openOrCancel;
    private final String openButtonText = "Open Virtual Stack";
    private final String cancelButtonText = "Cancel";

    private final JButton dataReduction;
    private final String runScriptButtonText = "Run Measurement Script";
    private final String cancelScriptButtonText = "Cancel Measurement Script";
//    private final JButton openLocal = new JButton("Open N5 Local");
    private final JButton questionMark;

    public final JCheckBox displayAdvancedFeatures;

    private N5ExportTable n5ExportTable;
    private RemoteFileSelection remoteFileSelection;
    public final AdvancedFeatures advancedFeatures = new AdvancedFeatures();
    private PanelState panelState = PanelState.NOTHING_OR_LOADING_IMAGE;

    public ControlButtonsPanel(){
        displayAdvancedFeatures = new JCheckBox("Advanced Features");

        openOrCancel = new JButton("Open Virtual Stack");
        dataReduction = new JButton(runScriptButtonText);
        questionMark = new JButton("?");
        questionMark.setPreferredSize(new Dimension(20, 20));

        GridBagConstraints gridBagConstraints = new GridBagConstraints();

        JPanel topRow = new JPanel(new GridBagLayout());
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        topRow.add(openOrCancel, gridBagConstraints);

        gridBagConstraints.gridx = 1;
        topRow.add(dataReduction, gridBagConstraints);



        JPanel bottomRow = new JPanel(new GridBagLayout());
        gridBagConstraints.gridx = 1;
        bottomRow.add(displayAdvancedFeatures, gridBagConstraints);
        gridBagConstraints.gridx = 2;
        bottomRow.add(questionMark);

        JPanel userButtonsPanel = new JPanel(new GridBagLayout());
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        userButtonsPanel.add(topRow, gridBagConstraints);
        gridBagConstraints.gridy = 1;
        userButtonsPanel.add(bottomRow, gridBagConstraints);

//        buttonsPanel.add(questionMark);

        int paneWidth = 800;
        this.setPreferredSize(new Dimension(paneWidth, 110));
        this.setLayout(new BorderLayout());
//        topBar.add(openLocal);
        Border lowerEtchedBorder = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
        this.add(userButtonsPanel, BorderLayout.EAST);
        this.setBorder(BorderFactory.createTitledBorder(lowerEtchedBorder, " User Options "));

        advancedFeatures.setVisible(false);
        add(advancedFeatures, BorderLayout.WEST);


        openOrCancel.addActionListener(this);
        advancedFeatures.copyLink.addActionListener(this);
        questionMark.addActionListener(this);
        advancedFeatures.useN5Link.addActionListener(this);
//        openLocal.addActionListener(this);
        displayAdvancedFeatures.addActionListener(this);
        dataReduction.addActionListener(this);
        advancedFeatures.openInMemory.addActionListener(this);


        openOrCancel.setEnabled(false);
        dataReduction.setEnabled(false);
        advancedFeatures.copyLink.setEnabled(false);
    }

    public void initialize(N5ExportTable n5ExportTable, RemoteFileSelection remoteFileSelection){
        this.n5ExportTable = n5ExportTable;
        this.remoteFileSelection = remoteFileSelection;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        boolean inMemory = e.getSource().equals(advancedFeatures.openInMemory);
        if(e.getSource().equals(openOrCancel) || inMemory){
            panelState = PanelState.NOTHING_OR_LOADING_IMAGE;
            if (openOrCancel.getText().equals(cancelButtonText)){
                n5ExportTable.stopSelectedImageFromLoading();
                updateButtonsToMatchState(false);
            } else {
                n5ExportTable.openSelectedRows(inMemory, false, SimResultsLoader.OpenTag.VIEW);
                updateButtonsToMatchState(true);
            }
        } else if (e.getSource().equals(dataReduction)) {
            if (dataReduction.getText().equals(cancelScriptButtonText)){
                panelState = PanelState.NOTHING_OR_LOADING_IMAGE;
                N5ImageHandler.loadingManager.stopAllImagesAndAnalysis();
            } else{
                panelState = PanelState.PERFORMING_ANALYSIS;
                setButtonsToCancelReduction();
                n5ExportTable.openSelectedRows(false, true, SimResultsLoader.OpenTag.DATA_REDUCTION);
            }
            updateButtonsToMatchState();
        } else if (e.getSource().equals(advancedFeatures.copyLink)) {
            n5ExportTable.copySelectedRowLink();
        } else if (e.getSource().equals(questionMark)) {
            new HelpExplanation().displayHelpMenu();
        } else if (e.getSource().equals(advancedFeatures.useN5Link)) {
            remoteFileSelection.setVisible(true);
        } else if (e.getSource().equals(displayAdvancedFeatures)) {
            advancedFeatures.setVisible(displayAdvancedFeatures.isSelected());
        }
    }

    public void updateButtonsToMatchState(){
        updateButtonsToMatchState(false);
    }

    public void updateButtonsToMatchState(boolean rowIsLoadingImage){
        updateButtonsToMatchState(rowIsLoadingImage, panelState);
    }

    public void setStateToInitializing(boolean isInitializing){
        panelState = isInitializing ? PanelState.INITIALIZING : PanelState.NOTHING_OR_LOADING_IMAGE;
    }

    public void updateButtonsToMatchState(boolean rowIsLoadingImage, PanelState newPanelState){
        switch (newPanelState){
            case NOTHING_OR_LOADING_IMAGE:
                if (rowIsLoadingImage){
                    allowCancel();
                } else {
                    enableAllButtons(true);
                }
                break;
            case PERFORMING_ANALYSIS:
                setButtonsToCancelReduction();
                break;
            case INITIALIZING:
                enableAllButtons(false);
                break;
        }
        panelState = newPanelState;
    }

    public void setButtonsToCancelReduction(){
        openOrCancel.setText(openButtonText);
        openOrCancel.setEnabled(false);
        advancedFeatures.useN5Link.setEnabled(false);
        advancedFeatures.openInMemory.setEnabled(false);

        advancedFeatures.copyLink.setEnabled(true);
        dataReduction.setText(cancelScriptButtonText);
    }

    private void allowCancel(){
        openOrCancel.setEnabled(true);
        advancedFeatures.copyLink.setEnabled(true);
        advancedFeatures.useN5Link.setEnabled(true);
        remoteFileSelection.submitS3Info.setEnabled(true);
        dataReduction.setEnabled(false);
        advancedFeatures.openInMemory.setEnabled(false);
        openOrCancel.setText(cancelButtonText);
    }

    public void disableAllContextDependentButtons(){
        openOrCancel.setEnabled(false);
        advancedFeatures.copyLink.setEnabled(false);
        dataReduction.setEnabled(false);
        advancedFeatures.openInMemory.setEnabled(false);
    }

    public void enableAllButtons(boolean enable){
        openOrCancel.setText(openButtonText);
        dataReduction.setText(runScriptButtonText);

        advancedFeatures.useN5Link.setEnabled(enable);
        openOrCancel.setEnabled(enable);
        advancedFeatures.copyLink.setEnabled(enable);
        remoteFileSelection.submitS3Info.setEnabled(enable);
        dataReduction.setEnabled(enable);
        advancedFeatures.openInMemory.setEnabled(enable);
    }

    public enum PanelState {
        PERFORMING_ANALYSIS,
        NOTHING_OR_LOADING_IMAGE,
        INITIALIZING
    }
}
