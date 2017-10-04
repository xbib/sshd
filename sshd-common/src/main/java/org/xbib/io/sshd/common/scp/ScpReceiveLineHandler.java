package org.xbib.io.sshd.common.scp;

import java.io.IOException;

/**
 *
 */
@FunctionalInterface
public interface ScpReceiveLineHandler {
    /**
     * @param line  Received SCP input line
     * @param isDir Does the input line refer to a directory
     * @param time  The received {@link org.xbib.io.sshd.common.scp.ScpTimestamp} - may be {@code null}
     * @throws IOException If failed to process the line
     */
    void process(String line, boolean isDir, ScpTimestamp time) throws IOException;
}
