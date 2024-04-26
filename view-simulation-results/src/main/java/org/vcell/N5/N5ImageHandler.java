package org.vcell.N5;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.log.Logger;
import org.scijava.log.slf4j.SLF4JLogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.vcell.N5.UI.N5ExportTable;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Stack;


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
    public static N5ExportTable exportTable;
    public static String exportedMetaDataPath = System.getProperty("user.home") + "/.vcell/exportMetaData.json";

    @Override
    public void run() {
        exportTable = new N5ExportTable();
        initializeLogService();
//        N5ImageHandler.logService.setLevel(LogService.DEBUG);
        exportTable.displayExportTable();
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
        n5ImageHandler.run();
    }

    public static ExportDataRepresentation.SimulationExportDataRepresentation getLastJSONElement() throws FileNotFoundException {
        ExportDataRepresentation jsonData = getJsonData();
        if (jsonData != null && jsonData.formatData.containsKey(N5ImageHandler.formatName)) {
            ExportDataRepresentation.FormatExportDataRepresentation formatExportDataRepresentation = jsonData.formatData.get(N5ImageHandler.formatName);
            Stack<String> formatJobIDs = formatExportDataRepresentation.formatJobIDs;
            String jobID = formatJobIDs.isEmpty() ? null : formatJobIDs.peek();

            return jobID == null ? null : formatExportDataRepresentation.simulationDataMap.get(jobID);
        }
        return null;
    }

    public static boolean exportedDataExists(){
        File jsonFile = new File(exportedMetaDataPath);
        return jsonFile.exists();
    }

    public static ExportDataRepresentation getJsonData() throws FileNotFoundException {
        File jsonFile = new File(exportedMetaDataPath);
        if (jsonFile.exists() && jsonFile.length() != 0){
            ExportDataRepresentation jsonHashMap;
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            jsonHashMap = gson.fromJson(new FileReader(jsonFile.getAbsolutePath()), ExportDataRepresentation.class);
            return jsonHashMap;
        }
        return null;

    }

//    https://www.npoint.io/docs/b85bb21076bf422a7d93
    public static ExportDataRepresentation getExampleJSONData() throws FileNotFoundException {
        try(BufferedInputStream remoteJSONFile = new BufferedInputStream(new URL("https://api.npoint.io/b85bb21076bf422a7d93").openStream())){
            InputStreamReader remoteJSONFileReader = new InputStreamReader(remoteJSONFile);
            ExportDataRepresentation jsonHashMap;
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            jsonHashMap = gson.fromJson(remoteJSONFileReader, ExportDataRepresentation.class);
            return jsonHashMap;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }



}
