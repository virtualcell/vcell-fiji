package org.vcell.N5;


import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.log.Logger;
import org.scijava.log.slf4j.SLF4JLogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.vcell.N5.UI.MainPanel;
import org.vcell.N5.retrieving.LoadingManager;
import org.vcell.N5.retrieving.SimResultsLoader;

import java.io.*;
import java.net.URL;


/*
    Able to open N5 files locally, display the datasets that can be chosen from it, and open the datasets within ImageJ.
    Flow of operations is select an N5 file, get dataset list, select a dataset and then open it.
    http://vcellapi-beta.cam.uchc.edu:8088/test/test.n5

 */

@Plugin(type = Command.class, menuPath = "Plugins>VCell>VCell Simulation Results Viewer")
public class N5ImageHandler implements Command {
    public static final String formatName = "N5";
    @Parameter
    public static LogService logService;
    public static MainPanel exportTable;
    public static String exportedMetaDataPath = System.getProperty("user.home") + "/.vcell/exportMetaData.json";
    private static ExportDataRepresentation.FormatExportDataRepresentation exampleJSONData;
    public static LoadingManager loadingManager;

    @Override
    public void run() {
        initializeLogService();
        loadingManager = new LoadingManager();
        exportTable = new MainPanel();
        MainPanel.controlButtonsPanel.setStateToInitializing(true);
        setExampleJSONData();
//        N5ImageHandler.logService.setLevel(LogService.DEBUG);
        Thread thread = new Thread(() -> {
            // For some reason getting a standard client takes three seconds.
            // So create one upon initialization, while the user is focused on the GUI
            // and by the time they open an Image it's already loaded.
            SimResultsLoader.s3ClientBuilder = AmazonS3ClientBuilder.standard();
            MainPanel.controlButtonsPanel.setStateToInitializing(false);
            MainPanel.n5ExportTable.valueChanged(null);
        });
        thread.start();
    }

    public static Logger getLogger(Class classToLog){
        return logService.subLogger(classToLog.getCanonicalName(), LogService.DEBUG);
    }

    public static void initializeLogService(){
        if (N5ImageHandler.logService == null){
            N5ImageHandler.logService = new SLF4JLogService();
            N5ImageHandler.logService.initialize();
        }
    }

    public static void main(String[] args) {
        N5ImageHandler n5ImageHandler = new N5ImageHandler();
        initializeLogService();
        setExampleJSONData();
        n5ImageHandler.run();
    }

    public static boolean exportedDataExists(){
        File jsonFile = new File(exportedMetaDataPath);
        return jsonFile.exists();
    }

    public static ExportDataRepresentation.FormatExportDataRepresentation getJsonData() throws FileNotFoundException {
        File jsonFile = new File(exportedMetaDataPath);
        if (jsonFile.exists() && jsonFile.length() != 0){
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            return gson.fromJson(new FileReader(jsonFile.getAbsolutePath()), ExportDataRepresentation.class).formatData.get(N5ImageHandler.formatName);
        }
        return null;

    }

    private static void setExampleJSONData(){
        try(BufferedInputStream remoteJSONFile = new BufferedInputStream(new URL("https://vcell.org/export/n5examples.json").openStream())){
            InputStreamReader remoteJSONFileReader = new InputStreamReader(remoteJSONFile);
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            exampleJSONData = gson.fromJson(remoteJSONFileReader, ExportDataRepresentation.class).formatData.get(N5ImageHandler.formatName);
        } catch (IOException e) {
            System.err.print("Can't open example exports: " + e.getLocalizedMessage());
        }
    }

//    https://www.npoint.io/docs/b85bb21076bf422a7d93
    public static ExportDataRepresentation.FormatExportDataRepresentation getExampleJSONData() throws FileNotFoundException {
        return exampleJSONData;
    }



}
