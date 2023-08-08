package org.vcell.vcellfiji;


import ij.ImagePlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.img.basictypeaccess.DataAccess;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.integer.*;
import net.imglib2.type.numeric.real.*;
//import org.janelia.saalfeldlab.n5.ij.N5IJUtils;
import org.janelia.saalfeldlab.n5.ij.N5IJUtils;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.metadata.N5CosemMetadata;
import org.janelia.saalfeldlab.n5.metadata.N5CosemMetadataParser;
//import org.janelia.saalfeldlab.n5.metadata.imagej.CosemToImagePlus;
import org.janelia.saalfeldlab.n5.metadata.imagej.CosemToImagePlus;
import org.scijava.command.Command;
//import org.janelia.saalfeldlab.n5.blosc.BloscCompression;
import org.scijava.plugin.Plugin;
import org.janelia.saalfeldlab.n5.*;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

// Command plugins
@Plugin(type = Command.class, menuPath = "Plugins>VCell>ImageHandler")
public class N5ImageHandler <T extends NativeType<T>> implements Command{
    private VCellGUI vGui;
    private File selectedFile;

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

        this.vGui.datasetList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                loadN5Dataset(vGui.datasetList.getSelectedValue());
            }
        });
    }

    public ArrayList<String> getN5DatasetList(){
        // auto closes reader
        try (N5FSReader n5FSReader = new N5FSReader(selectedFile.getPath())) {
            String[] metaList = n5FSReader.deepList("/");
            ArrayList<String> fList = new ArrayList<>();
            for (String s : metaList) {
                if (n5FSReader.datasetExists(s)) {
                    fList.add(s);
                };
            }
            return fList;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void displayN5Results(){
        ArrayList<String> datasetList = this.getN5DatasetList();
        SwingUtilities.invokeLater(() ->{
            this.vGui.updateDatasetList(datasetList);
        });
    }



    public void loadN5Dataset(String selectedDataset){
        try (N5FSWriter n5FSWriter = new N5FSWriter(selectedFile.getPath())){
            N5CosemMetadataParser metadataParser = new N5CosemMetadataParser();
            CosemToImagePlus cosemToImagePlus = new CosemToImagePlus();

//            DatasetAttributes datasetAttributes = n5FSWriter.getDatasetAttributes(selectedDataset);
//            DataType dataType = datasetAttributes.getDataType();
//            CachedCellImg<UnsignedShortType, ?> n5imp = N5Utils.open(n5FSWriter, selectedDataset);

            ImagePlus imagePlus = N5IJUtils.load(n5FSWriter, selectedDataset, metadataParser, cosemToImagePlus);
            imagePlus.show();
        } catch (IOException e) {
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
