package org.xbib.io.sshd.client.subsystem.sftp;

import org.xbib.io.sshd.common.util.ValidateUtils;

import java.io.IOException;
import java.util.Objects;

/**
 * Provides an {@link Iterable} implementation of the {@link SftpClient.DirEntry}-ies
 * for a remote directory
 */
public class SftpIterableDirEntry implements Iterable<SftpClient.DirEntry> {
    private final SftpClient client;
    private final String path;

    /**
     * @param client The {@link SftpClient} instance to use for the iteration
     * @param path   The remote directory path
     */
    public SftpIterableDirEntry(SftpClient client, String path) {
        this.client = Objects.requireNonNull(client, "No client instance");
        this.path = ValidateUtils.checkNotNullAndNotEmpty(path, "No remote path");
    }

    /**
     * The client instance
     *
     * @return {@link SftpClient} instance used to access the remote file
     */
    public final SftpClient getClient() {
        return client;
    }

    /**
     * The remotely accessed directory path
     *
     * @return Remote directory path
     */
    public final String getPath() {
        return path;
    }

    @Override
    public SftpDirEntryIterator iterator() {
        try {
            return new SftpDirEntryIterator(getClient(), getPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}