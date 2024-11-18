package org.vcell.N5.reduction;

import com.opencsv.CSVWriter;
import org.vcell.N5.ExportDataRepresentation;
import org.vcell.N5.UI.MainPanel;
import org.vcell.N5.UI.N5ExportTable;
import org.vcell.N5.reduction.DTO.RangeOfImage;
import org.vcell.N5.reduction.DataReductionManager.ReducedData;
import org.vcell.N5.retrieving.SimResultsLoader;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class DataReductionWriter{
    private final Object metaDataLock = new Object();
    private final File file;

    public final DataReductionGUI.DataReductionSubmission submission;

    private final ArrayList<ArrayList<String>> averageMatrix = new ArrayList<>();
    private final ArrayList<ArrayList<String>> standardDivMatrix = new ArrayList<>();
    private final ArrayList<ArrayList<String>> metaDataSheet = new ArrayList<>();


    private final HashMap<SelectMeasurements.AvailableMeasurements, ArrayList<ArrayList<String>>> sheetsAvailable = new HashMap<SelectMeasurements.AvailableMeasurements, ArrayList<ArrayList<String>>>(){{
        put(SelectMeasurements.AvailableMeasurements.AVERAGE, averageMatrix);
        put(SelectMeasurements.AvailableMeasurements.STD_DEV, standardDivMatrix);
    }};
    private final HashMap<SelectMeasurements.AvailableMeasurements, Integer> columnsForSheets = new HashMap<>();

    private int metaDataParameterCol = 5;
    private final HashMap<String, Integer> parameterNameToCol = new HashMap<>();

    private final ArrayList<SelectMeasurements.AvailableMeasurements> selectedMeasurements;

    private final int maxZ;
    private final int maxT;

    ///////////////////////////////////////
    // Initialize Sheet and Lab results //
    /////////////////////////////////////

    public DataReductionWriter(DataReductionGUI.DataReductionSubmission submission, int maxT, int maxZ){
        this.submission = submission;
        this.selectedMeasurements = submission.selectedMeasurements;
        this.file = submission.fileToSaveResultsTo;
        this.maxZ = maxZ;
        this.maxT = maxT;
    }

    public void initializeDataSheets(){
        ArrayList<String> headers = new ArrayList<String>(){{add("Time Frame");}};
        boolean is3D = maxZ > 1;
        if (is3D){
            headers.add("Z Index");
        }

        // Add Time and Z-Index Columns
        for (SelectMeasurements.AvailableMeasurements measurement : selectedMeasurements){
            ArrayList<ArrayList<String>> dataSheet = sheetsAvailable.get(measurement);
            dataSheet.add(new ArrayList<>());
            ArrayList<String> headerRow = dataSheet.get(0);

            for (int i = 0; i < headers.size(); i++){
                headerRow.add(i, headers.get(i));
            }
            columnsForSheets.put(measurement, headers.size() + 1);
        }

        metaDataSheet.add(new ArrayList<>());
        metaDataSheet.get(0).add("");
        metaDataSheet.get(0).add("BioModel Name");
        metaDataSheet.get(0).add("Application Name");
        metaDataSheet.get(0).add("Simulation Name");
        metaDataSheet.get(0).add("N5 URL");

        // Fill in Time and Z-Index Columns with selected range

        for (SelectMeasurements.AvailableMeasurements measurement : selectedMeasurements){
            ArrayList<ArrayList<String>> dataSheet = sheetsAvailable.get(measurement);
            for (int t = 1; t <= maxT; t++){
                for (int z = 1; z <= maxZ; z++){
                    ArrayList<String> pointRow = new ArrayList<>();
                    pointRow.add(0, String.valueOf(t));
                    if (is3D){
                        pointRow.add(1, String.valueOf(z));
                    }
                    dataSheet.add(pointRow);
                }
            }
        }
    }

    ////////////////////////
    // General Functions //
    //////////////////////

    public void addValuesToWideCSVMatrix(ReducedData reducedData){
        ArrayList<ArrayList<String>> dataSheet = sheetsAvailable.get(reducedData.measurementType);
        int colIndex = columnsForSheets.get(reducedData.measurementType);
        fillWithEmptySpace(dataSheet.get(0), colIndex);
        for (int c = 0; c < reducedData.columnHeaders.size(); c++){
            dataSheet.get(0).add(colIndex, reducedData.columnHeaders.get(c));
        }
        RangeOfImage rangeOfImage = reducedData.rangeOfImage;
        int tzCounter = 1;
        for (int t = 1; t <= maxT; t++){
            for (int z = 1; z <= maxZ; z++){
                boolean inBetweenTime = t <= rangeOfImage.timeEnd && rangeOfImage.timeStart <= t;
                boolean inBetweenZ = z <= rangeOfImage.zEnd && rangeOfImage.zStart <= z;
                ArrayList<String> row = dataSheet.get(tzCounter);
                fillWithEmptySpace(row, colIndex);
                for (int c = 0; c < reducedData.colLen; c++){
                    if (inBetweenTime && inBetweenZ){
                        int dataRow = ((t - rangeOfImage.timeStart) * (z - rangeOfImage.zStart)) + (z - rangeOfImage.zStart);
                        double mean = reducedData.data[dataRow][c];
                        row.add(String.valueOf(mean));
                    }
                }
                tzCounter += 1;
            }
        }
        colIndex += 1 + reducedData.data[0].length;
        columnsForSheets.replace(reducedData.measurementType, colIndex);
    }

    // If parameter is not in list of parameters, add new column. If simulation does not have parameter say "not-present"
    public void addMetaData(SimResultsLoader loadedResults){
        synchronized (metaDataLock){
            N5ExportTable n5ExportTable = MainPanel.n5ExportTable;
            ExportDataRepresentation.SimulationExportDataRepresentation data = n5ExportTable.n5ExportTableModel.getRowData(loadedResults.rowNumber);
            ArrayList<String> newMetaData = new ArrayList<>();
            newMetaData.add(loadedResults.userSetFileName);
            newMetaData.add(data.biomodelName);
            newMetaData.add(data.applicationName);
            newMetaData.add(data.simulationName);
            newMetaData.add(data.uri);
            ArrayList<String> parameterValues = data.differentParameterValues;
            for (String s : parameterValues){
                String[] tokens = s.split(":");
                String colValue = tokens[1] + ":" + tokens[2];
                if (parameterNameToCol.containsKey(tokens[0])){
                    int col = parameterNameToCol.get(tokens[0]);
                    fillWithEmptySpace(newMetaData, col);
                    newMetaData.add(col, colValue);
                } else{
                    metaDataSheet.get(0).add(tokens[0] + " (Default:Set Value)");
                    fillWithEmptySpace(newMetaData, metaDataParameterCol);
                    newMetaData.add(metaDataParameterCol, colValue);
                    parameterNameToCol.put(tokens[0], metaDataParameterCol);
                    metaDataParameterCol += 1;
                }
            }
            metaDataSheet.add(newMetaData);
        }
    }

    // If specific entry to be added isn't in array list length, add empty space until it is
    private void fillWithEmptySpace(ArrayList<String> arrayList, int col){
        while (arrayList.size() < col){
            arrayList.add("");
        }
    }


    public void writeCSVMatrix() throws IOException {
        for (SelectMeasurements.AvailableMeasurements measurements : sheetsAvailable.keySet()){
            if (!sheetsAvailable.get(measurements).isEmpty()){
                File currentFile = new File(file.getAbsolutePath() + "-" + measurements.publicName + ".csv");
                try (FileWriter fileWriter = new FileWriter(currentFile)){
                    CSVWriter csvWriter = new CSVWriter(fileWriter);
                    for (ArrayList<String> row : sheetsAvailable.get(measurements)){
                        csvWriter.writeNext(row.toArray(new String[0]));
                    }
                }
            }
        }
        File currentFile = new File(file.getAbsolutePath() + "-Metadata.csv");
        try (FileWriter fileWriter = new FileWriter(currentFile)){
            CSVWriter csvWriter = new CSVWriter(fileWriter);
            for (ArrayList<String> row : metaDataSheet){
                csvWriter.writeNext(row.toArray(new String[0]));
            }
        }
    }
}





