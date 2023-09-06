package org.vcell.vcellfiji;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.ibm.icu.impl.ClassLoaderUtil;
import ij.ImagePlus;
import junit.framework.TestCase;
import org.gaul.s3proxy.junit.S3ProxyRule;
import org.janelia.saalfeldlab.n5.N5FSReader;
import org.junit.Rule;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;


/*
    One test file is only metadata with no actual visual information. Has a bunch of datasets, and these datasets have complex metadata.
    Another one has only one dataset and some visual information which is used to determine whether the data is accurate or not
 */

public class N5ImageHandlerTest extends TestCase {

    private File getTestResourceFiles(String filePath){
        try {
            URL url = ClassLoaderUtil.getClassLoader().getResource(filePath);
            return new File(url.toURI().getPath());
        }
        catch (URISyntaxException e){
            throw new RuntimeException(e);
        }
    }

    public void testN5DatasetList(){
        N5ImageHandler n5ImageHandler = new N5ImageHandler();
        n5ImageHandler.setSelectedLocalFile(this.getTestResourceFiles("N5/test_image.n5"));
        ArrayList<String> datasetList = n5ImageHandler.getN5DatasetList();

        assertEquals("test/c0/s0", datasetList.get(0));
        assertEquals(1, datasetList.size());
    }

    public void testGettingImgPlus() throws IOException {
        N5ImageHandler n5ImageHandler = new N5ImageHandler();
        n5ImageHandler.setSelectedLocalFile(this.getTestResourceFiles("N5/test_image.n5"));
        N5FSReader n5FSReader = new N5FSReader(this.getTestResourceFiles("N5/test_image.n5").getPath());
        ImagePlus imagePlus = n5ImageHandler.getImgPlusFromN5File("test/c0/s0", n5FSReader);

        // Using information that should stay constant for the image to determine if the image is being properly read
        assertEquals(1,imagePlus.getNChannels());
        int[] dimensions = {512, 512};{
        for (int i = 0; i < dimensions.length; i++)
            assertEquals(dimensions[i], imagePlus.getDimensions()[i]);
        }
    }

    // Should be same results for local images
    public void S3N5DatasetList(){
        N5ImageHandler n5ImageHandler = new N5ImageHandler();
        n5ImageHandler.setSelectedLocalFile(this.getTestResourceFiles(""));
        ArrayList<String> localDataSetList = n5ImageHandler.getN5DatasetList();

        n5ImageHandler.createS3Client("", null, null);
        ArrayList<String> remoteDataSetList = n5ImageHandler.getS3N5DatasetList();
        for (int i = 0; i<localDataSetList.size(); i++){
            assertEquals(localDataSetList.get(i), remoteDataSetList.get(i));
        }
    }

    // Create client without creds, with cred no endpoint, endpoint no creds, endpoint and creds, then test whether they can handle images as expected
    public void S3Client(){
        HashMap<String, String> endpoint = new HashMap<>();
        HashMap<String, String> credentials = new HashMap<>();
        endpoint.put("Endpoint", "");
        endpoint.put("Region", "");
        endpoint.put("BucketName", "");

        credentials.put("AccessKey", "");
        credentials.put("SecretKey", "");

        N5ImageHandler n5ImageHandler = new N5ImageHandler();
        n5ImageHandler.createS3Client("", null, null);
        this.remoteN5ImgPlusTests(n5ImageHandler);

        n5ImageHandler.createS3Client("", null, endpoint);
        this.remoteN5ImgPlusTests(n5ImageHandler);

        n5ImageHandler.createS3Client("", credentials, null);
        this.remoteN5ImgPlusTests(n5ImageHandler);

        n5ImageHandler.createS3Client("", credentials, endpoint);
        this.remoteN5ImgPlusTests(n5ImageHandler);

    }

    public void remoteN5ImgPlusTests(N5ImageHandler n5ImageHandler){
        try {
            String dataSet = "";
            ImagePlus imagePlus = n5ImageHandler.getImgPlusFromN5File(dataSet, n5ImageHandler.getN5AmazonS3Reader());

        }
        catch (Exception e){
            return;
        }
    }


}