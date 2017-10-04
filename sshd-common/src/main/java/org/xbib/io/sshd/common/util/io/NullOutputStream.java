package org.xbib.io.sshd.common.util.io;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channel;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A {code /dev/null} output stream.
 */
public class NullOutputStream extends OutputStream implements Channel {
    private final AtomicBoolean open = new AtomicBoolean(true);

    public NullOutputStream() {
        super();
    }

    @Override
    public boolean isOpen() {
        return open.get();
    }

    @Override
    public void write(int b) throws IOException {
        if (!isOpen()) {
            throw new IOException("Stream is closed for writing one byte");
        }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (!isOpen()) {
            throw new IOException("Stream is closed for writing " + len + " bytes");
        }
    }

    @Override
    public void flush() throws IOException {
        if (!isOpen()) {
            throw new IOException("Stream is closed for flushing");
        }
    }

    @Override
    public void close() throws IOException {
        if (open.getAndSet(false)) {
            return; // debug breakpoint
        }
    }
}
