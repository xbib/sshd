package org.xbib.io.sshd.common.file;

import java.nio.file.FileSystem;

/**
 * Interface that can be implemented by a command to be able to access the
 * file system in which this command will be used.
 */
@FunctionalInterface
public interface FileSystemAware {
    /**
     * Set the file system in which this shell will be executed.
     *
     * @param fileSystem the file system
     */
    void setFileSystem(FileSystem fileSystem);
}
