package org.vcell.N5.retrieving;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.AnonymousAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.http.conn.ssl.SdkTLSSocketFactory;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.LinkedTreeMap;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.io.RoiDecoder;
import ij.plugin.ContrastEnhancer;
import ij.plugin.Duplicator;
import ij.plugin.frame.RoiManager;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.real.DoubleType;
import org.apache.commons.io.IOUtils;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.ssl.SSLContexts;
import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.N5FSReader;
import org.janelia.saalfeldlab.n5.N5KeyValueReader;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.scijava.log.Logger;
import org.vcell.N5.N5ImageHandler;
import org.vcell.N5.UI.RangeSelector;
import org.vcell.N5.library.extensions.S3KeyValueAccess;
import org.vcell.N5.library.extensions.SimCacheLoader;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;

public class SimResultsLoader {
    private File selectedLocalFile;
    private ArrayList<URL> roiReferences;
    private URI uri;
    private String dataSetChosen;
    private static final String defaultS3Region = "site2-low";
    private N5Reader n5AmazonS3Reader = null;
    private static final Logger logger = N5ImageHandler.getLogger(SimResultsLoader.class);
    public static AmazonS3ClientBuilder s3ClientBuilder;
    private ImagePlus imagePlus = null;
    public OpenTag openTag;


    public String userSetFileName = null;
    public int rowNumber;
    public String exportID;

    public SimResultsLoader(){
        openTag = OpenTag.TEST;
    }
    public SimResultsLoader(String stringURI, String userSetFileName, int rowNumber, String exportID, OpenTag openTag, ArrayList<URL> roiReference){
        this(stringURI, userSetFileName, openTag);
        this.rowNumber = rowNumber;
        this.exportID = exportID;
        this.roiReferences = roiReference;
    }

    // Mostly for tests
    public SimResultsLoader(String stringURI, String userSetFileName, OpenTag openTag){
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
        this.openTag = openTag;
    }

    /////////////////////////////////
    // Loading Simulation Results //
    ///////////////////////////////

    public void createS3ClientAndReader(){
        logger.debug("Creating S3 Client with url: " + uri);
        if (uri.getHost().equals("minikube.remote") || uri.getHost().equals("minikube.island")){
            allowInsecureConnections();
        }
        //////////////////////////////////////////////
        // assume it is one of our URLs             //
        // http://vcell:8000/bucket/object/object2  //
        //////////////////////////////////////////////

        String[] pathSubStrings = uri.getPath().split("/", 3);
        String s3ObjectKey = pathSubStrings[2];
        String bucketName = pathSubStrings[1];
        s3ClientBuilder.withPathStyleAccessEnabled(true);
        logger.debug("Creating S3 Client with Path Based Buckets");

        s3ClientBuilder.withCredentials(new AWSStaticCredentialsProvider(new AnonymousAWSCredentials()));
        s3ClientBuilder.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(uri.getScheme() + "://" + uri.getAuthority(), defaultS3Region));
        AmazonS3 s3Client = s3ClientBuilder.build();
        S3KeyValueAccess amazonS3KeyValueAccess = new S3KeyValueAccess(s3Client, bucketName, uri, false);
        n5AmazonS3Reader = new N5KeyValueReader(amazonS3KeyValueAccess, "", new GsonBuilder(), true);
    }

    private void allowInsecureConnections(){
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

    /**
        By default N5 library assumes that S3 URL's are formated in the standard amazon format. That being
        https://BUCKETNAME.s3.REGION.amazonaws.com {@link org.janelia.saalfeldlab.n5.s3.AmazonS3Utils}. Because our objects
        don't originate from amazon this is not a format we can possibly mimic, so we have to use path based buckets because
        it's the fallback style chosen by the N5 libraries if standard format is unavailable.
     */

    public ImagePlus getImagePlus(){
        if (imagePlus != null){
            return imagePlus;
        }
        if (n5AmazonS3Reader == null){
            createS3ClientAndReader();
        }
        loadImageFromN5File();
        return imagePlus;
    }

    public void loadImageFromN5File() {
        long start = System.currentTimeMillis();
        logger.debug("Reading N5 File " + userSetFileName + " Into Virtual Image");
        if (userSetFileName == null || userSetFileName.isEmpty()){
            userSetFileName = n5AmazonS3Reader.getAttribute(dataSetChosen, "name", String.class);
        }

        SimCacheLoader<DoubleType, ?> simCacheLoader = SimCacheLoader.factoryDefault(n5AmazonS3Reader, dataSetChosen);
        CachedCellImg<DoubleType, ?> cachedCellImg = simCacheLoader.createCachedCellImage();
        imagePlus = ImageJFunctions.wrap(cachedCellImg, userSetFileName);
        simCacheLoader.setImagesResponsibleFor(imagePlus);
        long end = System.currentTimeMillis();
        logger.debug("Read N5 File " + userSetFileName + " Into ImageJ taking: " + ((end - start) / 1000.0) + "s");

        setUnits(n5AmazonS3Reader, imagePlus);
        imagePlus.setProperty("channelInfo", getChannelInfo());
        imagePlus.setProperty("maskInfo", n5AmazonS3Reader.getAttribute(dataSetChosen, "maskMapping", HashMap.class));
        imagePlus.setZ(Math.floorDiv(imagePlus.getNSlices(), 2));
        imagePlus.setT(Math.floorDiv(imagePlus.getNFrames(), 2));

        // There is ROI's referenced with this image
        if (roiReferences != null){
            for (URL roiReference : roiReferences){
                try (InputStream input = roiReference.openStream()) {
                    final File tempFile = File.createTempFile("imagej-roi", "tmp");
                    try (FileOutputStream out = new FileOutputStream(tempFile)) {
                        IOUtils.copy(input, out);
                    }
                    Roi roi = RoiDecoder.open(tempFile.getAbsolutePath());
                    imagePlus.setRoi(roi);
                    tempFile.delete();
                } catch (IOException e) {
                    logger.debug("Can't open stream to roi reference: " + roiReference);
                }
            }
        }
        new ContrastEnhancer().stretchHistogram(imagePlus, 1);
    }

    public ImagePlus openInMemory(RangeSelector rangeSelector){
        loadImageFromN5File();
        long start = System.currentTimeMillis();
        logger.debug("Loading Virtual N5 File " + userSetFileName + " Into Memory");
        imagePlus = new Duplicator().run(imagePlus, rangeSelector.startC, rangeSelector.endC, rangeSelector.startZ,
                rangeSelector.endZ, rangeSelector.startT, rangeSelector.endT);
        long end = System.currentTimeMillis();
        logger.debug("Loaded Virtual N5 File " + userSetFileName + " Into Memory taking: " + ((end - start)/ 1000) + "s");
        return imagePlus;
    }


    ////////////////
    // Properties //
    ////////////////

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

    public LinkedTreeMap<String, LinkedTreeMap<String, String>> getChannelInfo(){
        return (LinkedTreeMap<String, LinkedTreeMap<String, String>>) n5AmazonS3Reader.getAttribute(dataSetChosen, "channelInfo", LinkedTreeMap.class);
    }

    void setDataSetChosen(String dataSetChosen) {
        this.dataSetChosen = dataSetChosen;
    }

    public void setTagToCanceled(){
        openTag = OpenTag.CANCELED;
    }

    /////////////////////////
    // Local File Reading //
    ////////////////////////

    void setSelectedLocalFile(File selectedLocalFile){
        this.selectedLocalFile = selectedLocalFile;
    }

    ImagePlus getImgPlusFromLocalN5File() throws IOException {
        N5Reader n5Reader = new N5FSReader(selectedLocalFile.getPath());
        return ImageJFunctions.wrap((CachedCellImg<DoubleType, ?>) N5Utils.open(n5Reader, dataSetChosen), userSetFileName);
    }

    public enum OpenTag{
        VIEW,
        DATA_REDUCTION,
        CANCELED,
        TEST
    }
}
