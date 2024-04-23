package org.vcell.N5;


import org.scijava.log.slf4j.SLF4JLogService;
import org.vcell.N5.UI.N5ExportTable;
import org.vcell.N5.UI.N5ViewerGUI;
import ij.ImagePlus;
import ij.plugin.Duplicator;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URI;


/*
    Able to open N5 files locally, display the datasets that can be chosen from it, and open the datasets within ImageJ.
    Flow of operations is select an N5 file, get dataset list, select a dataset and then open it.
    http://vcellapi-beta.cam.uchc.edu:8088/test/test.n5

 */

@Plugin(type = Command.class, menuPath = "Plugins>VCell>VCell Simulation Results Viewer")
public class N5ImageHandler implements Command, ActionListener {
    private N5ViewerGUI vGui;
    public static final String formatName = "N5";
    private SimResultsLoader simResultsLoader;
    @Parameter
    public static LogService logService;

    @Override
    public void actionPerformed(ActionEvent e) {
        enableCriticalButtons(false);

        // generate dataset list
        if (e.getSource() == vGui.remoteFileSelection.submitS3Info || e.getSource() == vGui.mostRecentExport){
            simResultsLoader = new SimResultsLoader();
            SwingWorker n5DatasetListUpdater = new SwingWorker() {
                @Override
                protected Object doInBackground() throws Exception {
                    vGui.setCursor(new Cursor(Cursor.WAIT_CURSOR));
                    String uri = "";
                    if (e.getSource() == vGui.remoteFileSelection.submitS3Info) {
                        vGui.remoteFileSelection.setCursor(new Cursor(Cursor.WAIT_CURSOR));
                        uri = vGui.remoteFileSelection.getS3URL();
                    } else if (e.getSource() == vGui.mostRecentExport) {
                        ExportDataRepresentation.SimulationExportDataRepresentation lastElement = N5ExportTable.getLastJSONElement();
                        if (lastElement != null) {
                            uri = lastElement.uri;
                            simResultsLoader.setDataSetChosen(lastElement.savedFileName);
                        }
                    }
                    simResultsLoader.setURI(URI.create(uri));
                    simResultsLoader.createS3Client();
                    displayN5Dataset(simResultsLoader.getImgPlusFromN5File());

                    return null;
                }

                @Override
                protected void done() {
                    vGui.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                    enableCriticalButtons(true);
                    try {
                        if (e.getSource() == vGui.remoteFileSelection.submitS3Info) {
                            vGui.remoteFileSelection.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                            vGui.remoteFileSelection.dispose();
                        }
                    } catch (Exception exception) {
                        logService.error(exception);
                    }
                }
            };
            N5ImageHandler.logService.debug("Generating Dataset List");
            n5DatasetListUpdater.execute();
        }
        // https://stackoverflow.com/questions/16937997/java-swingworker-thread-to-update-main-gui
        // Why swing updating does not work


    }

    public void enableCriticalButtons(boolean enable) {
        logService.debug("Disabling Critical Buttons");
        vGui.remoteFileSelection.submitS3Info.setEnabled(enable);
        vGui.okayButton.setEnabled(enable);
        vGui.localFiles.setEnabled(enable);
        vGui.remoteFiles.setEnabled(enable);
        vGui.mostRecentExport.setEnabled(enable);
        vGui.exportTableButton.setEnabled(enable);
        logService.debug("Enabling Critical Buttons");
        logService.error("Test Error");
    }

    static {

    }

    @Override
    public void run() {
//        this.vGui = new N5ViewerGUI(this);
//        this.vGui.localFileDialog.addActionListener(this);
//        this.vGui.okayButton.addActionListener(this);
////        this.vGui.exportTableButton.addActionListener(this);
//
//        this.vGui.remoteFileSelection.submitS3Info.addActionListener(this);
//        this.vGui.mostRecentExport.addActionListener(this);
        N5ExportTable exportTable = new N5ExportTable(this);
        if(N5ImageHandler.logService == null){
            N5ImageHandler.logService = new SLF4JLogService();
        }
        N5ImageHandler.logService.setLevel(LogService.DEBUG);
        exportTable.displayExportTable();
    }

    public void displayN5Dataset(ImagePlus imagePlus) throws IOException {
        if (this.vGui.openMemoryCheckBox.isSelected()){
            ImagePlus memoryImagePlus = new Duplicator().run(imagePlus);
            memoryImagePlus.show();
        }
        else{
            imagePlus.show();
        }
    }

    public static LogService getLogger(){
        if (logService == null){
            logService = new SLF4JLogService();
        }
        return logService;
    }

    public static void main(String[] args) {
        N5ImageHandler n5ImageHandler = new N5ImageHandler();
        n5ImageHandler.run();
    }
}
