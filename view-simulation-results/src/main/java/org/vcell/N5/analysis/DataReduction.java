package org.vcell.N5.analysis;

import com.opencsv.CSVWriter;
import ij.ImagePlus;
import ij.gui.Roi;
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

    private final double normalPreChange;
    private final double normalPostChange;
    private final int timeWhenChangeOccurs;

    private int numOfSimImages;

    /*
Open an image from file (assume 2D for now).
Open a saved ROI (or allow one to be created and saved).
Apply the ROI to a selected channel(s) in the image.
collect the average intensity value for all times in the image.
Save to cvs file of some kind (something that can be imported into excel).
Open a set of N5 exported files- in reality these will likely be scanned parameters all from the same "simulation".  From each N5 data set, select the appropriate channel and z plane.
Apply the same ROI used previously and measure average intensity for each time.
save data to the cvs or equivalent file.  Ideally, these should be organized so they are easy to use excel.
If possible would like to compare N5 data to image data using a least squares analysis.  Users should be able to see the summed square differences and have the "best fit" identified.
Would be extra nice to plot the experimental data vs the "best fit" VCell simulation.
     */

    // GUI:

    // Create ROI beforehand, and have the user select what they want for ROI in addition to actual Image
    // For the N5 files that will be opened up, select the range for what you want to do analysis on
    // Select the type of statistical analysis, average intensity or something else (idk yet)
    // Take results and place them within a spreadsheet
    // Create a graph from that spreadsheet

    public DataReduction(boolean normalizePostChange, boolean normalizeWithPreChangeValues,
                         ArrayList<Roi> arrayOfSimRois, ArrayList<Roi> arrayOfLabRois,
                         ImagePlus labResults, int startPreChange, int endPreChange,
                         int timeWhenChangeOccurs, int numOfSimImages, File fileToSaveResultsTo){
        this.arrayOfSimRois = arrayOfSimRois;
        this.timeWhenChangeOccurs = timeWhenChangeOccurs;
        this.numOfSimImages = numOfSimImages;
        this.file = fileToSaveResultsTo;
        ArrayList<String> headers = new ArrayList<String>(){{add("Time Frame");}};
        csvMatrix.add(headers);

        normalPreChange = normalizeWithPreChangeValues ? calculateNormalValue(labResults, startPreChange, endPreChange) : Double.MIN_NORMAL;
        normalPostChange = normalizePostChange ? calculateNormalValue(labResults, timeWhenChangeOccurs, timeWhenChangeOccurs) : Double.MIN_NORMAL;

        HashMap<String, ArrayList<Double>> reducedData = calculateMean(labResults, arrayOfLabRois);
        synchronized (csvMatrixLock){
            for (int t = 0; t < labResults.getNFrames(); t++){
                ArrayList<String> rowForTime = new ArrayList<>();
                rowForTime.add(String.valueOf(t));
                csvMatrix.add(rowForTime);
            }

        }
        addValuesToCSVMatrix(labResults, reducedData);

    }

    private void addValuesToCSVMatrix(ImagePlus imagePlus, HashMap<String, ArrayList<Double>> reducedData){
        synchronized (csvMatrixLock){
            csvMatrix.get(0).add("");
            csvMatrix.get(0).add(imagePlus.getTitle());
            for (String roiName: reducedData.keySet()){
                csvMatrix.get(0).add(roiName);
                for(int t = 0; t < imagePlus.getNFrames(); t++){
                    double mean = reducedData.get(roiName).get(t);
                    csvMatrix.get(t + 1).add(String.valueOf(mean));
                }
            }
            numOfSimImages -= 1;
            if (numOfSimImages == 0){
                writeCSVMatrix();
            }
        }
    }

    private double calculateNormalValue(ImagePlus imagePlus, int startT, int endT){
        double normal = 0;
        for (int k = startT; k <= endT; k++){
            imagePlus.setT(k);
            normal += imagePlus.getProcessor().getStatistics().mean;
        }
        normal = normal / (endT - startT);
        return normal;
    }



    private HashMap<String, ArrayList<Double>> calculateMean(ImagePlus imagePlus, ArrayList<Roi> roiList){
//        ResultsTable resultsTable = new ResultsTable();

        HashMap<String,ArrayList<Double>> roiListOfMeans = new HashMap<>();
        for (Roi roi: roiList) {
            imagePlus.setRoi(roi);
            ArrayList<Double> meanValues = new ArrayList<>();
            for (int t = 0; t < imagePlus.getNFrames(); t++){
//                Analyzer analyzer = new Analyzer(imagePlus, Analyzer.MEAN, resultsTable);
//                analyzer.measure();
//
//                double meanValue = resultsTable.getValueAsDouble(resultsTable.getColumnIndex("Mean"), 0);
                double meanValue = imagePlus.getProcessor().getStatistics().mean;
                if (normalPreChange != Double.MIN_NORMAL){
                    meanValue = meanValue / normalPreChange;
                }
                if (normalPostChange != Double.MIN_NORMAL && t >= timeWhenChangeOccurs){
                    meanValue = meanValue / normalPostChange;
                }
                meanValues.add(meanValue);
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
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void simIsLoading(int itemRow, String exportID) {

    }

    @Override
    public void simFinishedLoading(int itemRow, String exportID, ImagePlus imagePlus) {

        HashMap<String, ArrayList<Double>> calculations = calculateMean(imagePlus, arrayOfSimRois);
        addValuesToCSVMatrix(imagePlus, calculations);
    }
}




