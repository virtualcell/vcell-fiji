package org.vcell.N5.reduction.DTO;

import ij.ImagePlus;
import ij.gui.Roi;
import org.vcell.N5.reduction.GUI.conclusion.SelectMeasurements;

import java.io.File;
import java.util.ArrayList;

public class DataReductionSubmission {
    public final boolean normalizeMeasurementsBool;
    public final ArrayList<Roi> arrayOfSimRois;
    public final ArrayList<Roi> arrayOfLabRois;
    public final ImagePlus labResults;
    public final int numOfSimImages;
    public final File fileToSaveResultsTo;
    public final RangeOfImage experiementNormRange;
    public final RangeOfImage simNormRange;
    public final RangeOfImage experimentImageRange;
    public final RangeOfImage simImageRange;
    public final ArrayList<SelectMeasurements.AvailableMeasurements> selectedMeasurements;
    public final boolean wideTable;

    public DataReductionSubmission(boolean normalizeMeasurementsBool, ArrayList<Roi> arrayOfSimRois, ArrayList<Roi> arrayOfLabRois,
                                   ImagePlus labResults, int simStartPointNorm, int simEndPointNorm, int imageStartPointNorm,
                                   int imageEndPointNorm, int numOfSimImages, File fileToSaveResultsTo,
                                   RangeOfImage simRange, ArrayList<SelectMeasurements.AvailableMeasurements> selectedMeasurements, boolean wideTable) {
        this.normalizeMeasurementsBool = normalizeMeasurementsBool;
        this.arrayOfSimRois = arrayOfSimRois;
        this.arrayOfLabRois = arrayOfLabRois;
        this.labResults = labResults;
        this.numOfSimImages = numOfSimImages;
        this.fileToSaveResultsTo = fileToSaveResultsTo;
        this.experiementNormRange = new RangeOfImage(imageStartPointNorm, imageEndPointNorm);
        this.simNormRange = new RangeOfImage(simStartPointNorm, simEndPointNorm);
        this.experimentImageRange = new RangeOfImage(1, labResults.getNFrames(),
                1, labResults.getNSlices(), 1, labResults.getNChannels());
        this.simImageRange = simRange;
        this.selectedMeasurements = selectedMeasurements;
        this.wideTable = wideTable;
    }
}
