package org.vcell.vcellfiji;


import net.imagej.ImageJ;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import org.janelia.saalfeldlab.n5.metadata.N5Metadata;
import org.janelia.saalfeldlab.n5.metadata.N5MetadataParser;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;
import ij.IJ;
import org.janelia.saalfeldlab.n5.*;
import org.janelia.saalfeldlab.n5.imglib2.*;
import net.imglib2.img.display.imagej.*;
import org.vcell.vcellfiji.VCellGUI;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Command plugins
@Plugin(type = Command.class, menuPath = "Plugins>VCell>ImageHandler")
public class N5ImageHandler implements Command{

    private N5TreeNode rootNode;


    @Override
    public void run() {
        VCellGUI vGui = new VCellGUI();
        vGui.localFileDialog.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                readN5Files(vGui.localFileDialog.getSelectedFile());
            }
        });
//        readN5Files();

    }


    private void readN5Files(File n5File){
        // auto closes reader
        try (N5FSReader n5FSReader = new N5FSReader(n5File.getPath())) {
            ExecutorService loaderExecuter = Executors.newCachedThreadPool();
            ArrayList<N5MetadataParser<?>> groupParsers = new ArrayList<>();
            ArrayList<N5MetadataParser<?>> parserList = new ArrayList<>();
            N5TreeNode rootPath = new N5TreeNode(n5File.getPath());

            N5DatasetDiscoverer datasetDiscoverer = new N5DatasetDiscoverer(n5FSReader, loaderExecuter, parserList, groupParsers);

//            String[] datasetPaths = n5FSReader.deepList(n5File.getPath(), loaderExecuter);


//            https://github.com/saalfeldlab/n5-imglib2/blob/master/src/test/java/org/janelia/saalfeldlab/n5/imglib2/N5DatasetViewerExample.java
            N5TreeNode n5TreeNode = datasetDiscoverer.discoverAndParseRecursive("/");
            String[] metaList = n5FSReader.deepList("/");
            ArrayList<String> fList = new ArrayList<>();

            for (String s : metaList) {
                if (n5FSReader.datasetExists(s)) {
                    fList.add(s);
                };
            }


            CachedCellImg<UnsignedShortType, ?> n5imp = N5Utils.open(n5FSReader, fList.get(0));
            ImageJFunctions.show(n5imp);
//            System.out.print(n5FSReader.toString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        new N5ImageHandler().run();
    }
}
