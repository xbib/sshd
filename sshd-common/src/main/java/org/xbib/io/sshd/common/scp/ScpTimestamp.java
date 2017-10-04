package org.xbib.io.sshd.common.scp;

import org.xbib.io.sshd.common.util.GenericUtils;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Represents an SCP timestamp definition.
 */
public class ScpTimestamp {
    private final long lastModifiedTime;
    private final long lastAccessTime;

    public ScpTimestamp(long modTime, long accTime) {
        lastModifiedTime = modTime;
        lastAccessTime = accTime;
    }

    /**
     * @param line The time specification - format:
     *             {@code T<mtime-sec> <mtime-micros> <atime-sec> <atime-micros>}
     *             where specified times are in seconds since UTC
     * @return The {@link ScpTimestamp} value with the timestamps converted to
     * <U>milliseconds</U>
     * @throws NumberFormatException if bad numerical values - <B>Note:</B>
     *                               does not check if 1st character is 'T'.
     * @see <A HREF="https://blogs.oracle.com/janp/entry/how_the_scp_protocol_works">How the SCP protocol works</A>
     */
    public static ScpTimestamp parseTime(String line) throws NumberFormatException {
        String[] numbers = GenericUtils.split(line.substring(1), ' ');
        return new ScpTimestamp(TimeUnit.SECONDS.toMillis(Long.parseLong(numbers[0])),
                TimeUnit.SECONDS.toMillis(Long.parseLong(numbers[2])));
    }

    public long getLastModifiedTime() {
        return lastModifiedTime;
    }

    public long getLastAccessTime() {
        return lastAccessTime;
    }

    @Override
    public String toString() {
        return "modified=" + new Date(lastModifiedTime)
                + ";accessed=" + new Date(lastAccessTime);
    }
}
