package org.vcell.N5.retrieving;

import ij.ImagePlus;

import java.util.EventListener;

public interface SimLoadingListener extends EventListener {

    public void simIsLoading(int itemRow, String exportID);

    public void simFinishedLoading(SimResultsLoader loadedResult);

}
