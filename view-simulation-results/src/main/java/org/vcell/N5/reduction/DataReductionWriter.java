package org.vcell.N5.reduction;

import com.google.gson.internal.LinkedTreeMap;
import ij.ImagePlus;
import ij.gui.Roi;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.vcell.N5.ExportDataRepresentation;
import org.vcell.N5.N5ImageHandler;
import org.vcell.N5.UI.MainPanel;
import org.vcell.N5.UI.N5ExportTable;
import org.vcell.N5.reduction.DTO.RangeOfImage;
import org.vcell.N5.retrieving.SimLoadingListener;
import org.vcell.N5.retrieving.SimResultsLoader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;

public class DataReductionWriter implements SimLoadingListener {
    private final ArrayList<Roi> arrayOfSimRois;

    private final Object csvMatrixLock = new Object();
    private final Object metaDataLock = new Object();
    private File file;

    private int numOfImagesToBeOpened;
    private final ReductionCalculations calculations;

    public final DataReductionGUI.DataReductionSubmission submission;

    private final Workbook workbook = new HSSFWorkbook();
    private final HashMap<SelectMeasurements.AvailableMeasurements, Sheet> sheetsAvailable = new HashMap<SelectMeasurements.AvailableMeasurements, Sheet>(){{
        put(SelectMeasurements.AvailableMeasurements.AVERAGE, workbook.createSheet("Average"));
        put(SelectMeasurements.AvailableMeasurements.STD_DEV, workbook.createSheet("Standard Deviation"));
    }};
    private final HashMap<SelectMeasurements.AvailableMeasurements, Integer> columnsForSheets = new HashMap<>();
    private final Sheet metaDataSheet = workbook.createSheet("Metadata");

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
            Sheet dataSheet = sheetsAvailable.get(measurement);
            Row headerRow = dataSheet.createRow(0);
            for (int i = 0; i < headers.size(); i++){
                headerRow.createCell(i).setCellValue(headers.get(i));
            }
            columnsForSheets.put(measurement, headers.size() + 1);
        }

        metaDataSheet.createRow(0).createCell(1).setCellValue("BioModel Name");
        metaDataSheet.getRow(0).createCell(2).setCellValue("Application Name");
        metaDataSheet.getRow(0).createCell(3).setCellValue("Simulation Name");
        metaDataSheet.getRow(0).createCell(4).setCellValue("N5 URL");

        // Fill in Time and Z-Index Columns with selected range

        for (SelectMeasurements.AvailableMeasurements measurement : selectedMeasurements){
            Sheet dataSheet = sheetsAvailable.get(measurement);
            int rowI = 1;
            for (int t = submission.experimentImageRange.timeStart; t <= submission.experimentImageRange.timeEnd; t++){
                for (int z = submission.experimentImageRange.zStart; z <= submission.experimentImageRange.zEnd; z++){
                    Row pointRow = dataSheet.createRow(rowI);
                    rowI += 1;
                    pointRow.createCell(0).setCellValue(t);
                    if (is3D){
                        pointRow.createCell(1).setCellValue(z);
                    }
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
            Sheet dataSheet = sheetsAvailable.get(reducedData.measurementType);
            int colIndex = columnsForSheets.get(reducedData.measurementType);
            for (int c = 0; c < reducedData.columnHeaders.size(); c++){
                dataSheet.getRow(0).createCell(colIndex + c).setCellValue(reducedData.columnHeaders.get(c));
            }
            for (int i = 0; i < reducedData.data.length; i++){
                for (int c = 0; c < reducedData.data[i].length; c++){
                    Row row = dataSheet.getRow(i + 1);
                    double mean = reducedData.data[i][c];
                    row.createCell(colIndex + c).setCellValue(mean);
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
            metaDataSheet.createRow(metaDataRow).createCell(0).setCellValue(loadedResults.userSetFileName);
            metaDataSheet.getRow(metaDataRow).createCell(1).setCellValue(data.biomodelName);
            metaDataSheet.getRow(metaDataRow).createCell(2).setCellValue(data.applicationName);
            metaDataSheet.getRow(metaDataRow).createCell(3).setCellValue(data.simulationName);
            metaDataSheet.getRow(metaDataRow).createCell(4).setCellValue(data.uri);
            ArrayList<String> parameterValues = data.differentParameterValues;
            for (String s : parameterValues){
                String[] tokens = s.split(":");
                String colValue = tokens[1] + ":" + tokens[2];
                if (parameterNameToCol.containsKey(tokens[0])){
                    metaDataSheet.getRow(metaDataRow).createCell(parameterNameToCol.get(tokens[0])).setCellValue(colValue);
                } else{
                    metaDataSheet.getRow(0).createCell(metaDataParameterCol).setCellValue(tokens[0] + "\n(Default:Set Value)");
                    metaDataSheet.getRow(metaDataRow).createCell(metaDataParameterCol).setCellValue(colValue);
                    parameterNameToCol.put(tokens[0], metaDataParameterCol);
                    metaDataParameterCol += 1;
                }
            }
            metaDataRow += 1;
        }
    }

    private void writeCSVMatrix(){

        try {
            file = new File(file.getAbsolutePath() + ".xls");
            workbook.write(Files.newOutputStream(file.toPath()));
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





