package org.vcell.vcellfiji;

import com.amazonaws.regions.Regions;
import ij.ImagePlus;
import ij.io.Opener;
import ij.plugin.ImageCalculator;
import junit.framework.TestCase;
import org.gaul.s3proxy.S3Proxy;
import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.BlobStoreContext;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;


/*
    One test file is only metadata with no actual visual information. Has a bunch of datasets, and these datasets have complex metadata.
    Another one has only one dataset and some visual information which is used to determine whether the data is accurate or not

    S3Proxy Rule does not work properly so I'm just using the native S3 proxy and manually controlling the initialization of the s3 mock server
 */

public class N5ImageHandlerTest extends TestCase {

    private final String s3AccessKey = "access";
    private final String s3SecretKey = "secret";
    private final String n5FileName = "nfive/test_image.n5";

    private final String s3CredsEndpoint = "http://127.0.0.1:9999";
    private final String s3NoCredsEndpoint = "http://127.0.0.1:9090";

    private final String testBucketName = "nfive";

    private final S3Proxy s3ProxyCreds;

    public S3Proxy s3ProxyNoCreds;

    public N5ImageHandlerTest(){
        Properties properties = new Properties();
        properties.setProperty("jclouds.filesystem.basedir", this.getTestResourceFiles("").getPath());

        BlobStoreContext context = ContextBuilder
                .newBuilder("filesystem")
                .credentials(this.s3AccessKey, this.s3SecretKey)
                .overrides(properties)
                .build(BlobStoreContext.class);

        BlobStoreContext contextNoCreds = ContextBuilder
                .newBuilder("filesystem")
                .overrides(properties)
                .build(BlobStoreContext.class);

        s3ProxyCreds = S3Proxy.builder()
                .blobStore(context.getBlobStore())
                .endpoint(URI.create(s3CredsEndpoint))
                .build();

        s3ProxyNoCreds = S3Proxy.builder()
                .blobStore(contextNoCreds.getBlobStore())
                .endpoint(URI.create(s3NoCredsEndpoint))
                .build();

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

    public void testN5DatasetList(){
        N5ImageHandler n5ImageHandler = new N5ImageHandler();
        n5ImageHandler.setSelectedLocalFile(this.getTestResourceFiles(n5FileName));
        this.dataSetListTest(n5ImageHandler.getN5DatasetList());
    }

    public void testGettingImgPlus() throws IOException {
        N5ImageHandler n5ImageHandler = new N5ImageHandler();
        n5ImageHandler.setSelectedLocalFile(this.getTestResourceFiles(n5FileName));
        ImagePlus imagePlus = n5ImageHandler.getImgPlusFromN5File("5DStack", n5ImageHandler.getN5FSReader());

        this.fiveDStackTests(imagePlus);
    }

    // Create client without creds, with cred no endpoint, endpoint no creds, endpoint and creds, then test whether they can handle images as expected
    public void testS3Client() throws Exception {
        s3ProxyCreds.start();
        s3ProxyNoCreds.start();

        HashMap<String, String> endpointCreds = new HashMap<>();
        HashMap<String, String> endpointNoCreds = new HashMap<>();
        HashMap<String, String> credentials = new HashMap<>();

        endpointCreds.put("Endpoint", this.s3CredsEndpoint);
        endpointCreds.put("Region", Regions.US_EAST_1.getName());

        endpointNoCreds.put("Endpoint", this.s3NoCredsEndpoint);
        endpointNoCreds.put("Region", Regions.US_EAST_1.getName());

        credentials.put("AccessKey", this.s3AccessKey);
        credentials.put("SecretKey", this.s3SecretKey);

        final String keyPath = "s3://" + this.testBucketName + "/test_image.n5";

        N5ImageHandler n5ImageHandler = new N5ImageHandler();
//        n5ImageHandler.createS3Client(s3NoCredsEndpoint + keyPath, null, null);
//        this.remoteN5ImgPlusTests(n5ImageHandler);
//
        n5ImageHandler.createS3Client(keyPath, null, endpointNoCreds);
        this.remoteN5ImgPlusTests(n5ImageHandler);

//        n5ImageHandler.createS3Client(this.s3CredsEndpoint + keyPath, credentials, null);
//        this.remoteN5ImgPlusTests(n5ImageHandler);

        n5ImageHandler.createS3Client(keyPath, credentials, endpointCreds);
        this.remoteN5ImgPlusTests(n5ImageHandler);

        s3ProxyCreds.stop();
        s3ProxyNoCreds.stop();
    }

    private void createTestBucketAndObjects(){

    }



    private void remoteN5ImgPlusTests(N5ImageHandler n5ImageHandler){
        try {
            String dataSet = "5DStack";
            ImagePlus imagePlus = n5ImageHandler.getImgPlusFromN5File(dataSet, n5ImageHandler.getN5AmazonS3Reader());
            dataSetListTest(n5ImageHandler.getS3N5DatasetList());
            fiveDStackTests(imagePlus);
        }
        catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    private void fiveDStackTests(ImagePlus variableImgPlus){
        ImagePlus controlImgPlus = Opener.openUsingBioFormats(this.getTestResourceFiles("mitosis.tif").getAbsolutePath());
        int[] variableDimensions = variableImgPlus.getDimensions();
        int[] controlDimensions = controlImgPlus.getDimensions();

        for(int i = 0; i<controlDimensions.length; i++){
            assertEquals(controlDimensions[i], variableDimensions[i]);
        }
        assertEquals("Same bit depth",controlImgPlus.getBitDepth(), variableImgPlus.getBitDepth());
        ImagePlus difference = ImageCalculator.run(controlImgPlus, variableImgPlus, "difference create");

        //Everything should be 0 since the images should be exactly the same, thus the difference results in 0
        assertEquals(0.0, difference.getStatistics().histMax);
        assertEquals(0.0, difference.getStatistics().histMin);
        assertEquals("Difference is Zero",0.0, difference.getStatistics().stdDev);
//        imagePlus.getStatistics().
    }

    private void dataSetListTest(ArrayList<String> dataSetList){
        assertEquals(3, dataSetList.size());
        assertTrue("Has test.c0.s0",dataSetList.contains("test/c0/s0"));
        assertTrue("Has 5DStack",dataSetList.contains("5DStack"));
        assertTrue("Has 5Channels",dataSetList.contains("5Channels"));
    }

}