package org.vcell.N5.reduction.DTO;

import org.vcell.N5.reduction.GUI.conclusion.SelectMeasurements;

import java.util.ArrayList;
import java.util.HashMap;

// Per Image, contain all ROI data
// Shape: [time * z][channel * roi]
public class ReducedData {
    private final int nChannels;
    private final int nSlices;
    private final HashMap<SelectMeasurements.AvailableMeasurements, double[][]> dataMap = new HashMap<>();

    public final ArrayList<SelectMeasurements.AvailableMeasurements> measurements;
    public final RangeOfImage rangeOfImage;
    public final int nROIs;
    public final HashMap<Integer, String> roiNames = new HashMap<>();
    public final HashMap<Integer, String> channelNames = new HashMap<>();
    public final String imageName;
    public ReducedData(String imageName, RangeOfImage rangeOfImage, int nROIs, ArrayList<SelectMeasurements.AvailableMeasurements> measurements){
        int nFrames = rangeOfImage.timeEnd - rangeOfImage.timeStart + 1;
        nSlices = rangeOfImage.zEnd - rangeOfImage.zStart + 1;
        this.measurements = measurements;
        this.nChannels = rangeOfImage.getNChannels();
        this.nROIs = nROIs;
        this.rangeOfImage = rangeOfImage;
        this.imageName = imageName;

        for (SelectMeasurements.AvailableMeasurements measurement: measurements){
            dataMap.put(measurement, new double[nFrames * nSlices][nChannels * nROIs]);
        }
    }

    public void putDataPoint(double data,int time, int z, int channel, int roi, SelectMeasurements.AvailableMeasurements measurementType){
        dataMap.get(measurementType)[(time * nSlices) + z][(roi * nChannels) + channel] = data;
    }

    public double getDataPoint(int time, int z, int channel, int roi, SelectMeasurements.AvailableMeasurements measurementType){
        return dataMap.get(measurementType)[(time * nSlices) + z][(roi * nChannels) + channel];
    }

    public String getWideTableHeader(int r, int c){
        return imageName + ":" + roiNames.get(r) + ":" + channelNames.get(c);
    }

}
