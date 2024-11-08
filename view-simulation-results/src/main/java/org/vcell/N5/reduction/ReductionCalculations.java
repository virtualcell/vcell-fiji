package org.vcell.N5.reduction;

import com.google.gson.internal.LinkedTreeMap;
import ij.ImagePlus;
import ij.gui.Roi;
import org.vcell.N5.reduction.DTO.RangeOfImage;

import java.util.ArrayList;
import java.util.HashMap;

class ReductionCalculations {
    private final boolean normalize;

    public ReductionCalculations(boolean normalize){
        this.normalize = normalize;
    }
    
    public void addAppropriateHeaders(ImagePlus imagePlus, ArrayList<Roi> roiList, RangeOfImage rangeOfImage,
                                      DataReductionWriter.ReducedData reducedData,
                                      LinkedTreeMap<String, LinkedTreeMap<String, String>> channelInfo){
        for (Roi roi: roiList){
            for (int c = rangeOfImage.channelStart; c <= rangeOfImage.channelEnd; c++){ //Last channel is domain channel, not variable
                String stringC = String.valueOf(c - 1);
                String channelName = channelInfo != null && channelInfo.containsKey(stringC) ? channelInfo.get(stringC).get("Name") : String.valueOf(c);
                reducedData.columnHeaders.add(imagePlus.getTitle() + ":" + roi.getName() + ":" + channelName);
            }
        }
    }

    /**
     * Set position in image with specific ROI, and then with the appropriate reduced data store, perform the calculation
     * and add it to reduced data.
     * @param imagePlus
     * @param roiList
     * @param normalizationValue
     * @param reducedDataArrayList
     * @param rangeOfImage
     */
    void calculateStatistics(ImagePlus imagePlus, ArrayList<Roi> roiList,
                            HashMap<String, Double> normalizationValue, 
                            ArrayList<DataReductionWriter.ReducedData> reducedDataArrayList,
                                    RangeOfImage rangeOfImage){
        int roiCounter = 0;
        for (Roi roi: roiList) {
            imagePlus.setRoi(roi);
            int tzCounter = 0;
            for (int t = rangeOfImage.timeStart; t <= rangeOfImage.timeEnd; t++){
                for (int z = rangeOfImage.zStart; z <= rangeOfImage.zEnd; z++){
                    for (int c = rangeOfImage.channelStart; c <= rangeOfImage.channelEnd; c++){
                        int channelSize = rangeOfImage.channelEnd - rangeOfImage.channelStart + 1;
                        imagePlus.setPosition(c, z, t);
                        double calculatedValue;
                        for (DataReductionWriter.ReducedData reducedData : reducedDataArrayList){
                            switch (reducedData.measurementType){
                                case AVERAGE:
                                    calculatedValue = imagePlus.getStatistics().mean;
                                    break;
                                case STD_DEV:
                                    calculatedValue = imagePlus.getStatistics().stdDev;
                                    break;
                                default:
                                    throw new RuntimeException("Unknown measurement type selected.");
                            }
                            if (normalize && reducedData.measurementType == SelectMeasurements.AvailableMeasurements.AVERAGE){
                                calculatedValue = calculatedValue / normalizationValue.get(roi.getName() + c);
                            }
                            reducedData.data[tzCounter][c - 1 + (roiCounter * channelSize)] = calculatedValue;
                        }
                    }
                    tzCounter += 1;
                }
            }
            roiCounter += 1;
        }
    }

    HashMap<String, Double> calculateNormalValue(ImagePlus imagePlus, RangeOfImage normRange,
                                                 ArrayList<Roi> roiList, RangeOfImage rangeOfImage){
        int startT = normRange.timeStart;
        int endT = normRange.timeEnd;
        HashMap<String, Double> result = new HashMap<>();
        for (Roi roi : roiList){
            imagePlus.setRoi(roi);
            for (int c = rangeOfImage.channelStart; c <= rangeOfImage.channelEnd; c++){
                double normal = 0;
                for (int t = startT; t <= endT; t++){
                    double zAverage = 0;
                    for (int z = rangeOfImage.zStart; z <= rangeOfImage.zEnd; z++){
                        imagePlus.setPosition(c, z, t);
                        zAverage += imagePlus.getProcessor().getStatistics().mean;
                    }
                    zAverage = zAverage / (rangeOfImage.zEnd - rangeOfImage.zStart + 1);
                    normal += zAverage;
                }
                normal = normal / (endT - startT + 1); // inclusive of final point
                result.put(roi.getName() + c, normal);
            }
        }
        return result;
    }

}
