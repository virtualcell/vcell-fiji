package org.vcell.N5;

import com.amazonaws.regions.Regions;
import ij.ImagePlus;
import ij.io.Opener;
import ij.plugin.Duplicator;
import ij.plugin.ImageCalculator;
import org.junit.*;
import org.vcell.N5.N5ImageHandler;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;


/*
    One test file is only metadata with no actual visual information. Has a bunch of datasets, and these datasets have complex metadata.
    Another one has only one dataset and some visual information which is used to determine whether the data is accurate or not

    S3Proxy Rule does not work properly so I'm just using the native S3 proxy and manually controlling the initialization of the s3 mock server
 */

public class N5ImageHandlerTest {
    private final String n5FileName = "nfive/test_image.n5";
    private final String datasetName = "5DStack";

    private File getTestResourceFiles(String filePath){
        try {
            URL url = ClassLoader.getSystemClassLoader().getResource(filePath);
            return new File(url.toURI().getPath());
        }
        catch (URISyntaxException e){
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testN5DatasetList(){
        SimResultsLoader simResultsLoader = new SimResultsLoader();
        simResultsLoader.setSelectedLocalFile(this.getTestResourceFiles(n5FileName));
        this.dataSetListTest(simResultsLoader.getN5DatasetList());
    }

    @Test
    public void testGettingImgPlus() throws IOException {
        SimResultsLoader simResultsLoader = new SimResultsLoader();
        simResultsLoader.setDataSetChosen(datasetName);
        simResultsLoader.setSelectedLocalFile(this.getTestResourceFiles(n5FileName));
        ImagePlus imagePlus = simResultsLoader.getImgPlusFromN5File();

        fiveDStackTests(imagePlus);
        imagePlus = new Duplicator().run(imagePlus);
        fiveDStackTests(imagePlus);
    }

    @Test
    // Create client without creds, with cred no endpoint, endpoint no creds, endpoint and creds, then test whether they can handle images as expected
    public void testS3Client() throws IOException {
        HashMap<String, String> s3Endpoint = new HashMap<>();
        HashMap<String, String> credentials = new HashMap<>();

        s3Endpoint.put("Endpoint", "http://127.0.0.1:4000");
        s3Endpoint.put("Region", Regions.US_EAST_1.getName());

        credentials.put("AccessKey", "jj");
        credentials.put("SecretKey", "jj");

        final String s3ProxyURI = "http://localhost:4000/" + this.n5FileName + "?datasetName=" + datasetName;

        SimResultsLoader simResultsLoader = new SimResultsLoader(s3ProxyURI);

        // Environment variables are set in github actions VM

        simResultsLoader.createS3Client(null, null);
        this.remoteN5ImgPlusTests(simResultsLoader);

        simResultsLoader.createS3Client(null, s3Endpoint);
        this.remoteN5ImgPlusTests(simResultsLoader);

        simResultsLoader.createS3Client(credentials, null);
        this.remoteN5ImgPlusTests(simResultsLoader);

        simResultsLoader.createS3Client(credentials, s3Endpoint);
        this.remoteN5ImgPlusTests(simResultsLoader);
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