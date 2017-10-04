package org.xbib.io.sshd.common.io.nio2;

import java.nio.channels.CompletionHandler;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * @param <V> Result type
 * @param <A> Attachment type
 */
public abstract class Nio2CompletionHandler<V, A> implements CompletionHandler<V, A> {
    protected Nio2CompletionHandler() {
        super();
    }

    @Override
    public void completed(final V result, final A attachment) {
        AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
            onCompleted(result, attachment);
            return null;
        });
    }

    @Override
    public void failed(final Throwable exc, final A attachment) {
        AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
            onFailed(exc, attachment);
            return null;
        });
    }

    protected abstract void onCompleted(V result, A attachment);

    protected abstract void onFailed(Throwable exc, A attachment);
}
