package org.xbib.io.sshd.common.scp;

import org.xbib.io.sshd.common.session.Session;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.OpenOption;
import java.nio.file.Path;

/**
 * Plug-in mechanism for users to intervene in the SCP process - e.g.,
 * apply some kind of traffic shaping mechanism, display upload/download
 * progress, etc...
 */
public interface ScpFileOpener {
    /**
     * Create an input stream to read from a file
     *
     * @param session The {@link Session} requesting the access
     * @param file    The requested local file {@link Path}
     * @param options The {@link OpenOption}s - may be {@code null}/empty
     * @return The open {@link InputStream} never {@code null}
     * @throws IOException If failed to open the file
     */
    InputStream openRead(Session session, Path file, OpenOption... options) throws IOException;

    /**
     * Create an output stream to write to a file
     *
     * @param session The {@link Session} requesting the access
     * @param file    The requested local file {@link Path}
     * @param options The {@link OpenOption}s - may be {@code null}/empty
     * @return The open {@link OutputStream} never {@code null}
     * @throws IOException If failed to open the file
     */
    OutputStream openWrite(Session session, Path file, OpenOption... options) throws IOException;
}
