package org.vcell.N5.reduction;

import com.google.gson.internal.LinkedTreeMap;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Roi;
import org.vcell.N5.N5ImageHandler;
import org.vcell.N5.UI.ControlButtonsPanel;
import org.vcell.N5.UI.MainPanel;
import org.vcell.N5.reduction.DTO.RangeOfImage;
import org.vcell.N5.reduction.DTO.ReducedData;
import org.vcell.N5.retrieving.SimLoadingListener;
import org.vcell.N5.retrieving.SimResultsLoader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class DataReductionManager implements SimLoadingListener {
    private final ArrayList<Roi> arrayOfSimRois;
    private final Object csvMatrixLock = new Object();

    private int numOfImagesToOpen;
    private final ReductionCalculations calculations;

    public final DataReductionGUI.DataReductionSubmission submission;

    private final ConcurrentHashMap<String, ThreadStruct> threadPool = new ConcurrentHashMap<>();
    private final Object threadPoolLock = new Object();

    private DataReductionWriter dataReductionWriter;

    public DataReductionManager(DataReductionGUI.DataReductionSubmission submission){
        N5ImageHandler.loadingManager.addSimLoadingListener(this);
        this.submission = submission;

        this.arrayOfSimRois = submission.arrayOfSimRois;
        this.numOfImagesToOpen = submission.numOfSimImages + 1; // Plus one for the lab image
        this.calculations = new ReductionCalculations(submission.normalizeMeasurementsBool);

        Thread processLabResults = new Thread(() -> {
            calculateAndAddResults(submission.labResults, submission.experiementNormRange, submission.experimentImageRange,
                    submission.arrayOfLabRois, null, "Lab");
            synchronized (threadPoolLock){
                threadPool.remove("Lab");
            }
        }, "Processing Lab Image");
        ThreadStruct threadStruct = new ThreadStruct(submission.labResults, new AtomicBoolean(true), processLabResults);
        synchronized (threadPoolLock){
            threadPool.put("Lab", threadStruct);
        }
    }

    ////////////////////////
    // General Functions //
    //////////////////////

    private void calculateAndAddResults(ImagePlus imagePlus, RangeOfImage normRange,
                                        RangeOfImage imageRange, ArrayList<Roi> rois,
                                        LinkedTreeMap<String, LinkedTreeMap<String, String>> channelInfo,
                                        String threadName){
        HashMap<String, Double> normValue = null;
        if (submission.normalizeMeasurementsBool){
            normValue = calculations.calculateNormalValue(imagePlus, normRange, rois, imageRange);
        }

        ReducedData reducedData = new ReducedData(imagePlus.getTitle(), imageRange, arrayOfSimRois.size(), submission.selectedMeasurements);
        calculations.addAppropriateHeaders(rois, imageRange, reducedData, channelInfo);

        AtomicBoolean continueOperation = threadPool.get(threadName).continueOperation;
        calculations.calculateStatistics(imagePlus, rois, normValue, reducedData, imageRange, continueOperation);
        if (continueOperation.get()){
            synchronized (csvMatrixLock){
                try {
                    dataReductionWriter.consumeNewData(reducedData);
                } catch (IOException e) {
                    stopAllThreads();
                    throw new RuntimeException(e);
                }
                numOfImagesToOpen -= 1;
            }
        }
        if (numOfImagesToOpen == 0 && continueOperation.get()){
            try{
                dataReductionWriter.close();
            } catch (IOException ioException){
                throw new RuntimeException(ioException);
            } finally {
                N5ImageHandler.loadingManager.removeFromSimLoadingListener(this);
                MainPanel.controlButtonsPanel.updateButtonsToMatchState(false, ControlButtonsPanel.PanelState.NOTHING_OR_LOADING_IMAGE);
            }
        }
    }

    public void stopAllThreads(){
        synchronized (threadPoolLock){
            for (String threadName: threadPool.keySet()){
                ThreadStruct threadStruct = threadPool.get(threadName);
                threadStruct.continueOperation.set(false);
                // Experiment image is in thread pool, so trying to retrieve a results loader for it would not work
                if (threadStruct.simResultsLoader != null){
                    SimResultsLoader loadedResults = threadStruct.simResultsLoader;
                    MainPanel.n5ExportTable.removeSpecificRowFromLoadingRows(loadedResults.rowNumber);
                    ImagePlus openImage = WindowManager.getImage(loadedResults.getImagePlus().getID());
                    if (openImage != null){
                        openImage.close();
                    }
                }
                threadPool.remove(threadName);
            }
        }
        N5ImageHandler.loadingManager.removeFromSimLoadingListener(this);
        MainPanel.controlButtonsPanel.updateButtonsToMatchState(false, ControlButtonsPanel.PanelState.NOTHING_OR_LOADING_IMAGE);
    }


    @Override
    public void simIsLoading(int itemRow, String exportID) {

    }

    @Override
    public void simFinishedLoading(SimResultsLoader loadedResults) {
        if (loadedResults.openTag == SimResultsLoader.OpenTag.DATA_REDUCTION){
            Thread imageProcessingThread = new Thread(() -> {
                ImagePlus imagePlus = loadedResults.getImagePlus();
                imagePlus.show();
                dataReductionWriter.addMetaData(loadedResults);
                calculateAndAddResults(imagePlus, submission.simNormRange, submission.simImageRange,
                        submission.arrayOfSimRois, loadedResults.getChannelInfo(), loadedResults.exportID);
                MainPanel.n5ExportTable.removeSpecificRowFromLoadingRows(loadedResults.rowNumber);
                imagePlus.close();
                synchronized (threadPoolLock){
                    threadPool.remove(loadedResults.exportID);
                }
            }, "Processing Image: " + loadedResults.userSetFileName);
            ThreadStruct threadStruct = new ThreadStruct(loadedResults, new AtomicBoolean(true), imageProcessingThread);
            synchronized (threadPoolLock){
                threadPool.put(loadedResults.exportID, threadStruct);
                if (threadPool.size() == (submission.numOfSimImages + 1)){
                    int maxZ = 0;
                    int maxT = 0;
                    for (String threadName : threadPool.keySet()){
                        int curZ = threadPool.get(threadName).imagePlus.getNSlices();
                        int curT = threadPool.get(threadName).imagePlus.getNFrames();
                        if (!threadName.equals("Lab") && submission.simImageRange.zEnd - submission.simImageRange.zStart == 0){
                            curZ = 1;
                        }
                        maxZ = Math.max(curZ, maxZ);
                        maxT = Math.max(curT, maxT);
                    }
                    dataReductionWriter = new DataReductionWriter(submission, maxT, maxZ);
                    dataReductionWriter.initializeDataSheets();
                    for (String threadName : threadPool.keySet()){
                        threadPool.get(threadName).thread.start();
                    }
                }
            }
        }
    }

    static class ThreadStruct {
        public final SimResultsLoader simResultsLoader;
        public final ImagePlus imagePlus;
        public final AtomicBoolean continueOperation;
        public final Thread thread;
        public ThreadStruct(SimResultsLoader simResultsLoader, AtomicBoolean continueOperation, Thread thread){
            this.simResultsLoader = simResultsLoader;
            this.continueOperation = continueOperation;
            this.thread = thread;
            this.imagePlus = simResultsLoader.getImagePlus();
        }

        public ThreadStruct(ImagePlus imagePlus, AtomicBoolean continueOperation, Thread thread){
            this.imagePlus = imagePlus;
            this.simResultsLoader = null;
            this.continueOperation = continueOperation;
            this.thread = thread;
        }
    }
}
