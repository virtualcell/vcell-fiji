package org.vcell.N5.retrieving;

import com.amazonaws.AbortedException;
import ij.ImagePlus;
import org.scijava.log.Logger;
import org.vcell.N5.N5ImageHandler;
import org.vcell.N5.UI.ControlButtonsPanel;
import org.vcell.N5.UI.RangeSelector;
import org.vcell.N5.UI.MainPanel;
import org.vcell.N5.reduction.DataReductionGUI;
import org.vcell.N5.reduction.DataReductionManager;
import org.vcell.N5.reduction.DataReductionWriter;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class LoadingManager implements SimLoadingEventCreator {
    private static final EventListenerList eventListenerList = new EventListenerList();

    private final ControlButtonsPanel controlButtonsPanel = MainPanel.controlButtonsPanel;

    private final HashMap<String, Thread> openingSimulations = new HashMap<>();
    private final Object openSimulationsLock = new Object();
    private DataReductionManager dataReductionWriter = null;

    private static final Logger logger = N5ImageHandler.getLogger(RangeSelector.class);

    public void openN5FileDataset(ArrayList<SimResultsLoader> filesToOpen, boolean openInMemory, boolean dataReduction){
        RangeSelector rangeSelector = new RangeSelector();
        DataReductionGUI dataReductionGUI = null;
        if (dataReduction || openInMemory){
            SimResultsLoader firstSim = filesToOpen.get(0);
            firstSim.createS3ClientAndReader();
            ArrayList<Double> dimensions = firstSim.getN5Dimensions();
            if (dataReduction){
                dataReductionGUI = new DataReductionGUI(filesToOpen, dimensions.get(2), dimensions.get(3), dimensions.get(4));
                dataReductionWriter = dataReductionGUI.shouldContinueWithProcess() ? new DataReductionManager(dataReductionGUI.createSubmission()) : null;
            } else {
                rangeSelector.displayRangeMenu(dimensions.get(2), dimensions.get(3), dimensions.get(4));
            }
        }
        boolean dataReductionOkay = dataReduction && dataReductionGUI.shouldContinueWithProcess();
        if (dataReductionOkay || !dataReduction){
            MainPanel.changeCursor(new Cursor(Cursor.WAIT_CURSOR));
            for (int i = 0; i < filesToOpen.size(); i++){
                SimResultsLoader simResultsLoader = filesToOpen.get(i);
                Thread openThread = new Thread(() -> {
                    ImagePlus imagePlus = null;
                    try{
                        simResultsLoader.createS3ClientAndReader();
                        notifySimIsLoading(simResultsLoader);
                        if (openInMemory){
                            if (!rangeSelector.cancel){
                                imagePlus = simResultsLoader.openInMemory(rangeSelector);
                            }
                        } else{
                            simResultsLoader.loadImageFromN5File();
                            imagePlus = simResultsLoader.getImagePlus();
                        }
                    }
                    catch (RuntimeException e) {
                        simResultsLoader.setTagToCanceled();
                        MainPanel.n5ExportTable.removeSpecificRowFromLoadingRows(simResultsLoader.rowNumber);
                        if (e instanceof AbortedException){
                            logger.debug("Simulation stopped loading");
                        } else {
                            throw new RuntimeException(e);
                        }
                    } finally {
                        MainPanel.changeCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                        notifySimIsDoneLoading(simResultsLoader, imagePlus);
                        synchronized (openSimulationsLock){
                            openingSimulations.remove(simResultsLoader.exportID);
                        }
                    }
                });
                openThread.setName("Opening sim number: " + i + ". With id: " + simResultsLoader.exportID);
                synchronized (openSimulationsLock){
                    openingSimulations.put(simResultsLoader.exportID, openThread);
                }
                openThread.start();
            }
        }
    }

    public void stopAllImagesAndAnalysis(){
        Thread stopEverything = new Thread(() -> {
            synchronized (openSimulationsLock){
                for (String threadName : openingSimulations.keySet()){
                    openingSimulations.get(threadName).interrupt();
                    openingSimulations.remove(threadName);
                }
            }
            if (dataReductionWriter != null){
                dataReductionWriter.stopAllThreads();
            }
        });
        stopEverything.start();
    }


    public void stopLoadingImage(String exportID){
        Thread stopOtherThread = new Thread(() -> {
            synchronized (openSimulationsLock){
                if (openingSimulations.containsKey(exportID)){
                    openingSimulations.get(exportID).interrupt();
                    openingSimulations.remove(exportID);
                }
            }
        });
        stopOtherThread.start();
    }

    public void openLocalN5FS(ArrayList<SimResultsLoader> filesToOpen){
        controlButtonsPanel.enableAllButtons(true);
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
                            controlButtonsPanel.enableAllButtons(true);
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

    public void removeFromSimLoadingListener(SimLoadingListener simLoadingListener){
        eventListenerList.remove(SimLoadingListener.class, simLoadingListener);
    }

    @Override
    public void notifySimIsLoading(SimResultsLoader simResultsLoader) {
        for (SimLoadingListener simLoadingListener: eventListenerList.getListeners(SimLoadingListener.class)){
            simLoadingListener.simIsLoading(simResultsLoader.rowNumber, simResultsLoader.exportID);
        }
    }

    @Override
    public void notifySimIsDoneLoading(SimResultsLoader simResultsLoader, ImagePlus imagePlus) {
        for (SimLoadingListener simLoadingListener: eventListenerList.getListeners(SimLoadingListener.class)){
            simLoadingListener.simFinishedLoading(simResultsLoader);
        }
    }
}
