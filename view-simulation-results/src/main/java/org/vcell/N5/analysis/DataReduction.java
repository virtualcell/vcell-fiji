package org.vcell.N5.analysis;

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
import org.vcell.N5.retrieving.SimLoadingListener;
import org.vcell.N5.retrieving.SimResultsLoader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;

public class DataReduction implements SimLoadingListener {
    private final ArrayList<Roi> arrayOfSimRois;

    private final Object csvMatrixLock = new Object();
    private final Object metaDataLock = new Object();
    private File file;

    private int numOfImagesToBeOpened;
    private final boolean normalize;

    public final DataReductionGUI.DataReductionSubmission submission;

    private final Workbook workbook = new HSSFWorkbook();
    private final Sheet dataSheet = workbook.createSheet("Average");
    private final Sheet metaDataSheet = workbook.createSheet("Metadata");

    private int colIndex;
    private int metaDataRow = 1;
    private int metaDataParameterCol = 5;
    private final HashMap<String, Integer> parameterNameToCol = new HashMap<>();

    private final SelectSimRange.RangeOfImage simRange;

    // Per Image
    static class ReducedData{
        public final double[][] data;
        public final ArrayList<String> columnHeaders;
        public final DataReductionGUI.AvailableMeasurements measurementType;
        public ReducedData(int rowLen, int colLen, DataReductionGUI.AvailableMeasurements measurementType){
            data = new double[rowLen][colLen];
            columnHeaders = new ArrayList<>();
            this.measurementType = measurementType;
        }
    }

    //

    // GUI:

    // Create ROI beforehand, and have the user select what they want for ROI in addition to actual Image
    // For the N5 files that will be opened up, select the range for what you want to do analysis on
    // Select the type of statistical analysis, average intensity or something else (idk yet)
    // Take results and place them within a spreadsheet
    // Create a graph from that spreadsheet

    public DataReduction(DataReductionGUI.DataReductionSubmission submission){
        this.submission = submission;
        this.arrayOfSimRois = submission.arrayOfSimRois;
        this.numOfImagesToBeOpened = submission.numOfSimImages + 1; // Plus one for the lab image
        this.file = submission.fileToSaveResultsTo;
        this.normalize = submission.normalizeMeasurementsBool;
        this.simRange = submission.simImageRange;

        ArrayList<String> headers = new ArrayList<String>(){{add("Time Frame");}};
        boolean is3D = submission.labResults.getNSlices() > 1;
        if (is3D){
            headers.add("Z Index");
        }

        Row headerRow = dataSheet.createRow(0);
        for (int i = 0; i < headers.size(); i++){
            headerRow.createCell(i).setCellValue(headers.get(i));
        }
        colIndex = headers.size() + 1;

        double normValue = calculateNormalValue(submission.labResults, submission.imageStartPointNorm, submission.imageEndPointNorm);

        int nFrames = submission.experimentImageRange.timeEnd - submission.experimentImageRange.timeStart + 1;
        int nSlices = submission.experimentImageRange.zEnd - submission.experimentImageRange.zStart + 1;
        int nChannels = submission.experimentImageRange.channelEnd - submission.experimentImageRange.channelStart + 1;
        ReducedData reducedData = new ReducedData(nFrames * nSlices, nChannels * arrayOfSimRois.size(), DataReductionGUI.AvailableMeasurements.AVERAGE);
        reducedData = calculateMean(submission.labResults, submission.arrayOfLabRois, normValue, reducedData, submission.experimentImageRange);
        synchronized (csvMatrixLock){
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
        addValuesToCSVMatrix(reducedData);
        synchronized (metaDataLock){
            metaDataSheet.createRow(0).createCell(1).setCellValue("BioModel Name");
            metaDataSheet.getRow(0).createCell(2).setCellValue("Application Name");
            metaDataSheet.getRow(0).createCell(3).setCellValue("Simulation Name");
            metaDataSheet.getRow(0).createCell(4).setCellValue("N5 URL");
        }
    }

    private void addValuesToCSVMatrix(ReducedData reducedData){
        synchronized (csvMatrixLock){
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
            if (numOfImagesToBeOpened == 0){
                writeCSVMatrix();
            }
        }
    }

    double calculateNormalValue(ImagePlus imagePlus, int startT, int endT){
        if (normalize){
            double normal = 0;
            for (int k = startT; k <= endT; k++){
                imagePlus.setT(k);
                normal += imagePlus.getProcessor().getStatistics().mean;
            }
            normal = normal / (endT - startT + 1); // inclusive of final point
            return normal;
        } else {
            return Double.MIN_NORMAL;
        }

    }


    ReducedData calculateMean(ImagePlus imagePlus, ArrayList<Roi> roiList,
                              double normalizationValue, ReducedData reducedData){
        SelectSimRange.RangeOfImage entireRange = new SelectSimRange.RangeOfImage(1, imagePlus.getNFrames(), 1, imagePlus.getNSlices(),
                1, imagePlus.getNChannels());
        return calculateMean(imagePlus, roiList, normalizationValue, reducedData, null, entireRange);
    }

    ReducedData calculateMean(ImagePlus imagePlus, ArrayList<Roi> roiList,
                              double normalizationValue, ReducedData reducedData, SelectSimRange.RangeOfImage rangeOfImage){
        return calculateMean(imagePlus, roiList, normalizationValue, reducedData, null, rangeOfImage);
    }

    ReducedData calculateMean(ImagePlus imagePlus, ArrayList<Roi> roiList,
                              double normalizationValue, ReducedData reducedData,
                              LinkedTreeMap<String, LinkedTreeMap<String, String>> channelInfo, SelectSimRange.RangeOfImage rangeOfImage){
        int roiCounter = 0;
        for (Roi roi: roiList) {
            imagePlus.setRoi(roi);
            for (int c = rangeOfImage.channelStart; c <= rangeOfImage.channelEnd; c++){ //Last channel is domain channel, not variable
                String stringC = String.valueOf(c - 1);
                String channelName = channelInfo != null && channelInfo.containsKey(stringC) ? channelInfo.get(stringC).get("Name") : String.valueOf(c);
                reducedData.columnHeaders.add(imagePlus.getTitle() + ":" + roi.getName() + ":" + channelName);
            }
            int tzCounter = 0;
            for (int t = rangeOfImage.timeStart; t <= rangeOfImage.timeEnd; t++){
                for (int z = rangeOfImage.zStart; z <= rangeOfImage.zEnd; z++){
                    for (int c = rangeOfImage.channelStart; c <= rangeOfImage.channelEnd; c++){
                        int channelSize = rangeOfImage.channelEnd - rangeOfImage.channelStart + 1;
                        imagePlus.setPosition(c, z, t);
                        double meanValue = imagePlus.getStatistics().mean;
                        if (normalize){
                            meanValue = meanValue / normalizationValue;
                        }
                        reducedData.data[tzCounter][c - 1 + (roiCounter * channelSize)] = meanValue;
                    }
                    tzCounter += 1;
                }
            }
            roiCounter += 1;
        }
        return reducedData;
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
        ImagePlus imagePlus = loadedResults.getImagePlus();
        double normValue = calculateNormalValue(imagePlus, submission.simStartPointNorm, submission.simEndPointNorm);
        ReducedData reducedData = new ReducedData(imagePlus.getNFrames() * imagePlus.getNSlices(),
                simRange.channelEnd - simRange.channelStart + 1, DataReductionGUI.AvailableMeasurements.AVERAGE);
        reducedData = calculateMean(imagePlus, arrayOfSimRois, normValue, reducedData, loadedResults.getChannelInfo(), simRange);
        addMetaData(loadedResults);
        addValuesToCSVMatrix(reducedData);
    }
}





