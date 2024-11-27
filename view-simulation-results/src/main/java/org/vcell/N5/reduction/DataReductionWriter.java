package org.vcell.N5.reduction;

import com.opencsv.CSVWriter;
import org.vcell.N5.ExportDataRepresentation;
import org.vcell.N5.UI.MainPanel;
import org.vcell.N5.UI.N5ExportTable;
import org.vcell.N5.reduction.DTO.RangeOfImage;
import org.vcell.N5.reduction.DTO.ReducedData;
import org.vcell.N5.reduction.GUI.DataReductionGUI;
import org.vcell.N5.reduction.GUI.SelectMeasurements;
import org.vcell.N5.retrieving.SimResultsLoader;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class DataReductionWriter{
    private final Object metaDataLock = new Object();
    private final File file;

    public final DataReductionGUI.DataReductionSubmission submission;

    private final ArrayList<ArrayList<String>> averageMatrix = new ArrayList<>();
    private final ArrayList<ArrayList<String>> standardDivMatrix = new ArrayList<>();
    private final ArrayList<ArrayList<String>> maxIntensityMatrix = new ArrayList<>();
    private final ArrayList<ArrayList<String>> minIntensityMatrix = new ArrayList<>();
    private final ArrayList<ArrayList<String>> metaDataSheet = new ArrayList<>();


    private final HashMap<SelectMeasurements.AvailableMeasurements, ArrayList<ArrayList<String>>> sheetsAvailable = new HashMap<SelectMeasurements.AvailableMeasurements, ArrayList<ArrayList<String>>>(){{
        put(SelectMeasurements.AvailableMeasurements.AVERAGE, averageMatrix);
        put(SelectMeasurements.AvailableMeasurements.STD_DEV, standardDivMatrix);
        put(SelectMeasurements.AvailableMeasurements.MAX_INTENSITY, maxIntensityMatrix);
        put(SelectMeasurements.AvailableMeasurements.MIN_INTENSITY, minIntensityMatrix);
    }};
    private final HashMap<SelectMeasurements.AvailableMeasurements, Integer> columnsForSheets = new HashMap<>();

    private int metaDataParameterCol = 5;
    private final HashMap<String, Integer> parameterNameToCol = new HashMap<>();

    private final ArrayList<SelectMeasurements.AvailableMeasurements> selectedMeasurements;

    private final int maxZ;
    private final int maxT;

    private final boolean wideTable;

    ///////////////////////////////////////
    // Initialize Sheet and Lab results //
    /////////////////////////////////////

    public DataReductionWriter(DataReductionGUI.DataReductionSubmission submission, int maxT, int maxZ){
        this.submission = submission;
        this.selectedMeasurements = submission.selectedMeasurements;
        this.file = submission.fileToSaveResultsTo;
        this.maxZ = maxZ;
        this.maxT = maxT;
        this.wideTable = submission.wideTable;
    }

    public void consumeNewData(ReducedData reducedData) throws IOException {
        if (wideTable){
            addValuesToWideCSVMatrix(reducedData);
        } else {
            appendAndWriteTallTable(reducedData);
        }
    }

    public void close() throws IOException {
        if (wideTable){
            writeWideTableCSVMatrix();
        }
        writeMetaDataTable();
    }

    public void initializeDataSheets(){
        ArrayList<String> headers = new ArrayList<String>(){{add("Time Frame");}};
        boolean is3D = maxZ > 1;

        // Add Time and Z-Index Columns


        metaDataSheet.add(new ArrayList<>());
        metaDataSheet.get(0).add("");
        metaDataSheet.get(0).add("BioModel Name");
        metaDataSheet.get(0).add("Application Name");
        metaDataSheet.get(0).add("Simulation Name");
        metaDataSheet.get(0).add("N5 URL");

        if (wideTable){
            // Fill in Time and Z-Index Columns with selected range
            if (is3D){
                headers.add("Z Index");
            }
            for (SelectMeasurements.AvailableMeasurements measurement : selectedMeasurements){
                ArrayList<ArrayList<String>> dataSheet = sheetsAvailable.get(measurement);
                dataSheet.add(new ArrayList<>());
                ArrayList<String> headerRow = dataSheet.get(0);

                for (int i = 0; i < headers.size(); i++){
                    headerRow.add(i, headers.get(i));
                }
                columnsForSheets.put(measurement, headers.size() + 1);
            }

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
        } else {
            headers.add(1, "Z Index");
            headers.add("Image Name");
            headers.add("ROI Name");
            headers.add("Channel Name");
            for (SelectMeasurements.AvailableMeasurements measurement : selectedMeasurements){
                headers.add(measurement.publicName);
            }
            File file = new File(this.file.getAbsolutePath() + ".csv");
            try(FileWriter fileWriter = new FileWriter(file)) {
                CSVWriter csvWriter = new CSVWriter(fileWriter);
                csvWriter.writeNext(headers.toArray(new String[0]));
            } catch (IOException ioException){
                throw new RuntimeException("Can't write to CSV file.", ioException);
            }
        }
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

    ////////////////////////
    // Private Functions //
    //////////////////////

    private void addValuesToWideCSVMatrix(ReducedData reducedData){
        for (SelectMeasurements.AvailableMeasurements measurement:  reducedData.measurements){
            ArrayList<ArrayList<String>> dataSheet = sheetsAvailable.get(measurement);
            int colIndex = columnsForSheets.get(measurement);
            fillWithEmptySpace(dataSheet.get(0), colIndex);
            RangeOfImage rangeOfImage = reducedData.rangeOfImage;
            for (int c = 0; c < rangeOfImage.getNChannels(); c++){
                for (int r = 0; r < reducedData.nROIs; r++){
                    dataSheet.get(0).add(colIndex, reducedData.getWideTableHeader(r, c));
                    int tzCounter = 1;
                    for (int t = 1; t <= maxT; t++){
                        for (int z = 1; z <= maxZ; z++){
                            boolean inBetweenTime = t <= rangeOfImage.timeEnd && rangeOfImage.timeStart <= t;
                            boolean onlyOneZ = rangeOfImage.zEnd - rangeOfImage.zStart == 0 && maxZ == 1;
                            boolean inBetweenZ = z <= rangeOfImage.zEnd && rangeOfImage.zStart <= z || onlyOneZ;
                            ArrayList<String> row = dataSheet.get(tzCounter);
                            fillWithEmptySpace(row, colIndex);
                            if (inBetweenTime && inBetweenZ){
                                int nt = t - rangeOfImage.timeStart;
                                int nz = onlyOneZ ? 0 : z - rangeOfImage.zStart;
                                row.add(String.valueOf(reducedData.getDataPoint(nt, nz, c, r, measurement)));
                            }
                            tzCounter += 1;
                        }
                    }
                    colIndex += 1;
                }
            }
            colIndex += 1;
            columnsForSheets.replace(measurement, colIndex);
        }
    }

    // If specific entry to be added isn't in array list length, add empty space until it is
    private void fillWithEmptySpace(ArrayList<String> arrayList, int col){
        while (arrayList.size() < col){
            arrayList.add("");
        }
    }

    // Each reduced data is a different measurement type
    private void appendAndWriteTallTable(ReducedData reducedData) throws IOException {
        RangeOfImage rangeOfImage = reducedData.rangeOfImage;
        for (int t = rangeOfImage.timeStart; t <= rangeOfImage.timeEnd; t++) {
            for (int z = rangeOfImage.zStart; z <= rangeOfImage.zEnd; z++) {
                for (int channel = 0; channel < rangeOfImage.getNChannels(); channel++){
                    for(int roi= 0; roi < reducedData.nROIs; roi++){
                        File file = new File(this.file.getAbsolutePath() + ".csv");
                        ArrayList<String> newRow = new ArrayList<>(Arrays.asList(String.valueOf(t), String.valueOf(z),
                                reducedData.imageName, reducedData.roiNames.get(roi), reducedData.channelNames.get(channel)));
                        for (SelectMeasurements.AvailableMeasurements measurement : reducedData.measurements){
                            int nt = t - rangeOfImage.timeStart;
                            int nz = z - rangeOfImage.zStart;
                            newRow.add(String.valueOf(reducedData.getDataPoint(nt, nz, channel, roi, measurement)));
                        }
                        try (FileWriter fileWriter = new FileWriter(file, true)) {
                            CSVWriter csvWriter = new CSVWriter(fileWriter);
                            csvWriter.writeNext(newRow.toArray(new String[0]));
                        }
                    }
                }
            }
        }
    }

    private void writeWideTableCSVMatrix() throws IOException {
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
    }

    private void writeMetaDataTable() throws IOException {
        File currentFile = new File(file.getAbsolutePath() + "-Metadata.csv");
        try (FileWriter fileWriter = new FileWriter(currentFile)){
            CSVWriter csvWriter = new CSVWriter(fileWriter);
            for (ArrayList<String> row : metaDataSheet){
                csvWriter.writeNext(row.toArray(new String[0]));
            }
        }
    }
}





