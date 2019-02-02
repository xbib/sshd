package org.xbib.io.sshd.common.channel;

import org.xbib.io.sshd.common.io.AbstractIoWriteFuture;
import org.xbib.io.sshd.common.util.buffer.Buffer;

import java.util.Objects;

/**
 *
 */
public class IoWriteFutureImpl extends AbstractIoWriteFuture {
    private final Buffer buffer;

    public IoWriteFutureImpl(Object id, Buffer buffer) {
        super(id,null);
        this.buffer = Objects.requireNonNull(buffer, "No buffer provided");
    }

    public Buffer getBuffer() {
        return buffer;
    }
}