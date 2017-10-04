package org.xbib.io.sshd.common.io.nio2;

import org.xbib.io.sshd.common.io.AbstractIoWriteFuture;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 *
 */
public class Nio2DefaultIoWriteFuture extends AbstractIoWriteFuture {
    private final ByteBuffer buffer;

    public Nio2DefaultIoWriteFuture(Object lock, ByteBuffer buffer) {
        super(lock);
        this.buffer = buffer;
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    public void setWritten() {
        setValue(Boolean.TRUE);
    }

    public void setException(Throwable exception) {
        setValue(Objects.requireNonNull(exception, "No exception specified"));
    }
}