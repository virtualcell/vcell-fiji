package org.vcell.N5;

import com.google.gson.internal.LinkedTreeMap;
import ij.ImagePlus;
import ij.io.Opener;
import ij.plugin.Duplicator;
import ij.plugin.ImageCalculator;
import ij.process.ImageProcessor;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;


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
            ImagePlus inMemory = new Duplicator().run(imagePlus);
            for (Object property : imagePlus.getProperties().keySet()){
                inMemory.setProperty((String) property, imagePlus.getProperty((String) property));
            }
            alphaStatsTest(inMemory, n5DataSetFile, stats.HISTMAX);
            alphaStatsTest(inMemory, n5DataSetFile, stats.HISTMIN);
            alphaStatsTest(inMemory, n5DataSetFile, stats.HISTAVERAGE);
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

            boolean threeD = imagePlus.getNSlices() > 1;
            if (threeD){
                areaOfPixel = imagePlus.getCalibration().getX(1) * imagePlus.getCalibration().getY(1) * imagePlus.getCalibration().getZ(1);
                totalArea = areaOfPixel * imagePlus.getWidth() * imagePlus.getHeight() * imagePlus.getNSlices();
            }
            Assert.assertEquals(n5DataSetFile.totalArea, totalArea, n5DataSetFile.totalArea * 0.0001);

            imagePlus.setPosition(imagePlus.getNChannels(), 1, 1);
            ImageProcessor imageProcessor = imagePlus.getProcessor();

            for(int domain: n5DataSetFile.mask.keySet()){
                totalArea = 0;

                if(!threeD) {
                    for (int h = 0; h < imagePlus.getHeight(); h++) {
                        for (int w = 0; w < imagePlus.getWidth(); w++) {
                            if (imageProcessor.getf(w, h) == domain) {
                                totalArea += areaOfPixel;
                            }
                        }
                    }
                }
                else{
                    for (int k = 0; k < imagePlus.getNSlices(); k++){
                        imagePlus.setPosition(imagePlus.getNChannels(), k + 1, 1);
                        imageProcessor = imagePlus.getProcessor();
                        for (int h = 0; h < imagePlus.getHeight(); h++) {
                            for (int w = 0; w < imagePlus.getWidth(); w++) {
                                if (imageProcessor.getf(w, h) == domain) {
                                    totalArea += areaOfPixel;
                                }
                            }
                        }
                    }
                }
                Assert.assertEquals("Domain: " + domain + ", Total Area: " + totalArea,
                        n5DataSetFile.testDomainArea[domain], totalArea, 0.000001);
            }
        }
    }

    private double alphaStatsThreeD(ImagePlus imagePlus, stats testType, int channel, int frame){
        double experimentValue = Double.NaN;
        for (int z = 1; z <= imagePlus.getNSlices(); z++){
            imagePlus.setPosition(channel, z, frame);
            double currentValue;
            switch (testType){
                case HISTMAX:
                    setImageMask(imagePlus);
                    currentValue = imagePlus.getStatistics().max;
                    if (currentValue ==  -Double.MAX_VALUE){
                        currentValue = 0;
                    }
                    if (Double.isNaN(experimentValue) || experimentValue < currentValue){
                        experimentValue = currentValue;
                    }
                    break;
                case HISTMIN:
                    setImageMask(imagePlus);
                    currentValue = imagePlus.getStatistics().min;
                    if (Double.isNaN(experimentValue) || experimentValue > currentValue){
                        experimentValue = currentValue;
                    }
                    break;
                case HISTAVERAGE:
                    experimentValue = average3D(imagePlus);
            }
            if (testType.equals(stats.HISTAVERAGE)){
                break;
            }
        }
        return experimentValue;
    }

    private double average3D(ImagePlus imagePlus){
        double total = 0;
        double voxelVolume = imagePlus.getCalibration().pixelHeight * imagePlus.getCalibration().pixelWidth * imagePlus.getCalibration().pixelDepth;
        double totalVolume = 0;
        for (int z = 1; z < imagePlus.getNSlices(); z++){
            imagePlus.setPosition(imagePlus.getChannel(), z, imagePlus.getFrame());
            setImageMask(imagePlus);
            ImageProcessor imageProcessor = imagePlus.getProcessor();
            for (int x = 0; x < imagePlus.getWidth(); x++){
                for (int y = 0; y < imagePlus.getHeight(); y++){
                    double pixelValue = imageProcessor.getValue(x, y);
                    if (!Double.isNaN(pixelValue)){
                        total += (pixelValue * voxelVolume);
                        totalVolume += voxelVolume;
                    }
                }
            }

        }
        return (total / totalVolume );
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
        boolean threeD = imagePlus.getNSlices() > 1;
        for(int channel = 1; channel <= controlData.length; channel++){
            for (int frame = 1; frame <= controlData[channel - 1].length; frame++){
                imagePlus.setPosition(channel, 1, frame); //frame position seems to start at 1
                double experimentalData = 0;
                switch (testType){
                    case HISTMAX:
                        experimentalData = threeD ? alphaStatsThreeD(imagePlus, testType, channel, frame) :
                                imagePlus.getStatistics().max;
                        break;
                    case HISTMIN:
                        setImageMask(imagePlus);
                        experimentalData = threeD ? alphaStatsThreeD(imagePlus, testType, channel, frame) :
                                imagePlus.getStatistics().min;
                        break;
                    case HISTAVERAGE:
                        setImageMask(imagePlus);
                        experimentalData = threeD ? alphaStatsThreeD(imagePlus, testType, channel, frame) :
                                imagePlus.getStatistics().mean;
                }
                Assert.assertEquals("Channel: " + channel + " Time: " + frame + " Stat: " + testType +
                                "\n Experiment Value: " + experimentalData + " Control Value: " + controlData[channel - 1][frame - 1]
                        ,0.0, experimentalData - controlData[channel - 1][frame - 1], 0.0001);
            }
        }
    }

    private ImagePlus setImageMask(ImagePlus imagePlus){
        ImagePlus maskImagePlus = imagePlus.duplicate();
        LinkedTreeMap<String, LinkedTreeMap<String, Object>> channelInfo = (LinkedTreeMap<String, LinkedTreeMap<String, Object>>) imagePlus.getProperty("channelInfo");
        String domainName = (String) channelInfo.get(String.valueOf(imagePlus.getChannel() - 1)).get("Domain");
        LinkedTreeMap<String, String> maskInfo = (LinkedTreeMap<String, String>) imagePlus.getProperty("maskInfo");
        int domainMaskValue = 0;
        for (Map.Entry<String, String> entry : maskInfo.entrySet()) {
            if (entry.getValue().equals(domainName)) {
                domainMaskValue = Integer.parseInt(entry.getKey());
                break;
            }
        }

        maskImagePlus.setPosition(maskImagePlus.getNChannels(), imagePlus.getSlice(), imagePlus.getFrame());
        ImageProcessor maskProcessor = maskImagePlus.getProcessor();
        int height = maskImagePlus.getHeight();
        int width = maskImagePlus.getWidth();
        for (int h = 0; h < height; h++){
            for (int w = 0; w < width; w++){
                float maskValue = maskProcessor.getPixelValue(w, h);
                if (maskValue != domainMaskValue){
                    imagePlus.getProcessor().setf(w, h, Float.NaN);
                }
            }
        }
        return imagePlus;
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