package org.vcell.N5;

import ij.ImagePlus;
import ij.io.Opener;
import ij.plugin.Duplicator;
import ij.plugin.ImageCalculator;
import ij.process.ImageProcessor;
import org.junit.*;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;


/*
    One test file is only metadata with no actual visual information. Has a bunch of datasets, and these datasets have complex metadata.
    Another one has only one dataset and some visual information which is used to determine whether the data is accurate or not

    S3Proxy Rule does not work properly so I'm just using the native S3 proxy and manually controlling the initialization of the s3 mock server
 */

public class N5ImageHandlerTest {
    private final String n5FileName = "nfive/test_image.n5";
    private final String datasetName = "5DStack";
    public enum stats{
        HISTMAX,
        HISTMIN,
        HISTAVERAGE
    }

    private File getTestResourceFiles(String filePath){
        try {
            URL url = ClassLoader.getSystemClassLoader().getResource(filePath);
            return new File(url.toURI().getPath());
        }
        catch (URISyntaxException e){
            throw new RuntimeException(e);
        }
    }

    @Before
    public void run(){
        N5ImageHandler.initializeLogService();
    }

    @Test
    public void testN5DatasetList() throws IOException {
        SimResultsLoader simResultsLoader = new SimResultsLoader();
        simResultsLoader.setSelectedLocalFile(this.getTestResourceFiles(n5FileName));
        this.dataSetListTest(simResultsLoader.getN5DatasetList());
    }

    @Test
    public void testGettingImgPlus() throws IOException {
        SimResultsLoader simResultsLoader = new SimResultsLoader();
        simResultsLoader.setDataSetChosen(datasetName);
        simResultsLoader.setSelectedLocalFile(this.getTestResourceFiles(n5FileName));
        ImagePlus imagePlus = simResultsLoader.getImgPlusFromLocalN5File();

        fiveDStackTests(imagePlus);
        imagePlus = new Duplicator().run(imagePlus);
        fiveDStackTests(imagePlus);
    }

//    @Test
//    // Create client without creds, with cred no endpoint, endpoint no creds, endpoint and creds, then test whether they can handle images as expected
//    public void testS3Client() throws IOException {
//        HashMap<String, String> s3Endpoint = new HashMap<>();
//        HashMap<String, String> credentials = new HashMap<>();
//
//        s3Endpoint.put("Endpoint", "http://127.0.0.1:4000");
//        s3Endpoint.put("Region", Regions.US_EAST_1.getName());
//
//        credentials.put("AccessKey", "jj");
//        credentials.put("SecretKey", "jj");
//
//        final String s3ProxyURI = "http://localhost:4000/" + this.n5FileName + "?datasetName=" + datasetName;
//
//        SimResultsLoader simResultsLoader = new SimResultsLoader(s3ProxyURI, "");
//
//        // Environment variables are set in github actions VM
//
//        simResultsLoader.createS3Client(null, null);
//        this.remoteN5ImgPlusTests(simResultsLoader);
//
//        simResultsLoader.createS3Client(null, s3Endpoint);
//        this.remoteN5ImgPlusTests(simResultsLoader);
//
////        simResultsLoader.createS3Client(credentials, null);
////        this.remoteN5ImgPlusTests(simResultsLoader);
////
////        simResultsLoader.createS3Client(credentials, s3Endpoint);
////        this.remoteN5ImgPlusTests(simResultsLoader);
//    }

    @Test
    public void testS3AlphaInstance() throws IOException{
        N5DataSetFile[] n5DataSetFiles = N5DataSetFile.alphaTestFiles();
        for(N5DataSetFile n5DataSetFile : n5DataSetFiles) {
            SimResultsLoader simResultsLoader = new SimResultsLoader(n5DataSetFile.uri, "");
            simResultsLoader.createS3Client();
            ImagePlus imagePlus = simResultsLoader.getImgPlusFromN5File();

            //stats that have been preemptively calculated within VCell
            alphaStatsTest(imagePlus, n5DataSetFile, stats.HISTMAX);
            alphaStatsTest(imagePlus, n5DataSetFile, stats.HISTMIN);
            alphaStatsTest(imagePlus, n5DataSetFile, stats.HISTAVERAGE);
        }
    }

    @Test
    public void testS3AlphaInstanceLoadedIntoMemory() throws IOException {
        N5DataSetFile[] n5DataSetFiles = N5DataSetFile.alphaTestFiles();
        for(N5DataSetFile n5DataSetFile : n5DataSetFiles) {
            SimResultsLoader simResultsLoader = new SimResultsLoader(n5DataSetFile.uri, "");
            simResultsLoader.createS3Client();
            ImagePlus imagePlus = simResultsLoader.getImgPlusFromN5File();
            alphaStatsTest(new Duplicator().run(imagePlus), n5DataSetFile, stats.HISTMAX);
            alphaStatsTest(new Duplicator().run(imagePlus), n5DataSetFile, stats.HISTMIN);
            alphaStatsTest(new Duplicator().run(imagePlus), n5DataSetFile, stats.HISTAVERAGE);
        }
    }

    interface PixelCalculations {
        int grabEdge(int w, int h, int w1, int h1);
    }

    @Test
    public void testUnits() throws IOException {
        N5DataSetFile[] n5DataSetFiles = N5DataSetFile.alphaTestFiles();
        for (N5DataSetFile n5DataSetFile: n5DataSetFiles){
            SimResultsLoader simResultsLoader = new SimResultsLoader(n5DataSetFile.uri, "");
            simResultsLoader.createS3Client();
            ImagePlus imagePlus = simResultsLoader.getImgPlusFromN5File();
            double areaOfPixel = imagePlus.getCalibration().getX(1) * imagePlus.getCalibration().getY(1);
            double totalArea = areaOfPixel * imagePlus.getWidth() * imagePlus.getHeight();
            Assert.assertEquals(n5DataSetFile.totalArea, totalArea, 0.0000001);

            totalArea = 0;
            imagePlus.setPosition(imagePlus.getNChannels(), 1, 1);
            ImageProcessor imageProcessor = imagePlus.getProcessor();

            PixelCalculations pixelCalculations = ((w, h, w1, h1) -> {
                int inBounds = (0 <= w1) && (w1 < imagePlus.getWidth()) && (0 <= h1) && (h1 < imagePlus.getHeight()) ? 2 : 1;
                if (inBounds == 2 && imageProcessor.getf(w1, h1) != imageProcessor.getf(w, h)){
                    return inBounds;
                }
                return 1;
            });
            for (int h = 0; h < imagePlus.getHeight(); h++){
                for (int w = 0; w < imagePlus.getWidth(); w++){
                    double sum = pixelCalculations.grabEdge(w, h, w, h -1) *
                                pixelCalculations.grabEdge(w, h, w, h + 1) *
                                pixelCalculations.grabEdge(w, h,w + 1, h) *
                                pixelCalculations.grabEdge(w, h,w -1, h);
                    if (imageProcessor.getf(w, h) != 1 && sum > 1){
                        totalArea += (areaOfPixel / sum);
                    }
                    if (imageProcessor.getf(w, h) == 1){
                        totalArea += areaOfPixel;
                    }
                }
            }
            Assert.assertEquals(n5DataSetFile.testDomainArea, totalArea, n5DataSetFile.testDomainArea * 0.013);
        }
    }

    public void alphaStatsTest(ImagePlus imagePlus, N5DataSetFile n5DataSetFile, stats testType){
        double[][] controlData = {{}};
        switch (testType){
            case HISTMAX:
                controlData = n5DataSetFile.histMax;
                break;
            case HISTMIN:
                controlData = n5DataSetFile.histMin;
                break;
            case HISTAVERAGE:
                controlData = n5DataSetFile.histAverage;
        }
        for(int k = 0; k < controlData.length; k++){
            for (int i = 0; i < controlData[k].length; i++){
                imagePlus.setPosition(k + 1, 1, i + 1); //frame position seems to start at 1
                double experimentalData = 0;
                switch (testType){
                    case HISTMAX:
                        experimentalData = imagePlus.getStatistics().histMax;
                        break;
                    case HISTMIN:
                        setImageMask(imagePlus);
                        experimentalData = imagePlus.getStatistics().min;
                        break;
                    case HISTAVERAGE:
                        setImageMask(imagePlus);
                        experimentalData = imagePlus.getStatistics().mean;
                }
                Assert.assertEquals("Channel: " + k + " Time: " + i + " Stat: " + testType +
                                "\n Experiment Value: " + experimentalData + " Control Value: " + controlData[k][i]
                        ,0.0, experimentalData - controlData[k][i], 0.000001);
            }
        }
    }

    private void setImageMask(ImagePlus imagePlus){
        ImagePlus maskImagePlus = imagePlus.duplicate();
        maskImagePlus.setPosition(maskImagePlus.getNChannels(), 1, 1);
        ImageProcessor maskProcessor = maskImagePlus.getChannelProcessor();
        int height = maskImagePlus.getHeight();
        int width = maskImagePlus.getWidth();
        for (int i = 0; i < height; i++){
            for (int k = 0; k < width; k++){
                int maskValue = maskProcessor.get(k, i);
                if (maskValue == 0){
                    imagePlus.getProcessor().setf(k, i, Float.NaN);
                }
            }
        }
    }


    private void remoteN5ImgPlusTests(SimResultsLoader simResultsLoader) throws IOException {
        ImagePlus imagePlus = simResultsLoader.getImgPlusFromN5File();
        dataSetListTest(simResultsLoader.getS3N5DatasetList());
        fiveDStackTests(imagePlus);
        imagePlus = new Duplicator().run(imagePlus); //Tests taking the N5 file from streaming to in memory
        fiveDStackTests(imagePlus);
    }

    private void fiveDStackTests(ImagePlus variableImgPlus){
        ImagePlus controlImgPlus = Opener.openUsingBioFormats(this.getTestResourceFiles("mitosis.tif").getAbsolutePath());
        int[] variableDimensions = variableImgPlus.getDimensions();
        int[] controlDimensions = controlImgPlus.getDimensions();

        for(int i = 0; i<controlDimensions.length; i++){
            Assert.assertEquals(controlDimensions[i], variableDimensions[i]);
        }
        Assert.assertEquals("Same bit depth",controlImgPlus.getBitDepth(), variableImgPlus.getBitDepth());
        ImagePlus difference = ImageCalculator.run(controlImgPlus, variableImgPlus, "difference create");

        //Everything should be 0 since the images should be exactly the same, thus the difference results in 0
        Assert.assertEquals(0.0, difference.getStatistics().histMax, 0.0);
        Assert.assertEquals(0.0, difference.getStatistics().histMin, 0.0);
        Assert.assertEquals("Difference is Zero",0.0, difference.getStatistics().stdDev, 0.0);
    }

    private void dataSetListTest(ArrayList<String> dataSetList){
        Assert.assertEquals(3, dataSetList.size());
        Assert.assertTrue("Has test.c0.s0",dataSetList.contains("test/c0/s0"));
        Assert.assertTrue("Has 5DStack",dataSetList.contains("5DStack"));
        Assert.assertTrue("Has 5Channels",dataSetList.contains("5Channels"));
    }

}