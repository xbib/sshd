package org.xbib.io.sshd.common.io;

import org.xbib.io.sshd.common.SshException;
import org.xbib.io.sshd.common.future.DefaultVerifiableSshFuture;

import java.io.IOException;

/**
 *
 */
public abstract class AbstractIoWriteFuture extends DefaultVerifiableSshFuture<IoWriteFuture> implements IoWriteFuture {
    protected AbstractIoWriteFuture(Object lock) {
        super(lock);
    }

    @Override
    public IoWriteFuture verify(long timeout) throws IOException {
        Boolean result = verifyResult(Boolean.class, timeout);
        if (!result) {
            throw new SshException("Write failed signalled");
        }

        return this;
    }

    @Override
    public boolean isWritten() {
        Object value = getValue();
        return (value instanceof Boolean) && (Boolean) value;
    }

    @Override
    public Throwable getException() {
        Object v = getValue();
        if (v instanceof Throwable) {
            return (Throwable) v;
        } else {
            return null;
        }
    }
}
