package org.xbib.io.sshd.common.session.helpers;

import org.xbib.io.sshd.common.future.SshFutureListener;
import org.xbib.io.sshd.common.io.AbstractIoWriteFuture;
import org.xbib.io.sshd.common.io.IoWriteFuture;
import org.xbib.io.sshd.common.util.buffer.Buffer;

import java.util.Objects;

/**
 * Future holding a packet pending key exchange termination.
 */
public class PendingWriteFuture extends AbstractIoWriteFuture implements SshFutureListener<IoWriteFuture> {
    private final Buffer buffer;

    public PendingWriteFuture(Buffer buffer) {
        super(null);
        this.buffer = Objects.requireNonNull(buffer, "No buffer provided");
    }

    public Buffer getBuffer() {
        return buffer;
    }

    public void setWritten() {
        setValue(Boolean.TRUE);
    }

    public void setException(Throwable cause) {
        Objects.requireNonNull(cause, "No cause specified");
        setValue(cause);
    }

    @Override
    public void operationComplete(IoWriteFuture future) {
        if (future.isWritten()) {
            setWritten();
        } else {
            setException(future.getException());
        }
    }
}
