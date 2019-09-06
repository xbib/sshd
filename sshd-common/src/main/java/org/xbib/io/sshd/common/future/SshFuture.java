package org.xbib.io.sshd.common.future;


/**
 * Represents the completion of an asynchronous SSH operation on a given object
 * (it may be an SSH session or an SSH channel).
 * Can be listened for completion using a {@link SshFutureListener}.
 *
 * @param <T> Type of future
 */
public interface SshFuture<T extends SshFuture> extends WaitableFuture {
    /**
     * Adds an event {@code listener} which is notified when
     * this future is completed. If the listener is added
     * after the completion, the listener is directly notified.
     *
     * @param listener The {@link SshFutureListener} instance to add
     * @return The future instance
     */
    T addListener(SshFutureListener<T> listener);

    /**
     * Removes an existing event {@code listener} so it won't be notified when
     * the future is completed.
     *
     * @param listener The {@link SshFutureListener} instance to remove
     * @return The future instance
     */
    T removeListener(SshFutureListener<T> listener);
}
