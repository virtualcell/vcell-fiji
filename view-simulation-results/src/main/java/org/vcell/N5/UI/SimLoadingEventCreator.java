package org.vcell.N5.UI;

import org.vcell.N5.SimResultsLoader;

import javax.swing.event.EventListenerList;

public interface SimLoadingEventCreator {
    public void addSimLoadingListener(SimLoadingListener simLoadingListener);

    public void notifySimIsLoading(SimResultsLoader simResultsLoader);

    public void notifySimIsDoneLoading(SimResultsLoader simResultsLoader);

}
