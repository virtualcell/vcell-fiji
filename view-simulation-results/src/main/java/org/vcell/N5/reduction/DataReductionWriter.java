package org.vcell.N5.reduction;

import com.google.gson.internal.LinkedTreeMap;
import com.opencsv.CSVWriter;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Roi;
import org.vcell.N5.ExportDataRepresentation;
import org.vcell.N5.N5ImageHandler;
import org.vcell.N5.UI.ControlButtonsPanel;
import org.vcell.N5.UI.MainPanel;
import org.vcell.N5.UI.N5ExportTable;
import org.vcell.N5.reduction.DTO.RangeOfImage;
import org.vcell.N5.retrieving.SimLoadingListener;
import org.vcell.N5.retrieving.SimResultsLoader;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class DataReductionWriter implements SimLoadingListener {
    private final ArrayList<Roi> arrayOfSimRois;

    private final Object csvMatrixLock = new Object();
    private final Object metaDataLock = new Object();
    private final File file;

    private int numOfCalculationsTimesNumImages;
    private final ReductionCalculations calculations;

    public final DataReductionGUI.DataReductionSubmission submission;

    private final ArrayList<ArrayList<String>> averageMatrix = new ArrayList<>();
    private final ArrayList<ArrayList<String>> standardDivMatrix = new ArrayList<>();
    private final ArrayList<ArrayList<String>> metaDataSheet = new ArrayList<>();

    private final ConcurrentHashMap<String, ThreadStruct> threadPool = new ConcurrentHashMap<>();
    private final Object threadPoolLock = new Object();

    private final HashMap<SelectMeasurements.AvailableMeasurements, ArrayList<ArrayList<String>>> sheetsAvailable = new HashMap<SelectMeasurements.AvailableMeasurements, ArrayList<ArrayList<String>>>(){{
        put(SelectMeasurements.AvailableMeasurements.AVERAGE, averageMatrix);
        put(SelectMeasurements.AvailableMeasurements.STD_DEV, standardDivMatrix);
    }};
    private final HashMap<SelectMeasurements.AvailableMeasurements, Integer> columnsForSheets = new HashMap<>();

    private int metaDataParameterCol = 5;
    private final HashMap<String, Integer> parameterNameToCol = new HashMap<>();

    private final ArrayList<SelectMeasurements.AvailableMeasurements> selectedMeasurements;

    private int maxZ;
    private int maxT;

    // Per Image
    static class ReducedData{
        public final double[][] data;
        public final ArrayList<String> columnHeaders;
        public final SelectMeasurements.AvailableMeasurements measurementType;
        public final RangeOfImage rangeOfImage;
        public final int colLen;
        public ReducedData(RangeOfImage rangeOfImage, int colLen, SelectMeasurements.AvailableMeasurements measurementType){
            int nFrames = rangeOfImage.timeEnd - rangeOfImage.timeStart + 1;
            int nSlices = rangeOfImage.zEnd - rangeOfImage.zStart + 1;
            data = new double[nFrames * nSlices][colLen]; // row - col
            columnHeaders = new ArrayList<>();
            this.measurementType = measurementType;
            this.colLen = colLen;
            this.rangeOfImage = rangeOfImage;
        }
    }

    ///////////////////////////////////////
    // Initialize Sheet and Lab results //
    /////////////////////////////////////

    public DataReductionWriter(DataReductionGUI.DataReductionSubmission submission){
        N5ImageHandler.loadingManager.addSimLoadingListener(this);
        this.submission = submission;
        this.selectedMeasurements = submission.selectedMeasurements;

        this.arrayOfSimRois = submission.arrayOfSimRois;
        this.numOfCalculationsTimesNumImages = (submission.numOfSimImages + 1) * submission.selectedMeasurements.size(); // Plus one for the lab image
        this.file = submission.fileToSaveResultsTo;
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

    private void initializeDataSheets(){
        ArrayList<String> headers = new ArrayList<String>(){{add("Time Frame");}};
        boolean is3D = maxZ > 1;
        if (is3D){
            headers.add("Z Index");
        }

        // Add Time and Z-Index Columns
        for (SelectMeasurements.AvailableMeasurements measurement : selectedMeasurements){
            ArrayList<ArrayList<String>> dataSheet = sheetsAvailable.get(measurement);
            dataSheet.add(new ArrayList<>());
            ArrayList<String> headerRow = dataSheet.get(0);

            for (int i = 0; i < headers.size(); i++){
                headerRow.add(i, headers.get(i));
            }
            columnsForSheets.put(measurement, headers.size() + 1);
        }

        metaDataSheet.add(new ArrayList<>());
        metaDataSheet.get(0).add("");
        metaDataSheet.get(0).add("BioModel Name");
        metaDataSheet.get(0).add("Application Name");
        metaDataSheet.get(0).add("Simulation Name");
        metaDataSheet.get(0).add("N5 URL");

        // Fill in Time and Z-Index Columns with selected range

        for (SelectMeasurements.AvailableMeasurements measurement : selectedMeasurements){
            ArrayList<ArrayList<String>> dataSheet = sheetsAvailable.get(measurement);
            for (int t = 1; t <= maxT; t++){
                for (int z = 1; z <= maxZ; z++){
                    ArrayList<String> pointRow = new ArrayList<>();
                    pointRow.add(0, String.valueOf(t));
                    if (is3D){
                        pointRow.add(1, String.valueOf(z));
                    }
                    dataSheet.add(pointRow);
                }
            }
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

        int nChannels = imageRange.channelEnd - imageRange.channelStart + 1;

        ArrayList<ReducedData> reducedDataArrayList = new ArrayList<>();
        for (SelectMeasurements.AvailableMeasurements measurement : submission.selectedMeasurements){
            ReducedData reducedData = new ReducedData(imageRange, nChannels * arrayOfSimRois.size(), measurement);
            reducedDataArrayList.add(reducedData);
            calculations.addAppropriateHeaders(imagePlus, rois, imageRange, reducedData, channelInfo);
        }
        AtomicBoolean continueOperation = threadPool.get(threadName).continueOperation;
        calculations.calculateStatistics(imagePlus, rois, normValue, reducedDataArrayList, imageRange, continueOperation);
        for (ReducedData reducedData: reducedDataArrayList){
            if (continueOperation.get()){
                addValuesToWideCSVMatrix(reducedData);
            }
        }
    }

    private void addValuesToWideCSVMatrix(ReducedData reducedData){
        synchronized (csvMatrixLock){
            ArrayList<ArrayList<String>> dataSheet = sheetsAvailable.get(reducedData.measurementType);
            int colIndex = columnsForSheets.get(reducedData.measurementType);
            fillWithEmptySpace(dataSheet.get(0), colIndex);
            for (int c = 0; c < reducedData.columnHeaders.size(); c++){
                dataSheet.get(0).add(colIndex, reducedData.columnHeaders.get(c));
            }
            RangeOfImage rangeOfImage = reducedData.rangeOfImage;
            int tzCounter = 1;
            for (int t = 1; t <= maxT; t++){
                for (int z = 1; z <= maxZ; z++){
                    boolean inBetweenTime = t <= rangeOfImage.timeEnd && rangeOfImage.timeStart <= t;
                    boolean inBetweenZ = z <= rangeOfImage.zEnd && rangeOfImage.zStart <= z;
                    ArrayList<String> row = dataSheet.get(tzCounter);
                    fillWithEmptySpace(row, colIndex);
                    for (int c = 0; c < reducedData.colLen; c++){
                        if (inBetweenTime && inBetweenZ){
                            int dataRow = ((t - rangeOfImage.timeStart) * (z - rangeOfImage.zStart)) + (z - rangeOfImage.zStart);
                            double mean = reducedData.data[dataRow][c];
                            row.add(String.valueOf(mean));
                        }
                    }
                    tzCounter += 1;
                }
            }
            numOfCalculationsTimesNumImages -= 1;
            colIndex += 1 + reducedData.data[0].length;
            columnsForSheets.replace(reducedData.measurementType, colIndex);
            if (numOfCalculationsTimesNumImages == 0){
                writeCSVMatrix();
            }
        }
    }

    // If parameter is not in list of parameters, add new column. If simulation does not have parameter say "not-present"
    private void addMetaData(SimResultsLoader loadedResults){
        synchronized (metaDataLock){
            N5ExportTable n5ExportTable = MainPanel.n5ExportTable;
            ExportDataRepresentation.SimulationExportDataRepresentation data = n5ExportTable.n5ExportTableModel.getRowData(loadedResults.rowNumber);
            ArrayList<String> newMetaData = new ArrayList<>();
            newMetaData.add(loadedResults.userSetFileName);
            newMetaData.add(data.biomodelName);
            newMetaData.add(data.applicationName);
            newMetaData.add(data.simulationName);
            newMetaData.add(data.uri);
            ArrayList<String> parameterValues = data.differentParameterValues;
            for (String s : parameterValues){
                String[] tokens = s.split(":");
                String colValue = tokens[1] + ":" + tokens[2];
                if (parameterNameToCol.containsKey(tokens[0])){
                    int col = parameterNameToCol.get(tokens[0]);
                    fillWithEmptySpace(newMetaData, col);
                    newMetaData.add(col, colValue);
                } else{
                    metaDataSheet.get(0).add(tokens[0] + " (Default:Set Value)");
                    fillWithEmptySpace(newMetaData, metaDataParameterCol);
                    newMetaData.add(metaDataParameterCol, colValue);
                    parameterNameToCol.put(tokens[0], metaDataParameterCol);
                    metaDataParameterCol += 1;
                }
            }
            metaDataSheet.add(newMetaData);
        }
    }

    // If specific entry to be added isn't in array list length, add empty space until it is
    private void fillWithEmptySpace(ArrayList<String> arrayList, int col){
        while (arrayList.size() < col){
            arrayList.add("");
        }
    }

//    // Z changes the fastest
//    private ArrayList<ArrayList<String>> fillWithEmptySpace(ReducedData reducedData){
//        int t = 1;
//        int z = 1;
//        ArrayList<ArrayList<String>> paddedInfo = new ArrayList<>();
//        while (reducedData.numFrames != t){
//            while (reducedData.numSlices != z){
//
//            }
//            t += 1;
//        }
//    }

    private void writeCSVMatrix(){

        try {
            for (SelectMeasurements.AvailableMeasurements measurements : sheetsAvailable.keySet()){
                if (!sheetsAvailable.get(measurements).isEmpty()){
                    File currentFile = new File(file.getAbsolutePath() + "-" + measurements.publicName + ".csv");
                    try (FileWriter fileWriter = new FileWriter(currentFile)){
                        CSVWriter csvWriter = new CSVWriter(fileWriter);
                        for (ArrayList<String> row : sheetsAvailable.get(measurements)){
                            csvWriter.writeNext(row.toArray(new String[0]));
                        }
                    }
                }
            }
            File currentFile = new File(file.getAbsolutePath() + "-Metadata.csv");
            try (FileWriter fileWriter = new FileWriter(currentFile)){
                CSVWriter csvWriter = new CSVWriter(fileWriter);
                for (ArrayList<String> row : metaDataSheet){
                    csvWriter.writeNext(row.toArray(new String[0]));
                }
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            N5ImageHandler.loadingManager.removeFromSimLoadingListener(this);
            MainPanel.controlButtonsPanel.updateButtonsToMatchState(false, ControlButtonsPanel.PanelState.NOTHING_OR_LOADING_IMAGE);
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
                addMetaData(loadedResults);
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
                    maxZ = 0;
                    maxT = 0;
                    for (String threadName : threadPool.keySet()){
                        int curZ = threadPool.get(threadName).imagePlus.getNSlices();
                        int curT = threadPool.get(threadName).imagePlus.getNFrames();
                        maxZ = Math.max(curZ, maxZ);
                        maxT = Math.max(curT, maxT);
                    }
                    initializeDataSheets();
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





