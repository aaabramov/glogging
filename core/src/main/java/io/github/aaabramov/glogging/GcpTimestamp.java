package io.github.aaabramov.glogging;

/**
 * Represents timestamp as a combination of seconds & nanos.
 *
 * @author Andrii Abramov
 * @since 0.0.1
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
     * <p>
     * Uses floor division so that {@link #nanos} is always in {@code [0, 999999999]}, as
     * the protobuf {@code Timestamp} these two fields model requires. Plain {@code /} and
     * {@code %} truncate towards zero, which for a pre-1970 instant yields a negative
     * {@code nanos} — an invalid encoding, even though it still adds up to the right
     * instant.
     *
     * @param millis Epoch time
     */
    static GcpTimestamp ofEpoch(long millis) {
        long seconds = Math.floorDiv(millis, 1000);
        long nanos = Math.floorMod(millis, 1000) * 1000000;
        return new GcpTimestamp(seconds, nanos);
    }
    
}
