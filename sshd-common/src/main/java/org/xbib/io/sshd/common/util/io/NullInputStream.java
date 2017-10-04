package org.xbib.io.sshd.common.util.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channel;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A {@code /dev/null} input stream.
 */
public class NullInputStream extends InputStream implements Channel {
    private final AtomicBoolean open = new AtomicBoolean(true);

    public NullInputStream() {
        super();
    }

    @Override
    public boolean isOpen() {
        return open.get();
    }

    @Override
    public int read() throws IOException {
        if (!isOpen()) {
            throw new IOException("Stream is closed for reading one value");
        }
        return -1;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (!isOpen()) {
            throw new IOException("Stream is closed for reading " + len + " bytes");
        }
        return -1;
    }

    @Override
    public long skip(long n) throws IOException {
        if (!isOpen()) {
            throw new IOException("Stream is closed for skipping " + n + " bytes");
        }
        return 0L;
    }

    @Override
    public int available() throws IOException {
        if (!isOpen()) {
            throw new IOException("Stream is closed for availability query");
        }
        return 0;
    }

    @Override
    public synchronized void reset() throws IOException {
        if (!isOpen()) {
            throw new IOException("Stream is closed for reset");
        }
    }

    @Override
    public void close() throws IOException {
        if (open.getAndSet(false)) {
            return; // debug breakpoint
        }
    }
}
