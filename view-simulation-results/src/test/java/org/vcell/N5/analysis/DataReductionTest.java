package org.vcell.N5.analysis;

import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.io.RoiDecoder;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.vcell.N5.N5ImageHandler;
import org.vcell.N5.retrieving.SimResultsLoader;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

public class DataReductionTest {
    private final double[] labMeans2DLabROI = new double[]{6, 0, 6.489, 0, 7.247, 0}; //calculated through IJ measurement tool
    private final double[] labMeans2DSimROI = new double[]{18, 0, 16.341, 0, 14.469, 0};

    private final double[] threeDMeans = new double[]{2884.526,3102.159, 3884.279, 4668.205, 5016.744, 5524.792, 4794.329, 5624.351,
            4559.778, 5510.099
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
    }

    private void compareExpectedCalculations(ImagePlus imagePlus, ArrayList<Roi> roiList, HashMap<String, double[]> expectedResults){
        DataReductionGUI.DataReductionSubmission dataReductionSubmission = new DataReductionGUI.DataReductionSubmission(
                false, roiList, roiList, imagePlus,4, null);
        DataReduction dataReduction = new DataReduction(dataReductionSubmission);
        HashMap<String, ArrayList<Double>> result = dataReduction.calculateMean(imagePlus, roiList, Double.MIN_VALUE);
        for (Roi roi : roiList){
            for (int i = 0; i < expectedResults.get(roi.getName()).length; i++){
                Assert.assertEquals(expectedResults.get(roi.getName())[i], result.get(roi.getName()).get(i), 0.0009);
            }
        }
    }

    @Test
    public void testMean2DCalculation(){
        // Ensure the mean calculated for each ROI, and each time point is what's to be expected
        SimResultsLoader simResultsLoader = new SimResultsLoader("https://vcell.cam.uchc.edu/n5Data/ezequiel23/ddf7f4f0c77dffd.n5?dataSetName=4864003788", "test1");
        simResultsLoader.createS3ClientAndReader();
        ImagePlus labResultImage2D = simResultsLoader.getImgPlusFromN5File();

        Roi labRoi = RoiDecoder.open(getTestResourceFiles("ROIs/Lab ROI.roi").getAbsolutePath());
        Roi simROI = RoiDecoder.open(getTestResourceFiles("ROIs/Sim ROI.roi").getAbsolutePath());
        ArrayList<Roi> roiList = new ArrayList<Roi>(){{add(labRoi); add(simROI);}};
        HashMap<String, double[]> expectedResults = new HashMap<String, double[]>(){{put(labRoi.getName(), labMeans2DLabROI); put(simROI.getName(), labMeans2DSimROI);}};
        compareExpectedCalculations(labResultImage2D, roiList, expectedResults);
    }

    @Test
    public void testMean3DCalculation(){
        ImagePlus mitosis = new ImagePlus(getTestResourceFiles("mitosis.tif").getAbsolutePath());
        Roi mitosisROI = RoiDecoder.open(getTestResourceFiles("ROIs/Mitosis Center.roi").getAbsolutePath());
        ArrayList<Roi> roiList = new ArrayList<Roi>(){{add(mitosisROI);}};
        HashMap<String, double[]> expectedResults = new HashMap<String, double[]>(){{put(mitosisROI.getName(), threeDMeans);}};
        compareExpectedCalculations(mitosis, roiList, expectedResults);
    }
}
