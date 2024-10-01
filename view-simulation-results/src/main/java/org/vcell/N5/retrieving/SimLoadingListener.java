package org.vcell.N5.retrieving;

import java.util.EventListener;

public interface SimLoadingListener extends EventListener {

    public void simIsLoading(int itemRow);

    public void simFinishedLoading(int itemRow);

}
