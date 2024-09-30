package org.vcell.N5;

import ij.ImagePlus;
import org.vcell.N5.UI.ImageIntoMemory;
import org.vcell.N5.UI.N5ExportTable;
import org.vcell.N5.UI.SimLoadingEventCreator;
import org.vcell.N5.UI.SimLoadingListener;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class LoadingFactory implements SimLoadingEventCreator{
    private static final EventListenerList eventListenerList = new EventListenerList();

    public void openN5FileDataset(ArrayList<SimResultsLoader> filesToOpen, boolean openInMemory){
        N5ExportTable.enableCriticalButtons(false);
        N5ExportTable.exportTableDialog.setCursor(new Cursor(Cursor.WAIT_CURSOR));
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
                        imageIntoMemory.displayRangeMenu();
                    } else{
                        ImagePlus imagePlus = simResultsLoader.getImgPlusFromN5File();
                        imagePlus.show();
                        notifySimIsDoneLoading(simResultsLoader);
                    }
                }
            } catch (Exception ex) {
                N5ExportTable.exportTableDialog.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                N5ExportTable.enableCriticalButtons(true);
                throw new RuntimeException(ex);
            } finally {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        if (!openInMemory) {
                            N5ExportTable.exportTableDialog.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                            N5ExportTable.enableCriticalButtons(true);
                        }
                    }
                });
            }
        });
        openN5FileDataset.setName("Open N5 File");
        openN5FileDataset.start();
    }

    public void openLocalN5FS(ArrayList<SimResultsLoader> filesToOpen){
        N5ExportTable.enableCriticalButtons(true);
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setAcceptAllFileFilterUsed(false);
        int result = fileChooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION){
            File file = fileChooser.getSelectedFile();
            N5ExportTable.exportTableDialog.setCursor(new Cursor(Cursor.WAIT_CURSOR));
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
                            N5ExportTable.exportTableDialog.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                            N5ExportTable.enableCriticalButtons(true);
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
            simLoadingListener.simIsLoading(simResultsLoader.rowNumber);
        }
    }

    @Override
    public void notifySimIsDoneLoading(SimResultsLoader simResultsLoader) {
        for (SimLoadingListener simLoadingListener: eventListenerList.getListeners(SimLoadingListener.class)){
            simLoadingListener.simFinishedLoading(simResultsLoader.rowNumber);
        }
    }
}
