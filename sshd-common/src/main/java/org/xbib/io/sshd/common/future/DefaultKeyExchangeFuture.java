package org.xbib.io.sshd.common.future;

import org.xbib.io.sshd.common.SshException;

import java.io.IOException;

/**
 *
 */
public class DefaultKeyExchangeFuture extends DefaultVerifiableSshFuture<KeyExchangeFuture> implements KeyExchangeFuture {
    public DefaultKeyExchangeFuture(Object lock) {
        super(lock);
    }

    @Override
    public KeyExchangeFuture verify(long timeoutMillis) throws IOException {
        Boolean result = verifyResult(Boolean.class, timeoutMillis);
        if (!result) {
            throw new SshException("Key exchange failed");
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
}
