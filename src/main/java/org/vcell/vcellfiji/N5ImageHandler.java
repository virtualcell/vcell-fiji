package org.vcell.vcellfiji;


import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.AnonymousAWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.AmazonS3URI;
import ij.ImagePlus;
import ij.plugin.Duplicator;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.integer.*;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.s3.N5AmazonS3Reader;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.janelia.saalfeldlab.n5.*;
import org.vcell.vcellfiji.UI.N5ViewerGUI;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;


/*
    Able to open N5 files locally, display the datasets that can be chosen from it, and open the datasets within ImageJ.
    Flow of operations is select an N5 file, get dataset list, select a dataset and then open it.
    http://vcellapi-beta.cam.uchc.edu:8088/test/test.n5

 */

@Plugin(type = Command.class, menuPath = "Plugins>VCell>N5 Dataset Viewer")
public class N5ImageHandler implements Command, ActionListener {
    private N5ViewerGUI vGui;
    private File selectedLocalFile;
    private AmazonS3 s3Client;
    private String bucketName;
    private String s3ObjectKey;
    @Parameter
    private LogService logService;
    private SwingWorker<ArrayList<String>, ArrayList<String>> n5DatasetListUpdater;
    @Override
    public void actionPerformed(ActionEvent e) {
        enableCriticalButtons(false);

        if(e.getSource() == vGui.localFileDialog){
            n5DatasetListUpdater= new SwingWorker<ArrayList<String>, ArrayList<String>>() {
                @Override
                protected ArrayList<String> doInBackground() throws Exception {
                    selectedLocalFile = vGui.localFileDialog.getSelectedFile();
                    ArrayList<String> n5DataSetList = getN5DatasetList();
                    publish(n5DataSetList);
                    return null;
                }

                @Override
                protected void done() {
                    enableCriticalButtons(true);
                    try{
                        if(vGui.jFileChooserResult == JFileChooser.APPROVE_OPTION){
                            get();
                        }
                    }
                    catch (Exception exception){
                        logService.error(exception);
                    }
                }

                @Override
                protected void process(List<ArrayList<String>> chunks) {
                    displayN5Results(chunks.get(0));
                }
            };
            n5DatasetListUpdater.execute();
        }

        else if (e.getSource() == vGui.okayButton) {
            SwingWorker swingWorker = new SwingWorker() {
                @Override
                protected Object doInBackground() throws Exception {
                    loadN5Dataset(vGui.datasetList.getSelectedValue());
                    return null;
                }

                @Override
                protected void done() {
                    enableCriticalButtons(true);
                    try{
                        get();
                    }
                    catch (Exception exception){
                        logService.error(exception);
                    }
                }
            };
            swingWorker.execute();
        }
        // https://stackoverflow.com/questions/16937997/java-swingworker-thread-to-update-main-gui
        // Why swing updating does not work

        else if (e.getSource() == vGui.remoteFileSelection.submitS3Info){
            n5DatasetListUpdater= new SwingWorker<ArrayList<String>, ArrayList<String>>() {
                @Override
                protected ArrayList<String> doInBackground() throws Exception {
                    createS3Client(vGui.remoteFileSelection.getS3URL(), vGui.remoteFileSelection.returnCredentials(), vGui.remoteFileSelection.returnEndpoint());
                    ArrayList<String> list = getS3N5DatasetList();
                    publish(list);
                    return null;
                }

                @Override
                protected void done() {
                    enableCriticalButtons(true);
                    try{
                        get();
                        vGui.remoteFileSelection.dispose();
                    }
                    catch (Exception exception){
                        logService.error(exception);
                    }
                }

                @Override
                protected void process(List<ArrayList<String>> chunks) {
                    displayN5Results(chunks.get(0));
                }
            };
            n5DatasetListUpdater.execute();
            n5DatasetListUpdater.getState();
        }
    }

    private void enableCriticalButtons(boolean enable) {
        vGui.remoteFileSelection.submitS3Info.setEnabled(enable);
        vGui.okayButton.setEnabled(enable);
        vGui.localFiles.setEnabled(enable);
        vGui.remoteFiles.setEnabled(enable);
    }

    @Override
    public void run() {
        this.vGui = new N5ViewerGUI();
        this.vGui.localFileDialog.addActionListener(this);

        this.vGui.okayButton.addActionListener(this);

        this.vGui.remoteFileSelection.submitS3Info.addActionListener(this);
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
        this.vGui.updateDatasetList(datasetList);
    }


    public ImagePlus getImgPlusFromN5File(String selectedDataset, N5Reader n5Reader) throws IOException {
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
            case FLOAT64:
                return ImageJFunctions.wrap((CachedCellImg<DoubleType, ?>)N5Utils.open(n5Reader, selectedDataset), "");
//                final ARGBARGBDoubleConverter<ARGBDoubleType> converters = new ARGBARGBDoubleConverter<>();
//                final CachedCellImg<DoubleType, ?> cachedCellImg = N5Utils.open(n5Reader, selectedDataset);
//                return ImageJFunctions.showRGB(cachedCellImg, converters, "");
        }
        return null;
    }


    private void displayN5Dataset(ImagePlus imagePlus){
        if (this.vGui.openMemoryCheckBox.isSelected()){
            ImagePlus memoryImagePlus = new Duplicator().run(imagePlus);
            memoryImagePlus.show();
        }
        else{
            imagePlus.show();
        }
    }

    public N5AmazonS3Reader getN5AmazonS3Reader(){
        try(N5AmazonS3Reader n5AmazonS3Reader = new N5AmazonS3Reader(this.s3Client, this.bucketName, this.s3ObjectKey)){
            return n5AmazonS3Reader;
        }
        catch (IOException e){
            throw new RuntimeException(e);
        }
    }

    public N5FSReader getN5FSReader(){
        try (N5FSReader n5FSReader = new N5FSReader(selectedLocalFile.getPath())) {
            return n5FSReader;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void loadN5Dataset(String selectedDataset) throws IOException {
        if(selectedLocalFile != null) {
            ImagePlus imagePlus = this.getImgPlusFromN5File(selectedDataset, this.getN5FSReader());
            this.displayN5Dataset(imagePlus);
        }
        else {
            ImagePlus imagePlus = this.getImgPlusFromN5File(selectedDataset, this.getN5AmazonS3Reader());
            this.displayN5Dataset(imagePlus);
        }
    }

    //When creating client's try to make one for an Amazon link, otherwise use our custom url scheme
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


    public ArrayList<String> getS3N5DatasetList(){

        // used as a flag to tell that remote access is occurring, and that there is no local files
        selectedLocalFile = null;

        try(N5AmazonS3Reader n5AmazonS3Reader = new N5AmazonS3Reader(this.s3Client, this.bucketName)) {
            return new ArrayList<>(Arrays.asList(n5AmazonS3Reader.deepListDatasets(this.s3ObjectKey)));
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
        N5ImageHandler n5ImageHandler = new N5ImageHandler();
        n5ImageHandler.run();
    }
}
