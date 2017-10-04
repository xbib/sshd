package org.xbib.io.sshd.common.file;

import org.xbib.io.sshd.common.session.Session;

import java.io.IOException;
import java.nio.file.FileSystem;

/**
 * Factory for file system implementations - it returns the file system for user.
 */
@FunctionalInterface
public interface FileSystemFactory {

    /**
     * Create user specific file system.
     *
     * @param session The session created for the user
     * @return The current {@link FileSystem} for the provided session
     * @throws IOException if the filesystem can not be created
     */
    FileSystem createFileSystem(Session session) throws IOException;
}
