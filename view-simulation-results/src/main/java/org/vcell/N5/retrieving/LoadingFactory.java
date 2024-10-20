package org.vcell.N5.retrieving;

import com.amazonaws.AbortedException;
import com.amazonaws.http.timers.client.SdkInterruptedException;
import ij.ImagePlus;
import ij.plugin.Duplicator;
import org.scijava.log.Logger;
import org.vcell.N5.N5ImageHandler;
import org.vcell.N5.UI.ControlButtonsPanel;
import org.vcell.N5.UI.RangeSelector;
import org.vcell.N5.UI.MainPanel;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class LoadingFactory implements SimLoadingEventCreator {
    private static final EventListenerList eventListenerList = new EventListenerList();

    private final ControlButtonsPanel controlButtonsPanel = MainPanel.controlButtonsPanel;

    private final HashMap<String, Thread> openingSimulations = new HashMap<>();
    private final Object openSimulationsLock = new Object();

    private static final Logger logger = N5ImageHandler.getLogger(RangeSelector.class);

    public void openN5FileDataset(ArrayList<SimResultsLoader> filesToOpen, boolean openInMemory){
        controlButtonsPanel.allowCancel(true);
        MainPanel.changeCursor(new Cursor(Cursor.WAIT_CURSOR));

        for (int i = 0; i < filesToOpen.size(); i++){
            SimResultsLoader simResultsLoader = filesToOpen.get(i);
            Thread openThread = new Thread(() -> {
                try{
                    simResultsLoader.createS3ClientAndReader();
                    notifySimIsLoading(simResultsLoader);
                    RangeSelector rangeSelector;
                    if (openInMemory){
                        ArrayList<Double> dimensions = simResultsLoader.getN5Dimensions();
                        rangeSelector = new RangeSelector(dimensions.get(2), dimensions.get(3), dimensions.get(4), simResultsLoader.userSetFileName);
                        rangeSelector.displayRangeMenu();
                        if (!rangeSelector.cancel){
                            openInMemory(simResultsLoader, rangeSelector);
                        }
                        rangeSelector.dispose();
                    } else{
                        ImagePlus imagePlus = simResultsLoader.getImgPlusFromN5File();
                        imagePlus.show();
                    }
                }
                catch (RuntimeException e) {
                    if (e.getCause().getCause().getCause() instanceof SdkInterruptedException ||
                            e.getCause().getCause() instanceof AbortedException){
                        logger.debug("Simulation stopped loading");
                    } else {
                        throw new RuntimeException(e);
                    }
                }
                catch (Exception e){
                    throw new RuntimeException(e);
                } finally {
                    MainPanel.changeCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                    controlButtonsPanel.enableCriticalButtons(true);
                    notifySimIsDoneLoading(simResultsLoader);
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

    private void openInMemory(SimResultsLoader simResultsLoader, RangeSelector rangeSelector) throws IOException {
        ImagePlus imagePlus = simResultsLoader.getImgPlusFromN5File();
        long start = System.currentTimeMillis();
        logger.debug("Loading Virtual N5 File " + simResultsLoader.userSetFileName + " Into Memory");
        imagePlus = new Duplicator().run(imagePlus, rangeSelector.startC, rangeSelector.endC, rangeSelector.startZ,
                rangeSelector.endZ, rangeSelector.startT, rangeSelector.endT);
        long end = System.currentTimeMillis();
        logger.debug("Loaded Virtual N5 File " + simResultsLoader.userSetFileName + " Into Memory taking: " + ((end - start)/ 1000) + "s");
        imagePlus.show();
    }

    public void stopOpeningSimulation(String exportID){
        Thread stopOtherThread = new Thread(() -> {
            synchronized (openSimulationsLock){
                openingSimulations.get(exportID).interrupt();
                openingSimulations.remove(exportID);
            }
        });
        stopOtherThread.start();
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
