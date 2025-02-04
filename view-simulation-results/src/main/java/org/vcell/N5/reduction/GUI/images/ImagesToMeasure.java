package org.vcell.N5.reduction.GUI.images;

import ij.ImagePlus;
import ij.WindowManager;
import org.vcell.N5.retrieving.SimResultsLoader;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.util.ArrayList;

public class ImagesToMeasure extends JPanel {
    private final Border lowerEtchedBorder = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
    private JComboBox<String> chosenImage;
    private final ArrayList<SimResultsLoader> simsToOpen;

    public ImagesToMeasure(ArrayList<SimResultsLoader> simsToOpen){
        this.simsToOpen = simsToOpen;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        Border border = new CompoundBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5),
                BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "1. Images To Measure"));
        setBorder(border);
        add(imageSelectionPanel());
        add(selectedImagesToOpenPanel());
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
        String[] namesOfImagesToOpen = new String[simsToOpen.size()];
        for (int i = 0; i < simsToOpen.size(); i++){
            namesOfImagesToOpen[i] = simsToOpen.get(i).userSetFileName;
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

    public ImagePlus getChosenExpImage(){
        return WindowManager.getImage((String) chosenImage.getSelectedItem());
    }

}
