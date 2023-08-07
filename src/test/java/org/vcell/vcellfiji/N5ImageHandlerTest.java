package org.vcell.vcellfiji;

import com.ibm.icu.impl.ClassLoaderUtil;
import junit.framework.TestCase;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;

public class N5ImageHandlerTest extends TestCase {

    public void testN5DatasetList() throws URISyntaxException {
        N5ImageHandler n5ImageHandler = new N5ImageHandler();
        URL url = ClassLoaderUtil.getClassLoader().getResource("N5/test_image.n5");
        File n5File = new File(url.toURI().getPath());
        n5ImageHandler.setSelectedFile(n5File);
        ArrayList<String> datasetList = n5ImageHandler.getN5DatasetList();

        assertEquals("test/c0/s0", datasetList.get(0));
        assertEquals(1, datasetList.size());
    }


}