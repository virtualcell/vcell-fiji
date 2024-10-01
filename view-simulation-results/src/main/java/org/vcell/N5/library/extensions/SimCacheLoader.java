package org.vcell.N5.library.extensions;

import ij.ImagePlus;
import ij.gui.ImageWindow;
import net.imglib2.IterableInterval;
import net.imglib2.cache.Cache;
import net.imglib2.cache.LoaderCache;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.cache.ref.SoftRefLoaderCache;
import net.imglib2.img.basictypeaccess.AccessFlags;
import net.imglib2.img.basictypeaccess.ArrayDataAccessFactory;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.type.NativeType;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.imglib2.N5CacheLoader;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;

import java.awt.*;
import java.util.Set;
import java.util.function.Consumer;


public class SimCacheLoader<T extends NativeType<T>, A extends ArrayDataAccess<A>> extends N5CacheLoader<T, A> {

    private final CellGrid cellGrid;
    private final LoaderCache<Long, Cell<A>> loaderCache;
    private final T type;
    private final Set<AccessFlags> accessFlags;
    private ImagePlus imagesResponsibleFor;


    public SimCacheLoader(N5Reader n5, String dataset, CellGrid grid, T type, Set<AccessFlags> accessFlags, Consumer<IterableInterval<T>> blockNotFoundHandler, LoaderCache<Long, Cell<A>> loaderCache) throws N5Exception {
        super(n5, dataset, grid, type, accessFlags, blockNotFoundHandler);
        this.cellGrid = grid;
        this.loaderCache = loaderCache;
        this.type = type;
        this.accessFlags = accessFlags;
    }

    public CachedCellImg<T, A> createCachedCellImage(){
        final Cache<Long, Cell<A>> cache = this.loaderCache.withLoader(this);
        return new CachedCellImg<>(cellGrid, type, cache, ArrayDataAccessFactory.get(type, accessFlags));
    }

    public void setImagesResponsibleFor(ImagePlus imagePlus){
        imagesResponsibleFor = imagePlus;
    }

    public static <T extends NativeType<T>, A extends ArrayDataAccess<A>> SimCacheLoader<T, A> factoryDefault(N5Reader n5Reader, String dataset){
        final DatasetAttributes attributes = n5Reader.getDatasetAttributes(dataset);
        final long[] dimensions = attributes.getDimensions();
        final int[] blockSize = attributes.getBlockSize();
        final CellGrid grid = new CellGrid(dimensions, blockSize);
        final Set<AccessFlags> accessFlags = AccessFlags.setOf();
        final T type = N5Utils.type(n5Reader.getDatasetAttributes(dataset).getDataType());
        return new SimCacheLoader<>(n5Reader, dataset, grid, type, accessFlags, (Consumer<IterableInterval<T>>) img -> {}, new SoftRefLoaderCache<>());
    }


    @Override
    public Cell<A> get(Long key) throws Exception {
        ImageWindow window = imagesResponsibleFor == null ? null : imagesResponsibleFor.getWindow();
        
        if (window != null){ window.setCursor(new Cursor(Cursor.WAIT_CURSOR)); }

        Cell<A> cell = super.get(key);

        if (window != null){ window.setCursor(new Cursor(Cursor.DEFAULT_CURSOR)); }

        return cell;
    }


}
