package org.xbib.io.sshd.common.util.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * A {@code /dev/null} implementation - always open.
 */
public class EmptyInputStream extends InputStream {
    public static final EmptyInputStream DEV_NULL = new EmptyInputStream();

    public EmptyInputStream() {
        super();
    }

    @Override
    public int read() throws IOException {
        return -1;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return -1;
    }

    @Override
    public long skip(long n) throws IOException {
        return 0L;
    }

    @Override
    public int available() throws IOException {
        return 0;
    }

    @Override
    public synchronized void mark(int readlimit) {
        throw new UnsupportedOperationException("mark(" + readlimit + ") called despite the fact that markSupported=" + markSupported());
    }

    @Override
    public synchronized void reset() throws IOException {
        // ignored
    }
}
