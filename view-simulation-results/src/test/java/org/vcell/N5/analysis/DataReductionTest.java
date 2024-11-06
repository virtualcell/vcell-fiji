package org.vcell.N5.analysis;

import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.io.RoiDecoder;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.vcell.N5.N5ImageHandler;
import org.vcell.N5.retrieving.LoadingManager;
import org.vcell.N5.retrieving.SimResultsLoader;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

public class DataReductionTest {
    // First two are SimROI, last two are LabROI
    private final double[][] labMeans2D = new double[][]{{6, 0, 18, 0}, {6.489, 0, 16.341, 0},
            {7.247, 0, 14.469, 0}}; //calculated through IJ measurement tool

    private final double[][] threeDMeans = new double[][]{{2884.526, 3102.159}, {3884.279, 4668.205}, {5016.744, 5524.792}, {4794.329, 5624.351},
            {4559.778, 5510.099}
    };

    private File getTestResourceFiles(String filePath){
        try {
            URL url = ClassLoader.getSystemClassLoader().getResource(filePath);
            return new File(url.toURI().getPath());
        }
        catch (URISyntaxException e){
            throw new RuntimeException(e);
        }
    }

    @BeforeClass
    public static void init(){
        N5ImageHandler.initializeLogService();
        SimResultsLoader.s3ClientBuilder = AmazonS3ClientBuilder.standard();
        N5ImageHandler.loadingManager = new LoadingManager();
    }

    private void compareExpectedCalculations(ImagePlus imagePlus, ArrayList<Roi> roiList, double[][] expectedResults){
        DataReductionGUI.DataReductionSubmission dataReductionSubmission = new DataReductionGUI.DataReductionSubmission(
                false, roiList, roiList, imagePlus,4, null);
        compareExpectedCalculations(imagePlus, roiList, expectedResults, dataReductionSubmission);
    }

    private void compareExpectedCalculations(ImagePlus imagePlus, ArrayList<Roi> roiList, double[][] expectedResults,
                                             DataReductionGUI.DataReductionSubmission dataReductionSubmission){
        DataReduction dataReduction = new DataReduction(dataReductionSubmission);
        DataReduction.ReducedData reducedData = new DataReduction.ReducedData(imagePlus.getNFrames() * imagePlus.getNSlices(),
                imagePlus.getNChannels() * roiList.size(), SelectMeasurements.AvailableMeasurements.AVERAGE);
        SelectSimRange.RangeOfImage entireRange = new SelectSimRange.RangeOfImage(1, imagePlus.getNFrames(), 1, imagePlus.getNSlices(),
                1, imagePlus.getNChannels());
        HashMap<String, Double> norms = dataReduction.calculateNormalValue(imagePlus, 1, 1, roiList, entireRange);
        DataReduction.ReducedData result = dataReduction.calculateMean(imagePlus, roiList, norms, reducedData, null, entireRange);
        for (int r = 0; r < expectedResults.length; r++){
            for (int c = 0; c < expectedResults[r].length; c++){
                Assert.assertEquals(expectedResults[r][c], result.data[r][c], 0.0009);
            }
        }
    }

    @Test
    public void testMean2DCalculation(){
        // Ensure the mean calculated for each ROI, and each time point is what's to be expected
        SimResultsLoader simResultsLoader = new SimResultsLoader("https://vcell.cam.uchc.edu/n5Data/ezequiel23/ddf7f4f0c77dffd.n5?dataSetName=4864003788", "test1");
        ImagePlus labResultImage2D = simResultsLoader.getImagePlus();

        Roi labRoi = RoiDecoder.open(getTestResourceFiles("ROIs/Lab ROI.roi").getAbsolutePath());
        Roi simROI = RoiDecoder.open(getTestResourceFiles("ROIs/Sim ROI.roi").getAbsolutePath());
        ArrayList<Roi> roiList = new ArrayList<Roi>(){{add(labRoi); add(simROI);}};
        compareExpectedCalculations(labResultImage2D, roiList, labMeans2D);
    }

    @Test
    public void testMean3DCalculation(){
        ImagePlus mitosis = new ImagePlus(getTestResourceFiles("mitosis.tif").getAbsolutePath());
        Roi mitosisROI = RoiDecoder.open(getTestResourceFiles("ROIs/Mitosis Center.roi").getAbsolutePath());
        ArrayList<Roi> roiList = new ArrayList<Roi>(){{add(mitosisROI);}};
        compareExpectedCalculations(mitosis, roiList, threeDMeans);
    }

    @Test
    public void testMeanAndNormalization2DCalculation(){
        SimResultsLoader simResultsLoader = new SimResultsLoader("https://vcell.cam.uchc.edu/n5Data/ezequiel23/ddf7f4f0c77dffd.n5?dataSetName=4864003788", "test1");
        ImagePlus labResultImage2D = simResultsLoader.getImagePlus();
        Roi labRoi = RoiDecoder.open(getTestResourceFiles("ROIs/Lab ROI.roi").getAbsolutePath());
        Roi simROI = RoiDecoder.open(getTestResourceFiles("ROIs/Sim ROI.roi").getAbsolutePath());
        ArrayList<Roi> roiList = new ArrayList<Roi>(){{add(labRoi); add(simROI);}};
        HashMap<String, Double> normValues = new HashMap<>();

        for (Roi roi: roiList){
            labResultImage2D.setRoi(roi);
            for (int c = 1; c <= labResultImage2D.getNChannels(); c++){
                labResultImage2D.setC(c);
                normValues.put(roi.getName() + c, labResultImage2D.getStatistics().mean);
            }
        }
        double[][] normalizedValues = new double[labMeans2D.length][labMeans2D[0].length];

        for (int r = 0; r < labMeans2D.length; r++){
            for (int c =0; c < labMeans2D[0].length; c++){
                String roiName = c < 2 ? labRoi.getName() : simROI.getName();
                double normValue = c % 2 == 0 ? normValues.get(roiName + 1) : normValues.get(roiName + 2);
                normalizedValues[r][c] = labMeans2D[r][c] / normValue;
            }
        }

        DataReductionGUI.DataReductionSubmission dataReductionSubmission = new DataReductionGUI.DataReductionSubmission(
                true, roiList, roiList, labResultImage2D, 1, 1, 1, 1,4, null);

        compareExpectedCalculations(labResultImage2D, roiList, normalizedValues, dataReductionSubmission);
    }
}
