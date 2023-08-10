package org.vcell.vcellfiji;


import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.AnonymousAWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import ij.ImagePlus;
//import org.janelia.saalfeldlab.n5.ij.N5IJUtils;
import ij.plugin.Duplicator;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.type.numeric.integer.UnsignedLongType;
//import org.janelia.saalfeldlab.n5.ij.N5IJUtils;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
//import org.janelia.saalfeldlab.n5.metadata.N5CosemMetadataParser;
//import org.janelia.saalfeldlab.n5.metadata.imagej.CosemToImagePlus;
//import org.janelia.saalfeldlab.n5.metadata.imagej.CosemToImagePlus;
import org.janelia.saalfeldlab.n5.s3.N5AmazonS3Reader;
import org.janelia.saalfeldlab.n5.s3.N5AmazonS3Writer;
import org.scijava.command.Command;
//import org.janelia.saalfeldlab.n5.blosc.BloscCompression;
import org.scijava.plugin.Plugin;
import org.janelia.saalfeldlab.n5.*;
import org.vcell.vcellfiji.UI.VCellGUI;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

// Command plugins

/*
    Able to open N5 files locally, display the datasets that can be chosen from it, and open the datasets within ImageJ.
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
//        readN5Files();

        this.vGui.okayButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadN5Dataset(vGui.datasetList.getSelectedValue());
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


    private void loadN5DatasetHelper (String selectedDataset, N5Writer n5Writer) throws IOException {
//        N5CosemMetadataParser metadataParser = new N5CosemMetadataParser();
//        CosemToImagePlus cosemToImagePlus = new CosemToImagePlus();

//            DatasetAttributes datasetAttributes = n5FSWriter.getDatasetAttributes(selectedDataset);
//            DataType dataType = datasetAttributes.getDataType();
//            CachedCellImg<UnsignedShortType, ?> n5imp = N5Utils.open(n5FSWriter, selectedDataset);

//           Using the N5IJUtils library allows for the data type from N5 files to automatically be handled

        CachedCellImg<UnsignedLongType, ?> cachedCellImg = N5Utils.open(n5Writer, selectedDataset);
        ImagePlus imagePlus = ImageJFunctions.show(cachedCellImg);
        new Duplicator().run(imagePlus).show();
//        ImagePlus imagePlus = N5IJUtils.load(n5Writer, selectedDataset, metadataParser, cosemToImagePlus);
//        imagePlus.show();
    }

    public void loadN5Dataset(String selectedDataset){
        if(selectedFile != null) {
            try (N5FSWriter n5FSWriter = new N5FSWriter(selectedFile.getPath())) {
                this.loadN5DatasetHelper(selectedDataset, n5FSWriter);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        else {
            try (N5AmazonS3Writer n5AmazonS3Writer = new N5AmazonS3Writer(this.s3Client, this.bucketName, "/test_image.n5")) {
                this.loadN5DatasetHelper(selectedDataset, n5AmazonS3Writer);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void S3Acess(){
        BasicAWSCredentials basicAWSCredentials = new BasicAWSCredentials("", "");

        this.s3Client = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(new AnonymousAWSCredentials()))
//                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration("http://155.37.247.226", "site2-low"))
                .withRegion("us-east-1")
                .build();
        final ListObjectsV2Request request = new ListObjectsV2Request();
        this.bucketName = "janelia-cosem-datasets";
        request.setBucketName(this.bucketName);
        request.setDelimiter("/");
        request.setPrefix("jrc_hela-2/jrc_hela-2.n5/");
//
//        this.displayN5Results();
//
//
//
        s3Client.listObjectsV2(request).getObjectSummaries().forEach(object -> {System.out.print(object.getKey() + "\n");});
        s3Client.listObjectsV2(request).getCommonPrefixes().forEach(object -> {System.out.print("Prefix: " + object + "\n");});
//        s3Client.listBuckets().forEach(bucket -> {System.out.print(bucket.getName());});
        try(N5AmazonS3Reader n5AmazonS3Reader = new N5AmazonS3Reader(s3Client, this.bucketName, "janelia-cosem-datasets/jrc_hela-2/jrc_hela-2.n5/");){
            String[] deepList = n5AmazonS3Reader.deepList("/"); // A better approach will be to just list all of the different prefixes, and then use a isDataset function on it
            System.out.print("");
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
