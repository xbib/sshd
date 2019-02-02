package org.xbib.io.sshd.common.future;

import org.xbib.io.sshd.common.SshException;

import java.io.IOException;
import java.util.Objects;

/**
 * A default implementation of {@link OpenFuture}.
 */
public class DefaultOpenFuture extends DefaultVerifiableSshFuture<OpenFuture> implements OpenFuture {
    public DefaultOpenFuture(Object id, Object lock) {
        super(id, lock);
    }

    @Override
    public OpenFuture verify(long timeoutMillis) throws IOException {
        Boolean result = verifyResult(Boolean.class, timeoutMillis);
        if (!result) {
            throw new SshException("Channel opening failed");
        }

        return this;
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

    @Override
    public void setException(Throwable exception) {
        Objects.requireNonNull(exception, "No exception provided");
        setValue(exception);
    }

    @Override
    public boolean isOpened() {
        Object value = getValue();
        return (value instanceof Boolean) && (Boolean) value;
    }

    @Override
    public void setOpened() {
        setValue(Boolean.TRUE);
    }
}
