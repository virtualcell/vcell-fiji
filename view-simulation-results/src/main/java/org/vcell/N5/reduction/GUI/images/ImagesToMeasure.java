package org.vcell.N5.reduction.GUI.images;

import ij.ImagePlus;
import ij.WindowManager;
import ij.io.Opener;
import ij.io.RoiDecoder;
import org.vcell.N5.retrieving.SimResultsLoader;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;

public class ImagesToMeasure extends JPanel {
    private final Border lowerEtchedBorder = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
    private JComboBox<String> chosenImage = null;
    private final ArrayList<SimResultsLoader> simsToOpen;
    private ImagePlus experimentalImage;

    public ImagesToMeasure(ArrayList<SimResultsLoader> simsToOpen){
        this.simsToOpen = simsToOpen;

        setLayout(new GridLayout(1, 2));
        Border border = new CompoundBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5),
                BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "1. Images To Measure"));
        setBorder(border);
        add(imageSelectionPanel());
        add(selectedImagesToOpenPanel());
    }

    private JPanel imageSelectionPanel(){
        JPanel jPanel = new JPanel();
        jPanel.setLayout(new BoxLayout(jPanel, BoxLayout.Y_AXIS));
        String[] imageTitles = WindowManager.getImageTitles();
        if (imageTitles.length == 0){
            JButton openExperimentalImage = new JButton("Open Experimental Image");
            openExperimentalImage.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    JFileChooser fileChooser = new JFileChooser();
                    fileChooser.setMultiSelectionEnabled(false);
                    int choice = fileChooser.showDialog(ImagesToMeasure.this, "Open Image");
                    if (choice == JFileChooser.APPROVE_OPTION){
                        File file = fileChooser.getSelectedFile();
                        experimentalImage = new Opener().openImage(file.getAbsolutePath());
                        jPanel.removeAll();
                        jPanel.add(new JLabel("<HTML><i>Selected Experimental Image</i></HTML>"));
                        JList<String> expImageList = new JList<>(new String[]{experimentalImage.getTitle()});
                        expImageList.setEnabled(false);
                        expImageList.setVisibleRowCount(4);
                        jPanel.add(new JScrollPane(expImageList));
                        jPanel.updateUI();
                    }
                }
            });
            jPanel.add(new JLabel("<HTML><i>Experimental</i></HTML>"));
            jPanel.add(openExperimentalImage);
        } else {
            chosenImage = new JComboBox<>(WindowManager.getImageTitles());
            jPanel.add(chosenImage);
        }
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
        jPanel.add(new JLabel("<HTML><i>Selected Simulations</i></HTML>"));
        jPanel.add(jScrollPane);
        jPanel.setLayout(new BoxLayout(jPanel, BoxLayout.Y_AXIS));
        return jPanel;
    }

    public ImagePlus getChosenExpImage(){
        if (chosenImage != null){
            return WindowManager.getImage((String) chosenImage.getSelectedItem());
        }
        return experimentalImage;
    }

}
