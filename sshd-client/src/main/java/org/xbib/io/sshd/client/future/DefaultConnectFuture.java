package org.xbib.io.sshd.client.future;

import org.xbib.io.sshd.client.session.ClientSession;
import org.xbib.io.sshd.common.RuntimeSshException;
import org.xbib.io.sshd.common.future.DefaultVerifiableSshFuture;

import java.io.IOException;
import java.util.Objects;

/**
 * A default implementation of {@link ConnectFuture}.
 */
public class DefaultConnectFuture extends DefaultVerifiableSshFuture<ConnectFuture> implements ConnectFuture {
    public DefaultConnectFuture(Object id, Object lock) {
        super(id, lock);
    }

    @Override
    public ConnectFuture verify(long timeout) throws IOException {
        long startTime = System.nanoTime();
        ClientSession session = verifyResult(ClientSession.class, timeout);
        long endTime = System.nanoTime();
        return this;
    }

    @Override
    public ClientSession getSession() {
        Object v = getValue();
        if (v instanceof RuntimeException) {
            throw (RuntimeException) v;
        } else if (v instanceof Error) {
            throw (Error) v;
        } else if (v instanceof Throwable) {
            throw new RuntimeSshException("Failed to get the session.", (Throwable) v);
        } else if (v instanceof ClientSession) {
            return (ClientSession) v;
        } else {
            return null;
        }
    }

    @Override
    public void setSession(ClientSession session) {
        Objects.requireNonNull(session, "No client session provided");
        setValue(session);
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
    public boolean isConnected() {
        return getValue() instanceof ClientSession;
    }
}
