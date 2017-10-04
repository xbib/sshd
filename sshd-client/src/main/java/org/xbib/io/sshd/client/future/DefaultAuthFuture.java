package org.xbib.io.sshd.client.future;

import org.xbib.io.sshd.common.SshException;
import org.xbib.io.sshd.common.future.DefaultVerifiableSshFuture;

import java.io.IOException;
import java.util.Objects;


/**
 * A default implementation of {@link AuthFuture}.
 */
public class DefaultAuthFuture extends DefaultVerifiableSshFuture<AuthFuture> implements AuthFuture {
    public DefaultAuthFuture(Object lock) {
        super(lock);
    }

    @Override
    public AuthFuture verify(long timeoutMillis) throws IOException {
        Boolean result = verifyResult(Boolean.class, timeoutMillis);
        if (!result) {
            throw new SshException("Authentication failed");
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
    public boolean isSuccess() {
        Object v = getValue();
        return (v instanceof Boolean) && (Boolean) v;
    }

    @Override
    public boolean isFailure() {
        Object v = getValue();
        if (v instanceof Boolean) {
            return !(Boolean) v;
        } else {
            return true;
        }
    }

    @Override
    public void setAuthed(boolean authed) {
        setValue(authed);
    }
}
