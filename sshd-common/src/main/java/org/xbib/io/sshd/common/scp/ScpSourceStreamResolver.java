package org.xbib.io.sshd.common.scp;

import org.xbib.io.sshd.common.session.Session;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Collection;

/**
 *
 */
public interface ScpSourceStreamResolver {
    /**
     * @return The uploaded file name
     * @throws IOException If failed to resolve the name
     */
    String getFileName() throws IOException;

    /**
     * @return The {@link Path} to use when invoking the {@link ScpTransferEventListener}
     */
    Path getEventListenerFilePath();

    /**
     * @return The permissions to be used for uploading a file
     * @throws IOException If failed to generate the required permissions
     */
    Collection<PosixFilePermission> getPermissions() throws IOException;

    /**
     * @return The {@link org.xbib.io.sshd.common.scp.ScpTimestamp} to use for uploading the file
     * if {@code null} then no need to send this information
     * @throws IOException If failed to generate the required data
     */
    ScpTimestamp getTimestamp() throws IOException;

    /**
     * @return An estimated size of the expected number of bytes to be uploaded.
     * If non-positive then assumed to be unknown.
     * @throws IOException If failed to generate an estimate
     */
    long getSize() throws IOException;

    /**
     * @param session The {@link Session} through which file is transmitted
     * @param options The {@link OpenOption}s may be {@code null}/empty
     * @return The {@link InputStream} containing the data to be uploaded
     * @throws IOException If failed to create the stream
     */
    InputStream resolveSourceStream(Session session, OpenOption... options) throws IOException;
}
