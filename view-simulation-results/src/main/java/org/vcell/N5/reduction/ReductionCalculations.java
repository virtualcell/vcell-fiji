package org.vcell.N5.reduction;

import com.google.gson.internal.LinkedTreeMap;
import ij.ImagePlus;
import ij.gui.Roi;
import org.vcell.N5.reduction.DTO.RangeOfImage;
import org.vcell.N5.reduction.DTO.ReducedData;
import org.vcell.N5.reduction.GUI.SelectMeasurements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;


class ReductionCalculations {
    private final boolean normalize;

    public ReductionCalculations(boolean normalize){
        this.normalize = normalize;
    }
    
    public void addAppropriateHeaders(ArrayList<Roi> roiList, RangeOfImage rangeOfImage,
                                      ReducedData reducedData,
                                      LinkedTreeMap<String, LinkedTreeMap<String, String>> channelInfo){
        for (int r = 0; r < roiList.size(); r++){
            for (int c = rangeOfImage.channelStart; c <= rangeOfImage.channelEnd; c++){ //Last channel is domain channel, not variable
                String stringC = String.valueOf(c - 1);
                String channelName = channelInfo != null && channelInfo.containsKey(stringC) ? channelInfo.get(stringC).get("Name") : String.valueOf(c);
                reducedData.channelNames.put(c - 1, channelName);
                reducedData.roiNames.put(r, roiList.get(r).getName());
            }
        }
    }

    /**
     * Set position in image with specific ROI, and then with the appropriate reduced data store, perform the calculation
     * and add it to reduced data.
     * @param imagePlus
     * @param roiList
     * @param normalizationValue
     * @param rangeOfImage
     */
    void calculateStatistics(ImagePlus imagePlus, ArrayList<Roi> roiList,
                             HashMap<String, Double> normalizationValue,
                             ReducedData reducedData,
                             RangeOfImage rangeOfImage, AtomicBoolean continueOperation){
        for (int roi = 0; roi < roiList.size(); roi++) {
            imagePlus.setRoi(roiList.get(roi));
            for (int t = rangeOfImage.timeStart; t <= rangeOfImage.timeEnd; t++){
                for (int z = rangeOfImage.zStart; z <= rangeOfImage.zEnd; z++){
                    for (int c = rangeOfImage.channelStart; c <= rangeOfImage.channelEnd; c++){
                        if (!continueOperation.get()){
                            return;
                        }
                        imagePlus.setPosition(c, z, t);
                        int nT = t - rangeOfImage.timeStart;
                        int nZ = z - rangeOfImage.zStart;
                        int nC = c - rangeOfImage.channelStart;
                        double calculatedValue;
                        for (SelectMeasurements.AvailableMeasurements measurement : reducedData.measurements){
                            switch (measurement){
                                case AVERAGE:
                                    calculatedValue = imagePlus.getStatistics().mean;
                                    break;
                                case STD_DEV:
                                    calculatedValue = imagePlus.getStatistics().stdDev;
                                    break;
                                default:
                                    throw new RuntimeException("Unknown measurement type selected.");
                            }
                            if (normalize){
                                calculatedValue = calculatedValue / normalizationValue.get(roiList.get(roi).getName() + c);
                            }
                            reducedData.putDataPoint(calculatedValue, nT, nZ, nC, roi, measurement);
                        }
                    }
                }
            }
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
