package org.vcell.vcellfiji;


import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.AnonymousAWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.AmazonS3URI;
import ij.ImagePlus;
import ij.plugin.Duplicator;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.integer.*;
import net.imglib2.type.numeric.real.FloatType;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.s3.N5AmazonS3Reader;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;
import org.janelia.saalfeldlab.n5.*;
import org.vcell.vcellfiji.UI.VCellGUI;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

// Command plugins
/* TODO:
     Security check for the S3Client builder, there seems to be an encrypted version of it and that should be checked
     Test cases for handling all cases regarding



 */

/*
    Able to open N5 files locally, display the datasets that can be chosen from it, and open the datasets within ImageJ.
    Flow of operations is select an N5 file, get dataset list, select a dataset and then open it.
 */

@Plugin(type = Command.class, menuPath = "Plugins>VCell>Image Handler1")
public class N5ImageHandler implements Command{
    private VCellGUI vGui;
    private File selectedLocalFile;
    private AmazonS3 s3Client;
    private String bucketName;
    private AmazonS3URI s3URI;

    @Override
    public void run() {
        this.vGui = new VCellGUI();
        this.vGui.localFileDialog.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectedLocalFile = vGui.localFileDialog.getSelectedFile();
                displayN5Results(getN5DatasetList());
            }
        });

        this.vGui.okayButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadN5Dataset(vGui.datasetList.getSelectedValue());
            }
        });

        this.vGui.remoteFileSelection.submitS3Info.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                createS3Client(vGui.remoteFileSelection.getS3URL(), vGui.remoteFileSelection.returnCredentials(), vGui.remoteFileSelection.returnEndpoint());
                ArrayList<String> list = getS3N5DatasetList();
                displayN5Results(list);
            }
        });
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
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void displayN5Results(ArrayList<String> datasetList){
        SwingUtilities.invokeLater(() ->{
            this.vGui.updateDatasetList(datasetList);
        });
    }


    public ImagePlus getImgPlusFromN5File(String selectedDataset, N5Reader n5Reader) throws IOException {
//        N5CosemMetadataParser metadataParser = new N5CosemMetadataParser();
//        CosemToImagePlus cosemToImagePlus = new CosemToImagePlus();

        // Theres definitly a better way to do this, I trie using a variable to change the cached cells first parameter type but it didn't seem to work :/
        switch (n5Reader.getDatasetAttributes(selectedDataset).getDataType()){
            case UINT8:
                return ImageJFunctions.wrap((CachedCellImg<UnsignedByteType, ?>)N5Utils.open(n5Reader, selectedDataset), "");
            case UINT16:
                return ImageJFunctions.wrap((CachedCellImg<UnsignedShortType, ?>)N5Utils.open(n5Reader, selectedDataset), "");
            case UINT32:
                return ImageJFunctions.wrap((CachedCellImg<UnsignedIntType, ?>)N5Utils.open(n5Reader, selectedDataset), "");
            case INT8:
                return ImageJFunctions.wrap((CachedCellImg<ByteType, ?>)N5Utils.open(n5Reader, selectedDataset), "");
            case INT16:
                return ImageJFunctions.wrap((CachedCellImg<ShortType, ?>)N5Utils.open(n5Reader, selectedDataset), "");
            case INT32:
                return ImageJFunctions.wrap((CachedCellImg<IntType, ?>)N5Utils.open(n5Reader, selectedDataset), "");
            case FLOAT32:
                return ImageJFunctions.wrap((CachedCellImg<FloatType, ?>)N5Utils.open(n5Reader, selectedDataset), "");
        }
        return null;
    }


    private void displayN5DatasetList(ImagePlus imagePlus){
        if (this.vGui.openVirtualCheckBox.isSelected()){
            imagePlus.show();
        }
        else{
            ImagePlus memoryImagePlus = new Duplicator().run(imagePlus);
            memoryImagePlus.show();
        }
    }

    public void loadN5Dataset(String selectedDataset){
        if(selectedLocalFile != null) {
            try (N5FSReader n5FSReader = new N5FSReader(selectedLocalFile.getPath())) {
                ImagePlus imagePlus = this.getImgPlusFromN5File(selectedDataset, n5FSReader);
                this.displayN5DatasetList(imagePlus);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        else {
            try (N5AmazonS3Reader n5AmazonS3Reader = new N5AmazonS3Reader(this.s3Client, this.bucketName, this.s3URI.getKey())) {
                ImagePlus imagePlus = this.getImgPlusFromN5File(selectedDataset, n5AmazonS3Reader);
                this.displayN5DatasetList(imagePlus);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    // https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/java-dg-region-selection.html
    // Chain of handling region is what gets set by client builder
    // AWS_REGION Environmental variable on computer
    // The AWS default profile on the computer
    // Use Amazon Elastic computing instance metadata to determine region
        // Does this mean that region is only known for buckets hosted on Amazons servers? Is that why the error is thrown? Should I use the EC2 builder to find the region?
    // Throw error

    //Can make it so that our bucket links contain the region in it, making it significantly easier to determine it
    public void createS3Client(String url, HashMap<String, String> credentials, HashMap<String, String> endpoint){
        AmazonS3ClientBuilder s3ClientBuilder = AmazonS3ClientBuilder.standard();
        this.s3URI = new AmazonS3URI(URI.create(url));
        this.bucketName = endpoint == null ? s3URI.getBucket(): endpoint.get("BucketName");
//        String bucketLocation = AmazonS3ClientBuilder.standard().withRegion(Regions.US_EAST_1).build().getBucketLocation(this.bucketName);
//        System.out.print(bucketLocation);
        if(credentials != null){
            s3ClientBuilder.withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(credentials.get("AccessKey"), credentials.get("SecretKey"))));
        }
        if(endpoint != null){
            s3ClientBuilder.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpoint.get("Endpoint"), endpoint.get("Region")));
            this.s3Client = s3ClientBuilder.build();
            return;
        }
        // if nothing is given, default user and return so that code after if statement does not execute
        if(endpoint == null && credentials == null){
            this.s3Client = AmazonS3ClientBuilder.standard().withRegion("site-low").withCredentials(new AWSStaticCredentialsProvider(new AnonymousAWSCredentials())).build();
            return;
        }

        //  TODO: hard coding a region is not a solution
        this.s3Client = s3ClientBuilder.withRegion("site2-low").build();
    }


    public ArrayList<String> getS3N5DatasetList(){

        // used as a flag to tell that remote access is occurring, and that there is no local files
        selectedLocalFile = null;

        try(N5AmazonS3Reader n5AmazonS3Reader = new N5AmazonS3Reader(this.s3Client, this.bucketName)) {
            return new ArrayList<>(Arrays.asList(n5AmazonS3Reader.deepListDatasets(s3URI.getKey())));
        }
        catch (IOException e){
            throw new RuntimeException(e);
        }
    }


    public void setSelectedLocalFile(File selectedLocalFile){
        this.selectedLocalFile = selectedLocalFile;
    }
    public File getSelectedLocalFile() {
        return selectedLocalFile;
    }

    public static void main(String[] args) {
//        new N5ImageHandler().getS3N5DatasetList("s3://janelia-cosem-datasets/jrc_macrophage-2/jrc_macrophage-2.n5", null, null);
        new N5ImageHandler().run();
    }
}
