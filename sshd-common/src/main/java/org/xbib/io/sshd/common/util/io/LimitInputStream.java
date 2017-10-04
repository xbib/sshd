package org.xbib.io.sshd.common.util.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channel;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Reads from another {@link InputStream} up to specified max. length.
 */
public class LimitInputStream extends FilterInputStream implements Channel {
    private final AtomicBoolean open = new AtomicBoolean(true);
    private long remaining;

    public LimitInputStream(InputStream in, long length) {
        super(in);
        remaining = length;
    }

    @Override
    public boolean isOpen() {
        return open.get();
    }

    @Override
    public int read() throws IOException {
        if (!isOpen()) {
            throw new IOException("read() - stream is closed (remaining=" + remaining + ")");
        }

        if (remaining > 0) {
            remaining--;
            return super.read();
        } else {
            return -1;
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (!isOpen()) {
            throw new IOException("read(len=" + len + ") stream is closed (remaining=" + remaining + ")");
        }

        int nb = len;
        if (nb > remaining) {
            nb = (int) remaining;
        }
        if (nb > 0) {
            int read = super.read(b, off, nb);
            remaining -= read;
            return read;
        } else {
            return -1;
        }
    }

    @Override
    public long skip(long n) throws IOException {
        if (!isOpen()) {
            throw new IOException("skip(" + n + ") stream is closed (remaining=" + remaining + ")");
        }

        long skipped = super.skip(n);
        remaining -= skipped;
        return skipped;
    }

    @Override
    public int available() throws IOException {
        if (!isOpen()) {
            throw new IOException("available() stream is closed (remaining=" + remaining + ")");
        }

        int av = super.available();
        if (av > remaining) {
            return (int) remaining;
        } else {
            return av;
        }
    }

    @Override
    public void close() throws IOException {
        // do not close the original input stream since it serves for ACK(s)
        if (open.getAndSet(false)) {
            //noinspection UnnecessaryReturnStatement
            return; // debug breakpoint
        }
    }
}