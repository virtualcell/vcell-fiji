package org.vcell.N5;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.AnonymousAWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.AmazonS3URI;
import ij.ImagePlus;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.integer.*;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import org.janelia.saalfeldlab.n5.N5FSReader;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.s3.N5AmazonS3Reader;

import java.io.File;
import java.io.IOException;
import java.net.URI;
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

    public SimResultsLoader(){

    }

    public SimResultsLoader(String stringURI){
        uri = URI.create(stringURI);
        setURI(uri);
    }

    public void createS3Client(String url, HashMap<String, String> credentials, HashMap<String, String> endpoint){
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
            return;
        }
        // otherwise assume it is one of our URLs
        // http://vcell:8000/bucket/object/object2
        catch (IllegalArgumentException e){
            String[] pathSubStrings = uri.getPath().split("/", 3);
            this.s3ObjectKey = pathSubStrings[2];
            this.bucketName = pathSubStrings[1];
            s3ClientBuilder.withPathStyleAccessEnabled(true);
            s3ClientBuilder.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(uri.getScheme() + "://" + uri.getAuthority(), "site2-low"));
            this.s3Client = s3ClientBuilder.build();
            return;
        }
    }
    //When creating client's try to make one for an Amazon link, otherwise use our custom url scheme

    public void createS3Client(){
        createS3Client(uri.toString(), null, null);
    }
    public void createS3Client(HashMap<String, String> credentials, HashMap<String, String> endpoint){createS3Client(uri.toString(), credentials, endpoint);}
    public ArrayList<String> getS3N5DatasetList(){

        // used as a flag to tell that remote access is occurring, and that there is no local files
        selectedLocalFile = null;

        try(N5AmazonS3Reader n5AmazonS3Reader = new N5AmazonS3Reader(this.s3Client, this.bucketName)) {
            return new ArrayList<>(Arrays.asList(n5AmazonS3Reader.deepListDatasets(this.s3ObjectKey)));
        }
    }
    public N5AmazonS3Reader getN5AmazonS3Reader(){
        return new N5AmazonS3Reader(this.s3Client, this.bucketName, this.s3ObjectKey);
    }

    public N5FSReader getN5FSReader(){
        return new N5FSReader(selectedLocalFile.getPath());

    }

    public ArrayList<String> getN5DatasetList(){
        // auto closes reader
        try (N5FSReader n5Reader = new N5FSReader(selectedLocalFile.getPath())) {
            String[] metaList = n5Reader.deepList("/");
            ArrayList<String> fList = new ArrayList<>();
            for (String s : metaList) {
                if (n5Reader.datasetExists(s)) {
                    fList.add(s);
                };
            }
            return fList;
        }
    }

    public void setSelectedLocalFile(File selectedLocalFile){
        this.selectedLocalFile = selectedLocalFile;
    }
    public File getSelectedLocalFile() {
        return selectedLocalFile;
    }

    public ImagePlus getImgPlusFromN5File() throws IOException {
        return getImgPlusFromN5File(dataSetChosen);
    }

    public ImagePlus getImgPlusFromN5File(String selectedDataset) throws IOException {
        N5Reader n5Reader = selectedLocalFile != null ? getN5FSReader() : getN5AmazonS3Reader();
        // Theres definitly a better way to do this, I trie using a variable to change the cached cells first parameter type but it didn't seem to work :/
        switch (n5Reader.getDatasetAttributes(selectedDataset).getDataType()){
            case UINT8:
                return ImageJFunctions.wrap((CachedCellImg<UnsignedByteType, ?>) N5Utils.open(n5Reader, selectedDataset), selectedDataset);
            case UINT16:
                return ImageJFunctions.wrap((CachedCellImg<UnsignedShortType, ?>)N5Utils.open(n5Reader, selectedDataset), selectedDataset);
            case UINT32:
                return ImageJFunctions.wrap((CachedCellImg<UnsignedIntType, ?>)N5Utils.open(n5Reader, selectedDataset), selectedDataset);
            case INT8:
                return ImageJFunctions.wrap((CachedCellImg<ByteType, ?>)N5Utils.open(n5Reader, selectedDataset), selectedDataset);
            case INT16:
                return ImageJFunctions.wrap((CachedCellImg<ShortType, ?>)N5Utils.open(n5Reader, selectedDataset), selectedDataset);
            case INT32:
                return ImageJFunctions.wrap((CachedCellImg<IntType, ?>)N5Utils.open(n5Reader, selectedDataset), selectedDataset);
            case FLOAT32:
                return ImageJFunctions.wrap((CachedCellImg<FloatType, ?>)N5Utils.open(n5Reader, selectedDataset), selectedDataset);
            case FLOAT64:
                return ImageJFunctions.wrap((CachedCellImg<DoubleType, ?>)N5Utils.open(n5Reader, selectedDataset), selectedDataset);
//                final ARGBARGBDoubleConverter<ARGBDoubleType> converters = new ARGBARGBDoubleConverter<>();
//                final CachedCellImg<DoubleType, ?> cachedCellImg = N5Utils.open(n5Reader, selectedDataset);
//                return ImageJFunctions.showRGB(cachedCellImg, converters, "");
        }
        return null;
    }

    public void setURI(URI uri){
        this.uri = uri;
        if(!(uri.getQuery() == null)){
            dataSetChosen = uri.getQuery().split("=")[1]; // query should be "dataSetName=name", thus splitting it by = and getting the second entry gives the name
        }
    }

    public void setDataSetChosen(String dataSetChosen){
        this.dataSetChosen = dataSetChosen;
    }
}
