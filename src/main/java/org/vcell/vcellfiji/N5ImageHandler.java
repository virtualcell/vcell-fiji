package org.vcell.vcellfiji;


import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.AnonymousAWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.Region;
import ij.ImagePlus;
import ij.plugin.Duplicator;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.integer.*;
import net.imglib2.type.numeric.real.FloatType;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.s3.N5AmazonS3Reader;
import org.jcodings.util.Hash;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;
import org.janelia.saalfeldlab.n5.*;
import org.vcell.vcellfiji.UI.VCellGUI;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

// Command plugins

/*
    Able to open N5 files locally, display the datasets that can be chosen from it, and open the datasets within ImageJ.
    Flow of operations is select an N5 file, get dataset list, select a dataset and then open it.
 */

@Plugin(type = Command.class, menuPath = "Plugins>VCell>ImageHandler")
public class N5ImageHandler implements Command{
    private VCellGUI vGui;
    private File selectedFile;

    private AmazonS3 s3Client;
    private String bucketName;

    @Override
    public void run() {
        this.vGui = new VCellGUI();
        this.vGui.localFileDialog.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectedFile = vGui.localFileDialog.getSelectedFile();
                displayN5Results();
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
                getS3N5DatasetList(vGui.remoteFileSelection.getS3URL(), vGui.remoteFileSelection.returnEndpoint(), vGui.remoteFileSelection.returnCredentials());
            }
        });
    }

    public ArrayList<String> getN5DatasetList(){
        // auto closes reader
        if (selectedFile != null) {
            try (N5FSReader n5Reader = new N5FSReader(selectedFile.getPath())) {
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
        else{
            // when dealing with the S3 buckets using the deeplist function does not seem to work with it, so instead just list the prefixes, and ensure that they are data sets
            return new ArrayList<>();
        }
    }

    public void displayN5Results(){
        ArrayList<String> datasetList = this.getN5DatasetList();
        SwingUtilities.invokeLater(() ->{
            this.vGui.updateDatasetList(datasetList);
        });
    }


    public ImagePlus getImgPlusFromN5File(String selectedDataset, N5Reader n5Reader) throws IOException {
//        N5CosemMetadataParser metadataParser = new N5CosemMetadataParser();
//        CosemToImagePlus cosemToImagePlus = new CosemToImagePlus();

//            DatasetAttributes datasetAttributes = n5FSWriter.getDatasetAttributes(selectedDataset);
//            DataType dataType = datasetAttributes.getDataType();
//            CachedCellImg<UnsignedShortType, ?> n5imp = N5Utils.open(n5FSWriter, selectedDataset);

//           Using the N5IJUtils library allows for the data type from N5 files to automatically be handled


//        CachedCellImg<UnsignedLongType, ?> cachedCellImg = N5Utils.open(n5Reader, selectedDataset);
//        ImagePlus imPlus = ImageJFunctions.wrap(cachedCellImg, "");
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


//        ImagePlus imagePlus = ImageJFunctions.show(cachedCellImg);
//        ImagePlus imagePlus = N5IJUtils.load(n5Writer, selectedDataset, metadataParser, cosemToImagePlus);
//        imagePlus.show();
    }


    private void displayN5Dataset(ImagePlus imagePlus){
        if (this.vGui.openVirtualCheckBox.isSelected()){
            imagePlus.show();
        }
        else{
            ImagePlus memoryImagePlus = new Duplicator().run(imagePlus);
            memoryImagePlus.show();
        }
    }

    public void loadN5Dataset(String selectedDataset){
        if(selectedFile != null) {
            try (N5FSReader n5FSReader = new N5FSReader(selectedFile.getPath())) {
                ImagePlus imagePlus = this.getImgPlusFromN5File(selectedDataset, n5FSReader);
                this.displayN5Dataset(imagePlus);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        else {
            try (N5AmazonS3Reader n5AmazonS3Reader = new N5AmazonS3Reader(this.s3Client, this.bucketName, "/test_image.n5")) {
                this.getImgPlusFromN5File(selectedDataset, n5AmazonS3Reader);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }


    public void getS3N5DatasetList(String url, HashMap<String, String> credentials, HashMap<String, String> endpoint){
        System.out.print("h");
    }

    public void S3Acess(){
        BasicAWSCredentials basicAWSCredentials = new BasicAWSCredentials("MKPOPK1MYZQ0U93CUL63", "WT6yHX2awaLr0Ua4VnFK7+dv8oGOHncmk8XAXZkQ");

        this.s3Client = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(basicAWSCredentials))
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration("http://155.37.247.226", "site2-low"))
//                .withRegion("us-east-1")
                .build();
        final ListObjectsV2Request request = new ListObjectsV2Request();
        this.bucketName = "vcelldev";
        request.setBucketName(this.bucketName);
        request.setDelimiter("/");
        request.setPrefix("N5/");


//
//        this.displayN5Results();
//
//
//
        s3Client.listObjectsV2(request).getObjectSummaries().forEach(object -> {System.out.print(object.getKey() + "\n");});
        s3Client.listObjectsV2(request).getCommonPrefixes().forEach(object -> {System.out.print("Prefix: " + object + "\n");});
//        s3Client.listBuckets().forEach(bucket -> {System.out.print(bucket.getName());});
        try(N5AmazonS3Reader n5AmazonS3Reader = new N5AmazonS3Reader(s3Client, this.bucketName, "N5/mitosis.n5");){
            String[] deepList = n5AmazonS3Reader.deepList("/"); // A better approach will be to just list all of the different prefixes, and then use a isDataset function on it
            System.out.print("");

            // Seems to work with the native N5Utils but when trying to do it with the N5IJUtils some funky stuff happens.
            CachedCellImg<UnsignedByteType, ?> cachedCellImg = N5Utils.open(n5AmazonS3Reader, "ij");
            ImagePlus imagePlus = ImageJFunctions.show(cachedCellImg);
//            ImagePlus imagePlus = N5IJUtils.load(n5AmazonS3Writer, "test/c0/s0");
//            imagePlus.show();
        }
        catch (IOException e){
            throw new RuntimeException(e);
        }

    }


    public void setSelectedFile(File selectedFile){
        this.selectedFile = selectedFile;
    }
    public File getSelectedFile() {
        return selectedFile;
    }

    public static void main(String[] args) {
        new N5ImageHandler().run();
    }
}
