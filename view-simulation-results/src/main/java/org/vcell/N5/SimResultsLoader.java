package org.vcell.N5;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.AnonymousAWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.AmazonS3URI;
import com.google.gson.GsonBuilder;
import ij.ImagePlus;
import ij.plugin.Duplicator;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.real.DoubleType;
import org.janelia.saalfeldlab.n5.N5FSReader;
import org.janelia.saalfeldlab.n5.N5KeyValueReader;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.s3.AmazonS3KeyValueAccess;
import org.janelia.saalfeldlab.n5.s3.N5AmazonS3Reader;
import org.scijava.log.Logger;
import org.vcell.N5.UI.N5ExportTable;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class SimResultsLoader {
    private File selectedLocalFile;
    private AmazonS3 s3Client;
    private String bucketName;
    private String s3ObjectKey;
    private URI uri;
    private String dataSetChosen;
    private String userSetFileName;

    private static final Logger logger = N5ImageHandler.getLogger(SimResultsLoader.class);

    public SimResultsLoader(){

    }

    public SimResultsLoader(String stringURI, String userSetFileName){
        uri = URI.create(stringURI);
        this.userSetFileName = userSetFileName;
        setURI(uri);
    }

    public void createS3Client(String url, HashMap<String, String> credentials, HashMap<String, String> endpoint){
        logger.debug("Creating S3 Client");
        AmazonS3ClientBuilder s3ClientBuilder = AmazonS3ClientBuilder.standard();
        URI uri = URI.create(url);

        if(credentials != null){
            s3ClientBuilder.withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(credentials.get("AccessKey"), credentials.get("SecretKey"))));
        }

        // believe that it's a s3 URL
        try{
            AmazonS3URI s3URI = new AmazonS3URI(uri);
            this.s3ObjectKey = s3URI.getKey();
            this.bucketName = s3URI.getBucket();
            if(s3URI.isPathStyle()){
                s3ClientBuilder.withPathStyleAccessEnabled(true);
            }
            if(endpoint != null){
                s3ClientBuilder.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpoint.get("Endpoint"), endpoint.get("Region")));
                this.s3Client = s3ClientBuilder.build();
                return;
            }
            // if nothing is given, default user and return so that code after if statement does not execute
            if(endpoint == null && credentials == null){
                this.s3Client = AmazonS3ClientBuilder.standard().withRegion(s3URI.getRegion()).withCredentials(new AWSStaticCredentialsProvider(new AnonymousAWSCredentials())).build();
                return;
            }
            //creds not null, but region is
            this.s3Client = s3ClientBuilder.withRegion(s3URI.getRegion()).build();
            logger.debug("Created S3 Client With Modern URL");
        }
        // otherwise assume it is one of our URLs
        // http://vcell:8000/bucket/object/object2
        catch (IllegalArgumentException e){
            String[] pathSubStrings = uri.getPath().split("/", 3);
            this.s3ObjectKey = pathSubStrings[2];
            this.bucketName = pathSubStrings[1];
            s3ClientBuilder.withPathStyleAccessEnabled(true);
            s3ClientBuilder.withCredentials(new AWSStaticCredentialsProvider(new AnonymousAWSCredentials()));
            s3ClientBuilder.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(uri.getScheme() + "://" + uri.getAuthority(), "site2-low"));
            this.s3Client = s3ClientBuilder.build();
            logger.debug("Created S3 Client With Legacy URL");
        }
    }
    //When creating client's try to make one for an Amazon link, otherwise use our custom url scheme

    public void createS3Client(){
        createS3Client(uri.toString(), null, null);
    }
    public void createS3Client(HashMap<String, String> credentials, HashMap<String, String> endpoint){createS3Client(uri.toString(), credentials, endpoint);}
    public ArrayList<String> getS3N5DatasetList() throws IOException {

        // used as a flag to tell that remote access is occurring, and that there is no local files
        try(N5AmazonS3Reader n5AmazonS3Reader = new N5AmazonS3Reader(this.s3Client, this.bucketName)) {
            return new ArrayList<>(Arrays.asList(n5AmazonS3Reader.deepListDatasets(this.s3ObjectKey)));
        }
    }

    public ArrayList<String> getN5DatasetList() throws IOException {
        // auto closes reader
        logger.debug("Getting List of N5 Datasets");
        try (N5FSReader n5Reader = new N5FSReader(selectedLocalFile.getPath())) {
            String[] metaList = n5Reader.deepList("/");
            ArrayList<String> fList = new ArrayList<>();
            for (String s : metaList) {
                if (n5Reader.datasetExists(s)) {
                    fList.add(s);
                };
            }
            logger.debug("Got List of N5 Datasets");
            return fList;
        }
    }

    public void setSelectedLocalFile(File selectedLocalFile){
        this.selectedLocalFile = selectedLocalFile;
    }

    public ImagePlus getImgPlusFromLocalN5File() throws IOException {
        N5Reader n5Reader = new N5FSReader(selectedLocalFile.getPath());
        return ImageJFunctions.wrap((CachedCellImg<DoubleType, ?>) N5Utils.open(n5Reader, dataSetChosen), userSetFileName);
    }

    public ImagePlus getImgPlusFromN5File() throws IOException {
        AmazonS3KeyValueAccess amazonS3KeyValueAccess = new AmazonS3KeyValueAccess(s3Client, bucketName, false);
        N5KeyValueReader n5AmazonS3Reader = new N5KeyValueReader(amazonS3KeyValueAccess, s3ObjectKey, new GsonBuilder(), false);

//        N5AmazonS3Reader n5AmazonS3Reader = new N5AmazonS3Reader(s3Client, bucketName, "/" + s3ObjectKey);
        long start = System.currentTimeMillis();
        logger.debug("Reading N5 File " + userSetFileName + " Into Virtual Image");
        ImagePlus imagePlus = ImageJFunctions.wrap((CachedCellImg<DoubleType, ?>) N5Utils.open(n5AmazonS3Reader, dataSetChosen), userSetFileName);
        long end = System.currentTimeMillis();
        logger.debug("Read N5 File " + userSetFileName + " Into ImageJ taking: " + ((end - start) / 1000) + "s");
        return imagePlus;
    }

    public void setURI(URI uri){
        this.uri = uri;
        if(!(uri.getQuery() == null)){
            dataSetChosen = uri.getQuery().split("=")[1]; // query should be "dataSetName=name", thus splitting it by = and getting the second entry gives the name
            try {
                dataSetChosen = URLDecoder.decode(dataSetChosen, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void setDataSetChosen(String dataSetChosen) {
        this.dataSetChosen = dataSetChosen;
    }

    public static void openN5FileDataset(ArrayList<SimResultsLoader> filesToOpen, boolean openInMemory){
        N5ExportTable.enableCriticalButtons(false);
        N5ExportTable.exportTableDialog.setCursor(new Cursor(Cursor.WAIT_CURSOR));
        Thread openN5FileDataset = new Thread(() -> {
            try{
                for(SimResultsLoader simResultsLoader: filesToOpen){
                    simResultsLoader.createS3Client();
                    ImagePlus imagePlus = simResultsLoader.getImgPlusFromN5File();
                    if(openInMemory){
                        long start = System.currentTimeMillis();
                        logger.debug("Loading Virtual N5 File " + simResultsLoader.userSetFileName + " Into Memory");
                        imagePlus = new Duplicator().run(imagePlus);
                        long end = System.currentTimeMillis();
                        logger.debug("Loaded Virtual N5 File " + simResultsLoader.userSetFileName + " Into Memory taking: " + ((end - start)/ 1000) + "s");
                    }
                    imagePlus.show();
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            } finally {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        N5ExportTable.exportTableDialog.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                        N5ExportTable.enableCriticalButtons(true);
                    }
                });
            }
        });
        openN5FileDataset.start();
    }
}
