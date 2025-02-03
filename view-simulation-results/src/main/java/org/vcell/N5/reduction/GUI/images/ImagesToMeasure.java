package org.vcell.N5.reduction.GUI.images;

import ij.ImagePlus;
import ij.WindowManager;
import org.vcell.N5.reduction.GUI.DataReductionGUI;
import org.vcell.N5.retrieving.SimResultsLoader;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

public class ImagesToMeasure extends JPanel implements ActionListener {
    private final Border lowerEtchedBorder = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
    private JComboBox<String> chosenImage;
    private final ArrayList<SimResultsLoader> simsToOpen;
    private final DataReductionGUI parent;

    public ImagesToMeasure(ArrayList<SimResultsLoader> simsToOpen, DataReductionGUI parent){
        this.simsToOpen = simsToOpen;
        this.parent = parent;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createTitledBorder(lowerEtchedBorder, "Images To Measure"));
        add(imageSelectionPanel());
        add(selectedImagesToOpenPanel());

        chosenImage.addActionListener(this);
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

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(chosenImage)){
            parent.activateNext();
        }
    }
}
