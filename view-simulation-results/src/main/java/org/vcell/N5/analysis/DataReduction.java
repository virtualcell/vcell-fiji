package org.vcell.N5.analysis;

import com.opencsv.CSVWriter;
import ij.ImagePlus;
import ij.gui.Roi;
import org.vcell.N5.N5ImageHandler;
import org.vcell.N5.UI.MainPanel;
import org.vcell.N5.retrieving.SimLoadingListener;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class DataReduction implements SimLoadingListener {
    private final ArrayList<Roi> arrayOfSimRois;

    private final Object csvMatrixLock = new Object();
    private File file;

    private final ArrayList<ArrayList<String>> csvMatrix = new ArrayList<>();

    private int numOfImagesToBeOpened;
    private final boolean normalize;

    public final DataReductionGUI.DataReductionSubmission submission;

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
        ArrayList<String> headers = new ArrayList<String>(){{add("Time Frame"); add("Z Index"); add("Channel");}};
        csvMatrix.add(headers);

        double normValue = calculateNormalValue(submission.labResults, submission.imageStartPointNorm, submission.imageEndPointNorm);

        HashMap<String, ArrayList<Double>> reducedData = calculateMean(submission.labResults, submission.arrayOfLabRois, normValue);
        synchronized (csvMatrixLock){
            for (int t = 0; t < submission.labResults.getNFrames(); t++){
                for (int z = 0; z < submission.labResults.getNSlices(); z++){
                    for (int c = 0; c < submission.labResults.getNChannels(); c++){
                        ArrayList<String> rowForTime = new ArrayList<>();
                        rowForTime.add(String.valueOf(t));
                        rowForTime.add(String.valueOf(z));
                        rowForTime.add(String.valueOf(c));
                        csvMatrix.add(rowForTime);
                    }
                }
            }

        }
        addValuesToCSVMatrix(submission.labResults, reducedData);

    }

    private void addValuesToCSVMatrix(ImagePlus imagePlus, HashMap<String, ArrayList<Double>> reducedData){
        synchronized (csvMatrixLock){
            csvMatrix.get(0).add("");
            for (String roiName: reducedData.keySet()){
                csvMatrix.get(0).add(imagePlus.getTitle()+" : " + roiName);
                int tN = imagePlus.getNFrames();
                int zN = imagePlus.getNSlices();
                int cN = imagePlus.getNChannels();
                for (int i = 0; i < (tN * zN * cN); i++){
                    double mean = reducedData.get(roiName).get(i);
                    csvMatrix.get(i + 1).add(""); // every array is a row
                    csvMatrix.get(i + 1).add(String.valueOf(mean));
                }
            }
            numOfImagesToBeOpened -= 1;
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
            normal = normal / (endT - startT);
            return normal;
        } else {
            return Double.MIN_NORMAL;
        }

    }



    HashMap<String, ArrayList<Double>> calculateMean(ImagePlus imagePlus, ArrayList<Roi> roiList,
                                                             double normalizationValue){
//        ResultsTable resultsTable = new ResultsTable();

        HashMap<String,ArrayList<Double>> roiListOfMeans = new HashMap<>();
        for (Roi roi: roiList) {
            imagePlus.setRoi(roi);
            ArrayList<Double> meanValues = new ArrayList<>();
            for (int t = 0; t < imagePlus.getNFrames(); t++){
                for (int z = 0; z < imagePlus.getNSlices(); z++){
                    for (int c = 0; c < imagePlus.getNChannels(); c++){
                        imagePlus.setPosition(c + 1, z + 1, t + 1);
                        double meanValue = imagePlus.getStatistics().mean;
                        if (normalize){
                            meanValue = meanValue / normalizationValue;
                        }
                        meanValues.add(meanValue);
                    }
                }
            }
            roiListOfMeans.put(roi.getName(), meanValues);
        }
        return roiListOfMeans;
    }

    private void writeCSVMatrix(){
        try (FileWriter fileWriter = new FileWriter(file)) {
            CSVWriter csvWriter = new CSVWriter(fileWriter);
            for (ArrayList<String> row: csvMatrix){
                csvWriter.writeNext(row.toArray(new String[0]));
            }
            csvWriter.close();
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
    public void simFinishedLoading(int itemRow, String exportID, ImagePlus imagePlus) {
        double normValue = calculateNormalValue(imagePlus, submission.simStartPointNorm, submission.simEndPointNorm);
        HashMap<String, ArrayList<Double>> calculations = calculateMean(imagePlus, arrayOfSimRois, normValue);
        addValuesToCSVMatrix(imagePlus, calculations);
    }
}





