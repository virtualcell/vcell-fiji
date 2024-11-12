package org.vcell.N5.reduction;

import com.google.gson.internal.LinkedTreeMap;
import com.opencsv.CSVWriter;
import ij.ImagePlus;
import ij.gui.Roi;
import org.vcell.N5.ExportDataRepresentation;
import org.vcell.N5.N5ImageHandler;
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

public class DataReductionWriter implements SimLoadingListener {
    private final ArrayList<Roi> arrayOfSimRois;

    private final Object csvMatrixLock = new Object();
    private final Object metaDataLock = new Object();
    private final File file;

    private int numOfImagesToBeOpened;
    private final ReductionCalculations calculations;

    public final DataReductionGUI.DataReductionSubmission submission;

    private final ArrayList<ArrayList<String>> averageMatrix = new ArrayList<>();
    private final ArrayList<ArrayList<String>> standardDivMatrix = new ArrayList<>();
    private final ArrayList<ArrayList<String>> metaDataSheet = new ArrayList<>();

    private final HashMap<SelectMeasurements.AvailableMeasurements, ArrayList<ArrayList<String>>> sheetsAvailable = new HashMap<SelectMeasurements.AvailableMeasurements, ArrayList<ArrayList<String>>>(){{
        put(SelectMeasurements.AvailableMeasurements.AVERAGE, averageMatrix);
        put(SelectMeasurements.AvailableMeasurements.STD_DEV, standardDivMatrix);
    }};
    private final HashMap<SelectMeasurements.AvailableMeasurements, Integer> columnsForSheets = new HashMap<>();

    private int metaDataRow = 1;
    private int metaDataParameterCol = 5;
    private final HashMap<String, Integer> parameterNameToCol = new HashMap<>();

    private final ArrayList<SelectMeasurements.AvailableMeasurements> selectedMeasurements;

    // Per Image
    static class ReducedData{
        public final double[][] data;
        public final ArrayList<String> columnHeaders;
        public final SelectMeasurements.AvailableMeasurements measurementType;
        public ReducedData(int rowLen, int colLen, SelectMeasurements.AvailableMeasurements measurementType){
            data = new double[rowLen][colLen];
            columnHeaders = new ArrayList<>();
            this.measurementType = measurementType;
        }
    }

    ///////////////////////////////////////
    // Initialize Sheet and Lab results //
    /////////////////////////////////////

    public static void createDataReductionProcess(DataReductionGUI.DataReductionSubmission submission){
        new DataReductionWriter(submission);
    }

    public DataReductionWriter(DataReductionGUI.DataReductionSubmission submission){
        N5ImageHandler.loadingManager.addSimLoadingListener(this);
        this.submission = submission;
        this.selectedMeasurements = submission.selectedMeasurements;
        synchronized (csvMatrixLock){
            synchronized (metaDataLock){
                initializeDataSheets();
            }
        }
        this.arrayOfSimRois = submission.arrayOfSimRois;
        this.numOfImagesToBeOpened = (submission.numOfSimImages + 1) * submission.selectedMeasurements.size(); // Plus one for the lab image
        this.file = submission.fileToSaveResultsTo;
        this.calculations = new ReductionCalculations(submission.normalizeMeasurementsBool);



        Thread processLabResults = new Thread(() -> {
            calculateAndAddResults(submission.labResults, submission.experiementNormRange, submission.experimentImageRange,
                    submission.arrayOfLabRois, null);
        }, "Processing Lab Image");
        processLabResults.start();
    }

    private void initializeDataSheets(){
        ArrayList<String> headers = new ArrayList<String>(){{add("Time Frame");}};
        boolean is3D = submission.labResults.getNSlices() > 1;
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
            for (int t = submission.experimentImageRange.timeStart; t <= submission.experimentImageRange.timeEnd; t++){
                for (int z = submission.experimentImageRange.zStart; z <= submission.experimentImageRange.zEnd; z++){
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
                                        LinkedTreeMap<String, LinkedTreeMap<String, String>> channelInfo){
        HashMap<String, Double> normValue = null;
        if (submission.normalizeMeasurementsBool){
            normValue = calculations.calculateNormalValue(imagePlus, normRange, rois, imageRange);
        }

        int nFrames = imageRange.timeEnd - imageRange.timeStart + 1;
        int nSlices = imageRange.zEnd - imageRange.zStart + 1;
        int nChannels = imageRange.channelEnd - imageRange.channelStart + 1;

        ArrayList<ReducedData> reducedDataArrayList = new ArrayList<>();
        for (SelectMeasurements.AvailableMeasurements measurement : submission.selectedMeasurements){
            ReducedData reducedData = new ReducedData(nFrames * nSlices, nChannels * arrayOfSimRois.size(), measurement);
            reducedDataArrayList.add(reducedData);
            calculations.addAppropriateHeaders(imagePlus, rois, imageRange, reducedData, channelInfo);
        }
        calculations.calculateStatistics(imagePlus, rois, normValue, reducedDataArrayList, imageRange);
        for (ReducedData reducedData: reducedDataArrayList){
            addValuesToCSVMatrix(reducedData);
        }
    }

    private void addValuesToCSVMatrix(ReducedData reducedData){
        synchronized (csvMatrixLock){
            ArrayList<ArrayList<String>> dataSheet = sheetsAvailable.get(reducedData.measurementType);
            int colIndex = columnsForSheets.get(reducedData.measurementType);
            fillWithEmptySpace(dataSheet.get(0), colIndex);
            for (int c = 0; c < reducedData.columnHeaders.size(); c++){
                dataSheet.get(0).add(colIndex, reducedData.columnHeaders.get(c));
            }
            for (int i = 0; i < reducedData.data.length; i++){
                for (int c = 0; c < reducedData.data[i].length; c++){
                    ArrayList<String> row = dataSheet.get(i + 1);
                    fillWithEmptySpace(row, colIndex);
                    double mean = reducedData.data[i][c];
                    row.add(String.valueOf(mean));
                }
            }
            numOfImagesToBeOpened -= 1;
            colIndex += 1 + reducedData.data[0].length;
            columnsForSheets.replace(reducedData.measurementType, colIndex);
            if (numOfImagesToBeOpened == 0){
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
            metaDataRow += 1;
        }
    }

    // If specific entry to be added isn't in array list length, add empty space until it is
    private void fillWithEmptySpace(ArrayList<String> arrayList, int col){
        while (arrayList.size() < col){
            arrayList.add("");
        }
    }

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
            File currentFile = new File(file.getAbsolutePath() + "-metadata.csv");
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
            MainPanel.controlButtonsPanel.enableCriticalButtons(true);
        }
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
                        submission.arrayOfSimRois, loadedResults.getChannelInfo());
            }, "Processing Image: " + loadedResults.userSetFileName);
            imageProcessingThread.start();
        }
    }

}





