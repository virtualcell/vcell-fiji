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

    public void testS3ClientCreation(){
        N5ImageHandler n5ImageHandler = new N5ImageHandler();
    }

    // Should be same results for local images
    public void testS3N5DatasetList(){

    }

    // Should be the exact same results for local images
    public void testS3GettingImgPlus(){

    }

    // Create client without creds, with cred no endpoint, endpoint no creds, endpoint and creds
    public void testS3Client(){

    }




    public void manualTesting(){
        N5ImageHandler n5ImageHandler = new N5ImageHandler();
        n5ImageHandler.run();
    }


}