package org.vcell.vcellfiji;

import com.ibm.icu.impl.ClassLoaderUtil;
import junit.framework.TestCase;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

public class N5ImageHandlerTest extends TestCase {

    public void testReadBasicN5File() throws URISyntaxException {
        N5ImageHandler n5ImageHandler = new N5ImageHandler();
        URL url = ClassLoaderUtil.getClassLoader().getResource("N5/test_image.n5");
        File n5File = new File(url.toURI().getPath());
        n5ImageHandler.readN5Files(n5File);
    }


}