package org.vcell.N5.retrieving;

import ij.ImagePlus;

public interface SimLoadingEventCreator {
    public void addSimLoadingListener(SimLoadingListener simLoadingListener);

    public void notifySimIsLoading(SimResultsLoader simResultsLoader);

    public void notifySimIsDoneLoading(SimResultsLoader simResultsLoader, ImagePlus imagePlus);

}
