package org.vcell.N5.UI;

import javax.swing.*;

public class HelpExplanation {
    private JDialog jDialog;

    public HelpExplanation(){
        JPanel helperPanel = new JPanel();
        JTextArea textArea = new JTextArea();

        String intro = "The VCell application can export the simulations it creates into multiple formats. The format type .N5 is stored remotely on VCell servers,"
                + "and allows for other applications such as ImageJ to access the results of these simulations, perform some type of analysis, and never have to save"
                + "the simulation results locally. \n\n\n";

        String accessingSimulationResults = "There are three methods for accessing simulation results within this plugin. \n"
                + "1. Copy the export link from the VCell app, click the 'Remote Files' button, paste the link in the text box then open it. This method is best for"
                + "when you need to save a set of exports for the long term future. \n"
                + "2. Click the recent export button to open the most recent simulation you exported in the N5 format. \n"
                + "3. Open the export table and open any export within that table using the open button. The tables information is stored locally on your computer so"
                + "if your computers drive fails the table will be empty, but any links saved elsewhere can still be used. \n\n\n";

        String n5AndDataSet = "N5 files are one to one with simulations within VCell in terms of grouping."
                +"Data sets are the grouping category for different export variances. For example with simulation A, if you decide to export only 2 of 3 variables, that"
                + " would be one dataset. For that same simulation if you decide to 1 variable that would be a completely different dataset. "
                + "Now with simulation B, if you decide to export 2 of 3 variables that would be a different N5 file and a different dataset.";

        textArea.setText("Intro: \n" + intro + "Accessing Simulation Results: \n" + accessingSimulationResults + "N5 and Datasets: \n" + n5AndDataSet);
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setSize(600, 600);
        helperPanel.add(textArea);

        JOptionPane pane = new JOptionPane(helperPanel, JOptionPane.PLAIN_MESSAGE, 0, null, new Object[]{"Close"});
        jDialog = pane.createDialog("Helper Menu");
        jDialog.setModal(false);
        jDialog.setResizable(true);
    }

    public void displayHelpMenu(){
        jDialog.setVisible(true);
    }
}
