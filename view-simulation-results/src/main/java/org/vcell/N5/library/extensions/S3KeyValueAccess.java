package org.vcell.N5.library.extensions;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.s3.AmazonS3KeyValueAccess;

public class S3KeyValueAccess extends AmazonS3KeyValueAccess {
    private final String bucketName;
    private final AmazonS3 s3;

    public S3KeyValueAccess(AmazonS3 s3, String containerURI, boolean createBucket) throws N5Exception.N5IOException{
        super(s3, containerURI, createBucket);
        this.s3 = s3;
        this.bucketName = containerURI;
    }


    /*
        Always return true when suffix is .N5 because our N5 files will always be a directory.
        And the ListObjectsV2 request that is called to find out if it's a directory is not bugged, for it does return only 1-2 keys, but
        while trying to do so requires the server to recursively discover all of the objects children.

        !Added Bonus!
        No need for complicated regex that determines query parameters used and their values. This is because the cases
        when queries don't contain a .n5 suffix is not encountered within the plugin.
     */
    @Override
    public boolean isDirectory(String normalPath) {
        if (normalPath.endsWith(".n5")){
            return true;
        }
        final ListObjectsV2Request listObjectsRequest = new ListObjectsV2Request()
                .withBucketName(bucketName)
                .withPrefix(normalPath)
                .withMaxKeys(2);
        return s3.listObjectsV2(listObjectsRequest).getKeyCount() > 1;
    }
}
