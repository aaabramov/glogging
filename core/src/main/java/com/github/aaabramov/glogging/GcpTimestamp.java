package com.github.aaabramov.glogging;

/**
 * Represents timestamp as a combination of seconds & nanos.
 */
class GcpTimestamp {
    
    public final long seconds;
    public final long nanos;
    
    GcpTimestamp(long seconds, long nanos) {
        this.seconds = seconds;
        this.nanos = nanos;
    }
    
    /**
     * Creates new {@link GcpTimestamp} from epoch millis.
     *
     * @param millis Epoch time
     */
    static GcpTimestamp ofEpoch(long millis) {
        long seconds = millis / 1000;
        long nanos = millis % 1000 * 1000000;
        return new GcpTimestamp(seconds, nanos);
    }
    
}
