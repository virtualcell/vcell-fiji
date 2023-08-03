package org.vcell.vcellfiji;


import ij.ImagePlus;
import net.imagej.ImageJ;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;
import ij.IJ;
import org.janelia.saalfeldlab.n5.*;
import org.janelia.saalfeldlab.n5.imglib2.*;
import net.imglib2.img.display.imagej.*;

import java.io.IOException;

// Command plugins
@Plugin(type = Command.class, menuPath = "Plugins>VCell>ImageHandler")
public class N5ImageHandler implements Command{


    @Override
    public void run() {
        ImagePlus image = IJ.openImage("http://imagej.net/images/clown.jpg");
        image.show();

        // auto closes reader
        try (N5FSReader n5FSReader = new N5FSReader("D:\\Downloads\\Images\\N5\\test_image.n5")) {
            CachedCellImg<UnsignedShortType, ?> n5imp = N5Utils.open(n5FSReader, "test\\c0\\s0");
//            System.out.print(n5FSReader.getDatasetAttributes("test"));
            ImageJFunctions.show(n5imp);
            System.out.print(n5FSReader.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        new ImageJ();
        new N5ImageHandler().run();

    }
}
