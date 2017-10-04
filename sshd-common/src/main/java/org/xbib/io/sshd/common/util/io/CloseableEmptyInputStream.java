package org.xbib.io.sshd.common.util.io;

import java.io.IOException;
import java.nio.channels.Channel;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A {@code /dev/null} stream that can be closed - in which case it will throw
 * {@link IOException}s if invoked after being closed.
 */
public class CloseableEmptyInputStream extends EmptyInputStream implements Channel {
    private final AtomicBoolean open = new AtomicBoolean(true);

    public CloseableEmptyInputStream() {
        super();
    }

    @Override
    public boolean isOpen() {
        return open.get();
    }

    @Override
    public int available() throws IOException {
        if (isOpen()) {
            return super.available();
        } else {
            throw new IOException("available() stream is closed");
        }
    }

    @Override
    public int read() throws IOException {
        if (isOpen()) {
            return super.read();
        } else {
            throw new IOException("read() stream is closed");
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (isOpen()) {
            return super.read(b, off, len);
        } else {
            throw new IOException("read([])[" + off + "," + len + "] stream is closed");
        }
    }

    @Override
    public long skip(long n) throws IOException {
        if (isOpen()) {
            return super.skip(n);
        } else {
            throw new IOException("skip(" + n + ") stream is closed");
        }
    }

    @Override
    public synchronized void reset() throws IOException {
        if (isOpen()) {
            super.reset();
        } else {
            throw new IOException("reset() stream is closed");
        }
    }

    @Override
    public void close() throws IOException {
        if (open.getAndSet(false)) {
            return; // debug breakpoint
        }
    }
}
