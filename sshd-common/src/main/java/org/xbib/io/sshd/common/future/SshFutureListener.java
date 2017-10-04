package org.xbib.io.sshd.common.future;

import org.xbib.io.sshd.common.util.SshdEventListener;

/**
 * Something interested in being notified when the completion
 * of an asynchronous SSH operation : {@link SshFuture}.
 *
 * @param <T> type of future
 */
@SuppressWarnings("rawtypes")
@FunctionalInterface
public interface SshFutureListener<T extends SshFuture> extends SshdEventListener {

    /**
     * Invoked when the operation associated with the {@link SshFuture}
     * has been completed even if you add the listener after the completion.
     *
     * @param future The source {@link SshFuture} which called this
     *               callback.
     */
    void operationComplete(T future);
}
