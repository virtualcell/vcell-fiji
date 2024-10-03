package org.vcell.N5.retrieving;

import ij.ImagePlus;
import org.vcell.N5.UI.ControlButtonsPanel;
import org.vcell.N5.UI.ImageIntoMemory;
import org.vcell.N5.UI.MainPanel;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class LoadingFactory implements SimLoadingEventCreator {
    private static final EventListenerList eventListenerList = new EventListenerList();

    private final ControlButtonsPanel controlButtonsPanel = MainPanel.controlButtonsPanel;

    public void openN5FileDataset(ArrayList<SimResultsLoader> filesToOpen, boolean openInMemory){
        controlButtonsPanel.enableCriticalButtons(false);
        MainPanel.changeCursor(new Cursor(Cursor.WAIT_CURSOR));
        Thread openN5FileDataset = new Thread(() -> {
            try{
                // Create clients and show loading status
                for(SimResultsLoader simResultsLoader: filesToOpen){
                    simResultsLoader.createS3ClientAndReader();
                    notifySimIsLoading(simResultsLoader);
                }
                for (SimResultsLoader simResultsLoader: filesToOpen){
                    ImageIntoMemory imageIntoMemory;
                    if (openInMemory){
                        ArrayList<Double> dimensions = simResultsLoader.getN5Dimensions();
                        imageIntoMemory = new ImageIntoMemory(dimensions.get(2), dimensions.get(3), dimensions.get(4), simResultsLoader);
                        for (SimLoadingListener simLoadingListener: eventListenerList.getListeners(SimLoadingListener.class)){
                            imageIntoMemory.addSimLoadingListener(simLoadingListener);
                        }
                        imageIntoMemory.displayRangeMenu();
                    } else{
                        ImagePlus imagePlus = simResultsLoader.getImgPlusFromN5File();
                        imagePlus.show();
                        notifySimIsDoneLoading(simResultsLoader);
                    }
                }
            } catch (Exception ex) {
                MainPanel.changeCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                controlButtonsPanel.enableCriticalButtons(true);
                throw new RuntimeException(ex);
            } finally {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        if (!openInMemory) {
                            MainPanel.changeCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                            controlButtonsPanel.enableCriticalButtons(true);
                        }
                    }
                });
            }
        });
        openN5FileDataset.setName("Open N5 File");
        openN5FileDataset.start();
    }

    public void openLocalN5FS(ArrayList<SimResultsLoader> filesToOpen){
        controlButtonsPanel.enableCriticalButtons(true);
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setAcceptAllFileFilterUsed(false);
        int result = fileChooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION){
            File file = fileChooser.getSelectedFile();
            MainPanel.changeCursor(new Cursor(Cursor.WAIT_CURSOR));
            Thread openN5FileDataset = new Thread(() -> {
                try{
                    for(SimResultsLoader simResultsLoader: filesToOpen){
                        simResultsLoader.setSelectedLocalFile(file);
                        ImagePlus imagePlus = simResultsLoader.getImgPlusFromLocalN5File();
                        imagePlus.show();
                    }
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                } finally {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            MainPanel.changeCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                            controlButtonsPanel.enableCriticalButtons(true);
                        }
                    });
                }
            });
            openN5FileDataset.start();
        }
    }

    @Override
    public void addSimLoadingListener(SimLoadingListener simLoadingListener) {
        eventListenerList.add(SimLoadingListener.class, simLoadingListener);
    }

    @Override
    public void notifySimIsLoading(SimResultsLoader simResultsLoader) {
        for (SimLoadingListener simLoadingListener: eventListenerList.getListeners(SimLoadingListener.class)){
            simLoadingListener.simIsLoading(simResultsLoader.rowNumber, simResultsLoader.exportID);
        }
    }

    @Override
    public void notifySimIsDoneLoading(SimResultsLoader simResultsLoader) {
        for (SimLoadingListener simLoadingListener: eventListenerList.getListeners(SimLoadingListener.class)){
            simLoadingListener.simFinishedLoading(simResultsLoader.rowNumber, simResultsLoader.exportID);
        }
    }
}
