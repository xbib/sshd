package org.xbib.io.sshd.client.subsystem.sftp;

import java.util.Objects;

/**
 *
 */
public class StfpIterableDirHandle implements Iterable<SftpClient.DirEntry> {
    private final SftpClient client;
    private final SftpClient.Handle handle;

    /**
     * @param client The {@link SftpClient} to use for iteration
     * @param handle The remote directory {@link SftpClient.Handle}
     */
    public StfpIterableDirHandle(SftpClient client, SftpClient.Handle handle) {
        this.client = Objects.requireNonNull(client, "No client instance");
        this.handle = handle;
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
     * @return The remote directory {@link SftpClient.Handle}
     */
    public final SftpClient.Handle getHandle() {
        return handle;
    }

    @Override
    public SftpDirEntryIterator iterator() {
        return new SftpDirEntryIterator(getClient(), getHandle());
    }
}
