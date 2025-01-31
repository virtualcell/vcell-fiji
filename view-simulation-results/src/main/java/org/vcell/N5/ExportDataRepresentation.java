package org.vcell.N5;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

public class ExportDataRepresentation {
    public Stack<String> globalJobIDs;
    public HashMap<String, FormatExportDataRepresentation> formatData;

    public ExportDataRepresentation(Stack<String> globalJobIDs, HashMap<String, FormatExportDataRepresentation> formatData){
        this.globalJobIDs = globalJobIDs;
        this.formatData = formatData;
    }

    public static class FormatExportDataRepresentation {
        public HashMap<String, SimulationExportDataRepresentation> simulationDataMap;
        public Stack<String> formatJobIDs;

        public FormatExportDataRepresentation(HashMap<String, SimulationExportDataRepresentation> simulationDataMap, Stack<String> formatJobIDs){
            this.formatJobIDs = formatJobIDs;
            this.simulationDataMap = simulationDataMap;
        }
    }

    public static class SimulationExportDataRepresentation {
        public final String exportDate;
        public final String uri;
        public final String jobID;
        public final String dataID;
        public final String simulationName;
        public final String applicationName;
        public final String biomodelName;
        public final String variables;
        public final String startAndEndTime;

        public final ArrayList<String> differentParameterValues;
        public final String savedFileName;

        public final ArrayList<URL> imageROIReferences;

        public int zSlices;
        public int tSlices;
        public int numVariables;

        public SimulationExportDataRepresentation(String exportDate, String uri, String jobID, String dataID, String simulationName,
                                                  String applicationName, String biomodelName, String variables, String startAndEndTime,
                                                  ArrayList<String> differentParameterValues, String savedFileName, ArrayList<URL> imageROIReferences){
            this.exportDate = exportDate;
            this.uri = uri;
            this.jobID = jobID;
            this.dataID = dataID;
            this.simulationName = simulationName;
            this.applicationName = applicationName;
            this.biomodelName = biomodelName;
            this.variables = variables;
            this.startAndEndTime = startAndEndTime;
            this.differentParameterValues = differentParameterValues;
            this.savedFileName = savedFileName;
            this.imageROIReferences = imageROIReferences;
        }
    }

}
