package org.vcell.N5;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.AnonymousAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.http.conn.ssl.SdkTLSSocketFactory;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.google.gson.GsonBuilder;
import ij.ImagePlus;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.real.DoubleType;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.ssl.SSLContexts;
import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.N5FSReader;
import org.janelia.saalfeldlab.n5.N5KeyValueReader;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.s3.N5AmazonS3Reader;
import org.scijava.log.Logger;
import org.vcell.N5.UI.N5ExportTable;
import org.vcell.N5.UI.ImageIntoMemory;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
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
    public String userSetFileName = null;
    private static final String defaultS3Region = "site2-low";
    private N5Reader n5AmazonS3Reader;

    private static final Logger logger = N5ImageHandler.getLogger(SimResultsLoader.class);
    public static AmazonS3ClientBuilder s3ClientBuilder;

    public SimResultsLoader(){

    }
    public SimResultsLoader(String stringURI, String userSetFileName){
        uri = URI.create(stringURI);
        this.userSetFileName = userSetFileName;
        if(!(uri.getQuery() == null)){
            dataSetChosen = uri.getQuery().split("=")[1]; // query should be "dataSetName=name", thus splitting it by = and getting the second entry gives the name
            try {
                dataSetChosen = URLDecoder.decode(dataSetChosen, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    void createS3ClientAndReader(){
        logger.debug("Creating S3 Client with url: " + uri);
        if (uri.getHost().equals("minikube.remote") || uri.getHost().equals("minikube.island")){
            SSLContext sslContext = null;
            try {
                sslContext = SSLContexts.custom().loadTrustMaterial(new TrustSelfSignedStrategy()).build();
            } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException ex) {
                throw new RuntimeException("Custom ssl context not functional.",ex);
            }
            HostnameVerifier hostnameVerifier = new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };
            ConnectionSocketFactory connectionSocketFactory = new SdkTLSSocketFactory(sslContext, hostnameVerifier);
            ClientConfiguration clientConfiguration = new ClientConfiguration();
            clientConfiguration.getApacheHttpClientConfig().setSslSocketFactory(connectionSocketFactory);
            s3ClientBuilder.setClientConfiguration(clientConfiguration);
        }

        // assume it is one of our URLs
        // http://vcell:8000/bucket/object/object2
        String[] pathSubStrings = uri.getPath().split("/", 3);
        this.s3ObjectKey = pathSubStrings[2];
        this.bucketName = pathSubStrings[1];
        s3ClientBuilder.withPathStyleAccessEnabled(true);
        logger.debug("Creating S3 Client with Path Based Buckets");

        s3ClientBuilder.withCredentials(new AWSStaticCredentialsProvider(new AnonymousAWSCredentials()));
        s3ClientBuilder.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(uri.getScheme() + "://" + uri.getAuthority(), defaultS3Region));
        this.s3Client = s3ClientBuilder.build();
        S3KeyValueAccess amazonS3KeyValueAccess = new S3KeyValueAccess(s3Client, bucketName, false);
        n5AmazonS3Reader = new N5KeyValueReader(amazonS3KeyValueAccess, s3ObjectKey, new GsonBuilder(), false);
    }

    /**
        By default N5 library assumes that S3 URL's are formated in the standard amazon format. That being
        https://BUCKETNAME.s3.REGION.amazonaws.com {@link org.janelia.saalfeldlab.n5.s3.AmazonS3Utils}. Because our objects
        don't originate from amazon this is not a format we can possibly mimic, so we have to use path based buckets because
        it's the fallback style chosen by the N5 libraries if standard format is unavailable.
     */
     public ImagePlus getImgPlusFromN5File() throws IOException {

//        N5AmazonS3Reader n5AmazonS3Reader = new N5AmazonS3Reader(s3Client, bucketName, "/" + s3ObjectKey);
        long start = System.currentTimeMillis();
        logger.debug("Reading N5 File " + userSetFileName + " Into Virtual Image");
        if (userSetFileName == null || userSetFileName.isEmpty()){
            userSetFileName = n5AmazonS3Reader.getAttribute(dataSetChosen, "name", String.class);
        }


        SimCacheLoader<DoubleType, ?> simCacheLoader = SimCacheLoader.factoryDefault(n5AmazonS3Reader, dataSetChosen);
        CachedCellImg<DoubleType, ?> cachedCellImg = simCacheLoader.createCachedCellImage();
        ImagePlus imagePlus = ImageJFunctions.wrap(cachedCellImg, userSetFileName);
        simCacheLoader.setImagesResponsibleFor(imagePlus);
        long end = System.currentTimeMillis();
        logger.debug("Read N5 File " + userSetFileName + " Into ImageJ taking: " + ((end - start) / 1000.0) + "s");

        setUnits(n5AmazonS3Reader, imagePlus);
        imagePlus.setProperty("channelInfo", n5AmazonS3Reader.getAttribute(dataSetChosen, "channelInfo", HashMap.class));
        imagePlus.setProperty("maskInfo", n5AmazonS3Reader.getAttribute(dataSetChosen, "maskMapping", HashMap.class));
        return imagePlus;
    }

    private void setUnits(N5Reader n5Reader, ImagePlus imagePlus){
        try{
            double pixelWidth = n5Reader.getAttribute(dataSetChosen, "pixelWidth", double.class);
            double pixelHeight = n5Reader.getAttribute(dataSetChosen, "pixelHeight", double.class);
            double pixelDepth = n5Reader.getAttribute(dataSetChosen, "pixelDepth", double.class);
            double frameInterval = n5Reader.getAttribute(dataSetChosen, "frameInterval", double.class);
            String unit = n5Reader.getAttribute(dataSetChosen, "unit", String.class);
            imagePlus.getCalibration().setUnit(unit);
            imagePlus.getCalibration().pixelHeight = pixelHeight;
            imagePlus.getCalibration().pixelWidth = pixelWidth;
            imagePlus.getCalibration().pixelDepth = pixelDepth;
            imagePlus.getCalibration().frameInterval = frameInterval;
        }
        catch (N5Exception n5Exception){
            logger.error("Can't read units.");
        }
    }

    public ArrayList getN5Dimensions(){
        return n5AmazonS3Reader.getAttribute(dataSetChosen, "dimensions", ArrayList.class);
    }


    public static void openN5FileDataset(ArrayList<SimResultsLoader> filesToOpen, boolean openInMemory){
        N5ExportTable.enableCriticalButtons(false);
        N5ExportTable.exportTableDialog.setCursor(new Cursor(Cursor.WAIT_CURSOR));
        Thread openN5FileDataset = new Thread(() -> {
            try{
                for(SimResultsLoader simResultsLoader: filesToOpen){
                    simResultsLoader.createS3ClientAndReader();
                    ImageIntoMemory imageIntoMemory;
                    if (openInMemory){
                        ArrayList<Double> dimensions = simResultsLoader.getN5Dimensions();
                        imageIntoMemory = new ImageIntoMemory(dimensions.get(2), dimensions.get(3), dimensions.get(4), simResultsLoader);
                        imageIntoMemory.displayRangeMenu();
                    } else{
                        ImagePlus imagePlus = simResultsLoader.getImgPlusFromN5File();
                        imagePlus.show();
                    }

                }
            } catch (Exception ex) {
                N5ExportTable.exportTableDialog.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                throw new RuntimeException(ex);
            } finally {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        if (!openInMemory) {
                            N5ExportTable.exportTableDialog.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                        }
                        N5ExportTable.enableCriticalButtons(true);
                    }
                });
            }
        });
        openN5FileDataset.setName("Open N5 File");
        openN5FileDataset.start();
    }

    public static void openLocalN5FS(ArrayList<SimResultsLoader> filesToOpen){
        N5ExportTable.enableCriticalButtons(true);
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setAcceptAllFileFilterUsed(false);
        int result = fileChooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION){
            File file = fileChooser.getSelectedFile();
            N5ExportTable.exportTableDialog.setCursor(new Cursor(Cursor.WAIT_CURSOR));
            Thread openN5FileDataset = new Thread(() -> {
                try{
                    for(SimResultsLoader simResultsLoader: filesToOpen){
                        simResultsLoader.setSelectedLocalFile(file);
                        ImagePlus imagePlus = simResultsLoader.getImgPlusFromLocalN5File();
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

    void setDataSetChosen(String dataSetChosen) {
        this.dataSetChosen = dataSetChosen;
    }

    void setSelectedLocalFile(File selectedLocalFile){
        this.selectedLocalFile = selectedLocalFile;
    }
    ArrayList<String> getS3N5DatasetList() throws IOException {

        // used as a flag to tell that remote access is occurring, and that there is no local files
        try(N5AmazonS3Reader n5AmazonS3Reader = new N5AmazonS3Reader(this.s3Client, this.bucketName)) {
            return new ArrayList<>(Arrays.asList(n5AmazonS3Reader.deepListDatasets(this.s3ObjectKey)));
        }
    }
    ImagePlus getImgPlusFromLocalN5File() throws IOException {
        N5Reader n5Reader = new N5FSReader(selectedLocalFile.getPath());
        return ImageJFunctions.wrap((CachedCellImg<DoubleType, ?>) N5Utils.open(n5Reader, dataSetChosen), userSetFileName);
    }

    ArrayList<String> getN5DatasetList() throws IOException {
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
}
