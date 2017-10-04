package org.xbib.io.sshd.common.scp;

import org.xbib.io.sshd.common.session.Session;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

/**
 *
 */
public interface ScpTargetStreamResolver {
    /**
     * Called when receiving a file in order to obtain an output stream
     * for the incoming data
     *
     * @param session The associated {@link Session}
     * @param name    File name as received from remote site
     * @param length  Number of bytes expected to receive
     * @param perms   The {@link Set} of {@link PosixFilePermission} expected
     * @param options The {@link OpenOption}s to use - may be {@code null}/empty
     * @return The {@link OutputStream} to write the incoming data
     * @throws IOException If failed to create the stream
     */
    OutputStream resolveTargetStream(Session session, String name, long length,
                                     Set<PosixFilePermission> perms, OpenOption... options) throws IOException;

    /**
     * @return The {@link Path} to use when invoking the {@link ScpTransferEventListener}
     */
    Path getEventListenerFilePath();

    /**
     * Called after successful reception of the data (and after closing the stream)
     *
     * @param name     File name as received from remote site
     * @param preserve If {@code true} then the resolver should attempt to preserve
     *                 the specified permissions and timestamp
     * @param perms    The {@link Set} of {@link PosixFilePermission} expected
     * @param time     If not {@code null} then the required timestamp(s) on the
     *                 incoming data
     * @throws IOException If failed to post-process the incoming data
     */
    void postProcessReceivedData(String name, boolean preserve, Set<PosixFilePermission> perms, ScpTimestamp time) throws IOException;
}
