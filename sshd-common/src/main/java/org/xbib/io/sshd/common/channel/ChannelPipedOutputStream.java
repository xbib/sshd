package org.xbib.io.sshd.common.channel;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channel;

/**
 *
 */
public class ChannelPipedOutputStream extends OutputStream implements Channel {

    private final org.xbib.io.sshd.common.channel.ChannelPipedSink sink;
    private final byte[] b = new byte[1];
    private boolean closed;

    public ChannelPipedOutputStream(ChannelPipedSink sink) {
        this.sink = sink;
    }

    @Override
    public void write(int i) throws IOException {
        synchronized (b) {
            b[0] = (byte) i;
            write(b, 0, 1);
        }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (!isOpen()) {
            throw new IOException("write(len=" + len + ") Stream has been closed");
        }
        sink.receive(b, off, len);
    }

    @Override
    public boolean isOpen() {
        return !closed;
    }

    @Override
    public void flush() throws IOException {
        if (!isOpen()) {
            throw new IOException("flush() Stream has been closed");
        }
    }

    @Override
    public void close() throws IOException {
        if (isOpen()) {
            try {
                sink.eof();
            } finally {
                closed = true;
            }
        }
    }
}
