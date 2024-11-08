package org.vcell.N5.reduction.DTO;

public class RangeOfImage {
    public final int timeStart;
    public final int timeEnd;
    public final int zStart;
    public final int zEnd;
    public final int channelStart;
    public final int channelEnd;

    public RangeOfImage(int timeStart, int timeEnd, int zStart, int zEnd, int channelStart, int channelEnd) {
        this.timeStart = timeStart;
        this.timeEnd = timeEnd;
        this.zStart = zStart;
        this.zEnd = zEnd;
        this.channelStart = channelStart;
        this.channelEnd = channelEnd;
    }

    /**
     * Only regards time for range, and is used for normalization.
     * @param timeStart
     * @param timeEnd
     */
    public RangeOfImage(int timeStart, int timeEnd){
        this.timeStart = timeStart;
        this.timeEnd = timeEnd;
        this.zStart = Integer.MIN_VALUE;
        this.zEnd = Integer.MIN_VALUE;
        this.channelStart = Integer.MIN_VALUE;
        this.channelEnd = Integer.MIN_VALUE;
    }
}
