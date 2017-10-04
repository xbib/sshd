package org.xbib.io.sshd.client.subsystem.sftp;

import org.xbib.io.sshd.common.util.io.OutputStreamWithChannel;

import java.io.IOException;
import java.util.Collection;
import java.util.Objects;

/**
 * Implements an output stream for a given remote file.
 */
public class SftpOutputStreamWithChannel extends OutputStreamWithChannel {
    private final SftpClient client;
    private final String path;
    private final byte[] bb = new byte[1];
    private final byte[] buffer;
    private int index;
    private SftpClient.CloseableHandle handle;
    private long offset;

    public SftpOutputStreamWithChannel(SftpClient client, int bufferSize, String path, Collection<SftpClient.OpenMode> mode) throws IOException {
        this.client = Objects.requireNonNull(client, "No SFTP client instance");
        this.path = path;
        buffer = new byte[bufferSize];
        handle = client.open(path, mode);
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
     * The remotely accessed file path
     *
     * @return Remote file path
     */
    public final String getPath() {
        return path;
    }

    @Override
    public boolean isOpen() {
        return (handle != null) && handle.isOpen();
    }

    @Override
    public void write(int b) throws IOException {
        bb[0] = (byte) b;
        write(bb, 0, 1);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (!isOpen()) {
            throw new IOException("write(" + getPath() + ")[len=" + len + "] stream is closed");
        }

        do {
            int nb = Math.min(len, buffer.length - index);
            System.arraycopy(b, off, buffer, index, nb);
            index += nb;
            if (index == buffer.length) {
                flush();
            }
            off += nb;
            len -= nb;
        } while (len > 0);
    }

    @Override
    public void flush() throws IOException {
        if (!isOpen()) {
            throw new IOException("flush(" + getPath() + ") stream is closed");
        }

        client.write(handle, offset, buffer, 0, index);
        offset += index;
        index = 0;
    }

    @Override
    public void close() throws IOException {
        if (isOpen()) {
            try {
                try {
                    if (index > 0) {
                        flush();
                    }
                } finally {
                    handle.close();
                }
            } finally {
                handle = null;
            }
        }
    }
}